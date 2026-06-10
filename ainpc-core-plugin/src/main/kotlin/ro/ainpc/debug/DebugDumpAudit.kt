package ro.ainpc.debug

import ro.ainpc.AINPCPlugin
import ro.ainpc.api.WorldAdminApi
import ro.ainpc.npc.AINPC
import ro.ainpc.world.WorldPlaceInfo

object DebugDumpAudit {
    @JvmStatic
    fun buildAuditText(plugin: AINPCPlugin): String {
        val warnings = ArrayList<String>()
        val errors = ArrayList<String>()

        val npcManager = runCatching { plugin.npcManager }.getOrNull()
        if (npcManager == null) {
            errors.add("NPCManager indisponibil.")
        } else {
            for (npc in npcManager.allNPCs) {
                val label = npc.name + "#" + npc.databaseId
                if (npc.homeAnchor == null) {
                    warnings.add("$label nu are homeAnchor.")
                }
                if (npc.workAnchor == null) {
                    warnings.add("$label nu are workAnchor.")
                }
                if (!npc.profileCreated) {
                    warnings.add("$label nu are profil persistent creat.")
                }
                if (npc.occupation.isNullOrBlank()) {
                    warnings.add("$label nu are ocupatie.")
                }
            }
        }

        val worldAdmin = runCatching { plugin.platform.worldAdmin }.getOrNull()
        if (worldAdmin == null || !worldAdmin.isEnabled) {
            warnings.add("World admin dezactivat sau indisponibil.")
        } else {
            if (worldAdmin.regionCount == 0) {
                warnings.add("World admin este activ, dar nu are regiuni.")
            }
            if (worldAdmin.placeCount == 0) {
                warnings.add("World admin nu are places.")
            }
            for (place in worldAdmin.places) {
                if (place.placeType().id == "house" && place.ownerNpcId().isBlank() &&
                    !DebugDumpSupport.hasPendingOwner(place)
                ) {
                    warnings.add("Casa fara owner_npc_id: " + place.id())
                }
                if (DebugDumpSupport.isWorkplace(place) && worldAdmin.getNodesForPlace(place.id()).isEmpty()) {
                    warnings.add("Loc de munca fara nodes: " + place.id())
                }
                if (DebugDumpSupport.isHousePlace(place)) {
                    auditHouseSpawnOrder(plugin, worldAdmin, place, warnings, errors)
                }
            }
            for (node in worldAdmin.nodes) {
                if (node.radius() <= 0) {
                    errors.add("Node cu raza invalida: " + node.id())
                }
            }
        }

        val sb = StringBuilder()
        sb.append("Errors: ").append(errors.size).append("\n")
        for (error in errors) {
            sb.append("[ERROR] ").append(error).append("\n")
        }
        sb.append("\nWarnings: ").append(warnings.size).append("\n")
        for (warning in warnings) {
            sb.append("[WARN] ").append(warning).append("\n")
        }
        return sb.toString()
    }

    @JvmStatic
    fun findLoadedNpcBySelector(plugin: AINPCPlugin, selector: String?): AINPC? {
        val normalizedSelector = DebugDumpSupport.normalizeKey(selector)
        val npcManager = runCatching { plugin.npcManager }.getOrNull()
        if (normalizedSelector.isBlank() || npcManager == null) {
            return null
        }

        for (npc in npcManager.allNPCs) {
            if (normalizedSelector.equals(npc.uuid.toString(), ignoreCase = true)) {
                return npc
            }
            if (npc.databaseId > 0) {
                val id = npc.databaseId.toString()
                if (normalizedSelector == id || normalizedSelector == "npc_$id") {
                    return npc
                }
            }
            val npcName = DebugDumpSupport.normalizeKey(npc.name)
            if (npcName.isNotBlank() && (normalizedSelector == npcName || normalizedSelector == "npc_$npcName")) {
                return npc
            }
        }

        return null
    }

    private fun auditHouseSpawnOrder(
        plugin: AINPCPlugin,
        worldAdmin: WorldAdminApi,
        house: WorldPlaceInfo,
        warnings: MutableList<String>,
        errors: MutableList<String>,
    ) {
        val residents = DebugDumpSupport.parseResidents(house)
        val maxResidents = DebugDumpSupport.parsePositiveIntMetadata(house, "max_residents", "maxResidents", "capacity")
        if (maxResidents == null) {
            warnings.add("Casa fara max_residents/capacity: " + house.id())
        } else if (residents.isNotEmpty() && residents.size > maxResidents) {
            errors.add("Casa " + house.id() + " are " + residents.size + " rezidenti peste max_residents=" + maxResidents + ".")
        }

        if (residents.isNotEmpty() &&
            !DebugDumpSupport.hasAnySemanticNode(
                worldAdmin.getNodesForPlace(house.id()),
                "bed",
                "home",
                "npc_spawn",
                "spawn"
            )
        ) {
            errors.add("Casa " + house.id() + " are rezidenti, dar nu are node bed/home/npc_spawn.")
        }

        for (residentSelector in residents) {
            val resident = findLoadedNpcBySelector(plugin, residentSelector)
            if (resident == null) {
                errors.add("Casa " + house.id() + " contine resident necunoscut: " + residentSelector + ".")
                continue
            }
            if (resident.homeAnchor == null) {
                errors.add(resident.name + "#" + resident.databaseId + " este resident in " + house.id() + ", dar nu are homeAnchor.")
            } else if (!DebugDumpSupport.ownedLocationInsidePlace(resident.homeAnchor, house)) {
                errors.add(resident.name + "#" + resident.databaseId + " este resident in " + house.id() + ", dar homeAnchor nu este in casa.")
            }
        }
    }
}
