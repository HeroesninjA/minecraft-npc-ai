package ro.ainpc.managers

import ro.ainpc.AINPCPlugin
import ro.ainpc.npc.AINPC
import ro.ainpc.spawn.FamilyBindingPlan
import ro.ainpc.spawn.FamilyBindingResult
import ro.ainpc.utils.NPCNameGenerator
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Types
import java.util.Locale
import java.util.Random

class FamilyManager(private val plugin: AINPCPlugin) {
    private val random = Random()

    fun generateFamily(npc: AINPC) {
        if (!plugin.config.getBoolean("family.auto_generate", false)) return

        val spouseChance = plugin.config.getInt("family.spouse_chance", 60)
        if (random.nextInt(100) < spouseChance && npc.age >= 20) generateSpouse(npc)

        val livingParentsChance = plugin.config.getInt("family.living_parents_chance", 40)
        generateParents(npc, random.nextInt(100) < livingParentsChance)

        if (hasSpouse(npc) && npc.age >= 25) {
            val maxChildren = plugin.config.getInt("family.max_children", 4)
            val numChildren = random.nextInt(maxChildren + 1)
            repeat(numChildren) { generateChild(npc) }
        }

        val numSiblings = random.nextInt(4)
        repeat(numSiblings) { generateSibling(npc) }
        plugin.debug("Familie generata pentru ${npc.name}")
    }

    private fun generateSpouse(npc: AINPC) {
        val spouseGender = if (npc.gender == "male") "female" else "male"
        val spouseName = generateName(spouseGender)
        var spouseAge = npc.age + random.nextInt(11) - 5
        spouseAge = maxOf(18, spouseAge)
        val backstory = "S-au casatorit acum ${random.nextInt(10) + 1} ani."
        addFamilyMember(npc, spouseName, "spouse", true, null, backstory)
    }

    private fun generateParents(npc: AINPC, alive: Boolean) {
        val fatherName = generateName("male")
        val fatherAlive = alive && random.nextBoolean()
        val fatherBackstory = if (fatherAlive) {
            "Lucreaza ca ${getRandomOccupation()}."
        } else {
            "A murit acum ${random.nextInt(20) + 1} ani."
        }
        addFamilyMember(npc, fatherName, "father", fatherAlive, null, fatherBackstory)

        val motherName = generateName("female")
        val motherAlive = alive && random.nextBoolean()
        val motherBackstory = if (motherAlive) {
            "Se ocupa de ${getRandomActivity()}."
        } else {
            "A murit acum ${random.nextInt(20) + 1} ani."
        }
        addFamilyMember(npc, motherName, "mother", motherAlive, null, motherBackstory)
    }

    private fun generateChild(npc: AINPC) {
        val childGender = if (random.nextBoolean()) "male" else "female"
        val childName = generateName(childGender)
        val relationType = if (childGender == "male") "son" else "daughter"
        val maxChildAge = npc.age - 18
        if (maxChildAge < 1) return
        val childAge = random.nextInt(minOf(maxChildAge, 25)) + 1
        val backstory = "Are $childAge ani. " + if (childAge >= 18) {
            "Lucreaza ca ${getRandomOccupation()}."
        } else {
            "Este inca la scoala."
        }
        addFamilyMember(npc, childName, relationType, true, null, backstory)
    }

    private fun generateSibling(npc: AINPC) {
        val siblingGender = if (random.nextBoolean()) "male" else "female"
        val siblingName = generateName(siblingGender)
        val relationType = if (siblingGender == "male") "brother" else "sister"
        var siblingAge = npc.age + random.nextInt(21) - 10
        siblingAge = maxOf(5, siblingAge)
        val alive = random.nextInt(100) < 90
        val backstory = if (alive) {
            "Are $siblingAge ani. Locuieste in ${getRandomLocation()}."
        } else {
            "A murit acum ${random.nextInt(10) + 1} ani."
        }
        addFamilyMember(npc, siblingName, relationType, alive, null, backstory)
    }

    fun addFamilyMember(
        npc: AINPC,
        name: String,
        relationType: String,
        isAlive: Boolean,
        relatedNpcId: Int?,
        backstory: String?
    ) {
        val sql = """
            INSERT INTO npc_family (npc_id, related_npc_id, related_name, relation_type, is_alive, backstory)
            VALUES (?, ?, ?, ?, ?, ?)
        """
        try {
            plugin.databaseManager.prepareStatement(sql).use { stmt ->
                stmt.setInt(1, npc.databaseId)
                if (relatedNpcId != null) stmt.setInt(2, relatedNpcId) else stmt.setNull(2, Types.INTEGER)
                stmt.setString(3, name)
                stmt.setString(4, relationType)
                stmt.setInt(5, if (isAlive) 1 else 0)
                stmt.setString(6, backstory)
                stmt.executeUpdate()
            }
        } catch (e: SQLException) {
            plugin.logger.warning("Eroare la adaugarea membrului familiei: ${e.message}")
        }
    }

    fun getFamily(npc: AINPC): List<FamilyMemberRecord> {
        val family = mutableListOf<FamilyMemberRecord>()
        val sql = """
            SELECT related_name, relation_type, is_alive, related_npc_id, backstory
            FROM npc_family
            WHERE npc_id = ?
            ORDER BY 
                CASE relation_type
                    WHEN 'spouse' THEN 1
                    WHEN 'father' THEN 2
                    WHEN 'mother' THEN 3
                    WHEN 'son' THEN 4
                    WHEN 'daughter' THEN 5
                    WHEN 'brother' THEN 6
                    WHEN 'sister' THEN 7
                    ELSE 8
                END
        """
        try {
            plugin.databaseManager.prepareStatement(sql).use { stmt ->
                stmt.setInt(1, npc.databaseId)
                stmt.executeQuery().use { rs ->
                    while (rs.next()) {
                        var relatedNpcId: Int? = rs.getInt("related_npc_id")
                        if (rs.wasNull()) relatedNpcId = null
                        family.add(
                            FamilyMemberRecord(
                                rs.getString("related_name"),
                                rs.getString("relation_type"),
                                rs.getInt("is_alive") == 1,
                                relatedNpcId
                            )
                        )
                    }
                }
            }
        } catch (e: SQLException) {
            plugin.logger.warning("Eroare la obtinerea familiei: ${e.message}")
        }
        return family
    }

    fun hasSpouse(npc: AINPC): Boolean {
        val sql = "SELECT COUNT(*) FROM npc_family WHERE npc_id = ? AND relation_type = 'spouse' AND is_alive = 1"
        return try {
            plugin.databaseManager.prepareStatement(sql).use { stmt ->
                stmt.setInt(1, npc.databaseId)
                stmt.executeQuery().use { rs -> rs.next() && rs.getInt(1) > 0 }
            }
        } catch (e: SQLException) {
            plugin.logger.warning("Eroare la verificarea sotului/sotiei: ${e.message}")
            false
        }
    }

    fun getFamilyReport(npc: AINPC): String {
        val sb = StringBuilder()
        val family = getFamily(npc)
        sb.append("&6=== Familia lui ").append(npc.name).append(" ===\n\n")
        if (family.isEmpty()) {
            sb.append("&7Nu are familie cunoscuta.\n")
            return sb.toString()
        }
        val grouped = linkedMapOf<String, MutableList<FamilyMemberRecord>>()
        for (member in family) grouped.computeIfAbsent(member.relationType() ?: "") { mutableListOf() }.add(member)
        for ((relation, members) in grouped) {
            sb.append("&e").append(getRelationNameRomanian(relation)).append(":\n")
            for (member in members) {
                sb.append("  &f- ").append(member.name())
                if (!member.alive()) sb.append(" &8(decedat)")
                sb.append("\n")
            }
        }
        return sb.toString()
    }

    fun linkNPCsAsFamily(npc1: AINPC, npc2: AINPC, relation1to2: String, relation2to1: String) {
        addFamilyMember(npc1, npc2.name, relation1to2, true, npc2.databaseId, null)
        addFamilyMember(npc2, npc1.name, relation2to1, true, npc1.databaseId, null)
    }

    fun bindSpawnedFamily(plan: FamilyBindingPlan?): FamilyBindingResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        if (plan == null) {
            errors.add("FamilyBindingPlan este null.")
            return FamilyBindingResult(false, "", 0, errors, warnings)
        }
        if (plan.familyId().isBlank()) errors.add("FamilyBindingPlan nu are familyId.")
        val members = validateFamilyMembers(plan.members(), errors, warnings)
        if (members.size < 2) errors.add("Familia ${plan.familyId()} are nevoie de cel putin 2 membri fizici salvati.")
        if (errors.isNotEmpty()) return FamilyBindingResult(false, plan.familyId(), 0, errors, warnings)

        var relationsCreated = 0
        for (i in members.indices) {
            val left = members[i]
            for (j in i + 1 until members.size) {
                val right = members[j]
                val relationPair = resolveRelationPair(left.familyRole(), right.familyRole())
                if (relationPair.leftToRight() == "relative" && relationPair.rightToLeft() == "relative") {
                    warnings.add(
                        "Relatie generica folosita intre ${left.npcName()} si ${right.npcName()} pentru roluri: ${left.familyRole()} / ${right.familyRole()}."
                    )
                }
                if (addFamilyMemberIfMissing(left.npcId(), right.npcId(), right.npcName(), relationPair.leftToRight())) relationsCreated++
                if (addFamilyMemberIfMissing(right.npcId(), left.npcId(), left.npcName(), relationPair.rightToLeft())) relationsCreated++
            }
        }

        if (relationsCreated == 0) {
            warnings.add("Nu au fost create relatii noi pentru familia ${plan.familyId()}.")
        } else {
            plugin.debug("Familie fizica legata: ${plan.familyId()} cu $relationsCreated relatii.")
        }
        return FamilyBindingResult(true, plan.familyId(), relationsCreated, errors, warnings)
    }

    private fun validateFamilyMembers(
        rawMembers: List<FamilyBindingPlan.Member>,
        errors: MutableList<String>,
        warnings: MutableList<String>
    ): List<FamilyBindingPlan.Member> {
        val valid = mutableListOf<FamilyBindingPlan.Member>()
        val seenNpcIds = mutableSetOf<Int>()
        for (member in rawMembers) {
            if (member.npcId() <= 0) {
                errors.add("Membrul familiei '${member.npcKey()}' nu are databaseId valid.")
                continue
            }
            if (member.npcName().isBlank()) {
                errors.add("Membrul familiei cu id ${member.npcId()} nu are nume.")
                continue
            }
            if (member.familyRole().isBlank()) {
                warnings.add("Membrul ${member.npcName()} nu are familyRole; folosesc 'relative'.")
            }
            if (!seenNpcIds.add(member.npcId())) {
                errors.add("NPC duplicat in FamilyBindingPlan: id=${member.npcId()}.")
                continue
            }
            valid.add(member)
        }
        return valid
    }

    private fun addFamilyMemberIfMissing(npcId: Int, relatedNpcId: Int, relatedName: String, relationType: String): Boolean {
        if (familyRelationExists(npcId, relatedNpcId, relationType)) return false
        val sql = """
            INSERT INTO npc_family (npc_id, related_npc_id, related_name, relation_type, is_alive, backstory)
            VALUES (?, ?, ?, ?, 1, ?)
        """
        return try {
            plugin.databaseManager.prepareStatement(sql).use { stmt ->
                stmt.setInt(1, npcId)
                stmt.setInt(2, relatedNpcId)
                stmt.setString(3, relatedName)
                stmt.setString(4, relationType)
                stmt.setString(5, "Relatie fizica legata dupa spawn.")
                stmt.executeUpdate()
            }
            true
        } catch (e: SQLException) {
            plugin.logger.warning("Eroare la legarea familiei dupa spawn: ${e.message}")
            false
        }
    }

    private fun familyRelationExists(npcId: Int, relatedNpcId: Int, relationType: String): Boolean {
        val sql = """
            SELECT COUNT(*)
            FROM npc_family
            WHERE npc_id = ? AND related_npc_id = ? AND relation_type = ?
        """
        return try {
            plugin.databaseManager.prepareStatement(sql).use { stmt ->
                stmt.setInt(1, npcId)
                stmt.setInt(2, relatedNpcId)
                stmt.setString(3, relationType)
                stmt.executeQuery().use { rs -> rs.next() && rs.getInt(1) > 0 }
            }
        } catch (e: SQLException) {
            plugin.logger.warning("Eroare la verificarea relatiei familiale existente: ${e.message}")
            false
        }
    }

    private fun resolveRelationPair(leftRole: String?, rightRole: String?): RelationPair {
        val left = normalizeRole(leftRole)
        val right = normalizeRole(rightRole)
        if (isParentRole(left) && isParentRole(right)) return RelationPair("spouse", "spouse")
        if (isSpouseRole(left) && isSpouseRole(right)) return RelationPair("spouse", "spouse")
        if (isParentRole(left) && isChildRole(right)) return RelationPair(childRelation(right), parentRelation(left))
        if (isChildRole(left) && isParentRole(right)) return RelationPair(parentRelation(right), childRelation(left))
        if (isChildRole(left) && isChildRole(right)) return RelationPair(siblingRelation(right), siblingRelation(left))
        if (isSiblingRole(left) && isSiblingRole(right)) return RelationPair(siblingRelation(right), siblingRelation(left))
        return RelationPair("relative", "relative")
    }

    private fun isParentRole(role: String): Boolean = role == "father" || role == "mother" || role == "parent"
    private fun isChildRole(role: String): Boolean = role == "son" || role == "daughter" || role == "child"
    private fun isSpouseRole(role: String): Boolean = role == "spouse" || role == "husband" || role == "wife"
    private fun isSiblingRole(role: String): Boolean = role == "brother" || role == "sister" || role == "sibling"

    private fun parentRelation(role: String): String = when (role) {
        "father" -> "father"
        "mother" -> "mother"
        else -> "parent"
    }

    private fun childRelation(role: String): String = when (role) {
        "son" -> "son"
        "daughter" -> "daughter"
        else -> "child"
    }

    private fun siblingRelation(role: String): String = when (role) {
        "brother", "son" -> "brother"
        "sister", "daughter" -> "sister"
        else -> "sibling"
    }

    private fun normalizeRole(role: String?): String {
        if (role.isNullOrBlank()) return "relative"
        return role.trim().lowercase(Locale.ROOT).replace(' ', '_').replace('-', '_')
    }

    private class RelationPair(
        private val leftToRightValue: String,
        private val rightToLeftValue: String
    ) {
        fun leftToRight(): String = leftToRightValue
        fun rightToLeft(): String = rightToLeftValue
    }

    fun clearFamily(npc: AINPC) {
        val sql = "DELETE FROM npc_family WHERE npc_id = ?"
        try {
            plugin.databaseManager.prepareStatement(sql).use { stmt ->
                stmt.setInt(1, npc.databaseId)
                stmt.executeUpdate()
            }
        } catch (e: SQLException) {
            plugin.logger.warning("Eroare la stergerea familiei: ${e.message}")
        }
    }

    private fun generateName(gender: String): String = NPCNameGenerator.randomName(gender, random)

    private fun getRandomOccupation(): String {
        val occupations = arrayOf(
            "worker", "caretaker", "guide", "resident"
        )
        return occupations[random.nextInt(occupations.size)]
    }

    private fun getRandomActivity(): String {
        val activities = arrayOf(
            "casa si familie", "organizarea locuintei", "pregatiri zilnice",
            "ingrijirea familiei", "ajutor pentru comunitate"
        )
        return activities[random.nextInt(activities.size)]
    }

    private fun getRandomLocation(): String {
        val locations = arrayOf(
            "satul vecin", "orasul mare", "peste munti", "langa rau",
            "in padure", "la marginea regatului", "peste mare"
        )
        return locations[random.nextInt(locations.size)]
    }

    private fun getRelationNameRomanian(relation: String): String = when (relation.lowercase()) {
        "spouse" -> "Sot/Sotie"
        "father" -> "Tata"
        "mother" -> "Mama"
        "son" -> "Fiu"
        "daughter" -> "Fiica"
        "brother" -> "Frate"
        "sister" -> "Sora"
        "grandfather" -> "Bunic"
        "grandmother" -> "Bunica"
        else -> relation
    }
}
