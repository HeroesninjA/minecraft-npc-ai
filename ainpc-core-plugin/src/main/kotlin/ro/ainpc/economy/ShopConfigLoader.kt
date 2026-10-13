package ro.ainpc.economy

import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import ro.ainpc.world.PlaceType
import java.io.File
import java.util.Locale

internal object ShopConfigLoader {
    fun load(file: File): List<NpcShopDefinition> = parse(YamlConfiguration().apply { load(file) })

    fun parse(config: ConfigurationSection): List<NpcShopDefinition> {
        val shops = requireNotNull(config.getConfigurationSection("shops")) { "Lipseste sectiunea shops" }
        return shops.getKeys(false).map { shopId ->
            val path = "shops.$shopId"
            val section = requireNotNull(shops.getConfigurationSection(shopId)) { "$path trebuie sa fie o sectiune" }
            val role = section.getString("npc_role")?.trim().orEmpty()
            require(role.isNotEmpty()) { "$path.npc_role lipseste" }
            require(!section.contains("permission") || section.isString("permission")) {
                "$path.permission trebuie sa fie text"
            }
            val currency = enumValue<ShopCurrency>(section.getString("currency", "ITEM"), "$path.currency")
            val restock = enumValue<RestockPolicy>(section.getString("restock_policy", "NEVER"), "$path.restock_policy")
            val offersSection = requireNotNull(section.getConfigurationSection("offers")) { "$path.offers lipseste" }
            val offers = offersSection.getKeys(false).map { offerId ->
                val offerPath = "$path.offers.$offerId"
                val offer = requireNotNull(offersSection.getConfigurationSection(offerId)) {
                    "$offerPath trebuie sa fie o sectiune"
                }
                val displayItem = material(offer.getString("display_item"), "$offerPath.display_item")
                val displayAmount = integer(offer, "display_amount", offerPath, 1)
                require(displayAmount > 0) { "$offerPath.display_amount trebuie sa fie pozitiv" }
                val maxUses = integer(offer, "max_uses", offerPath, -1)
                require(!offer.contains("requirements") || offer.isList("requirements")) {
                    "$offerPath.requirements trebuie sa fie o lista"
                }
                ShopOffer(
                    offerId,
                    displayItem,
                    displayAmount,
                    items(offer, "cost_items", offerPath),
                    items(offer, "result_items", offerPath),
                    maxUses,
                    offer.getStringList("requirements"),
                )
            }
            require(offers.isNotEmpty()) { "$path.offers nu poate fi gol" }
            NpcShopDefinition(
                shopId,
                role,
                currency,
                offers,
                restock,
                section.getString("permission")?.takeIf { it.isNotBlank() },
                placeTypes(section, path),
                placeIds(section, path),
            )
        }
    }

    private fun placeTypes(section: ConfigurationSection, path: String): Set<PlaceType> {
        if (!section.contains("place_types")) return emptySet()
        require(section.isList("place_types")) { "$path.place_types trebuie sa fie o lista" }
        return section.getList("place_types").orEmpty().map { value ->
            require(value is String) { "$path.place_types trebuie sa contina doar texte" }
            PlaceType.entries.firstOrNull { type ->
                type.name.equals(value, ignoreCase = true) || type.id.equals(value, ignoreCase = true)
            } ?: throw IllegalArgumentException("$path.place_types contine tipul necunoscut $value")
        }.toSet()
    }

    private fun placeIds(section: ConfigurationSection, path: String): Set<String> {
        if (!section.contains("place_ids")) return emptySet()
        require(section.isList("place_ids")) { "$path.place_ids trebuie sa fie o lista" }
        return section.getList("place_ids").orEmpty().map { value ->
            require(value is String && value.isNotBlank()) { "$path.place_ids trebuie sa contina ID-uri valide" }
            value.trim()
        }.toSet()
    }

    private fun items(section: ConfigurationSection, key: String, path: String): Map<Material, Int> {
        val itemSection = requireNotNull(section.getConfigurationSection(key)) { "$path.$key lipseste" }
        val entries = itemSection.getKeys(false).associate { name ->
            val material = material(name, "$path.$key.$name")
            val amount = integer(itemSection, name, "$path.$key")
            require(amount > 0) { "$path.$key.$name trebuie sa fie pozitiv" }
            material to amount
        }
        require(entries.isNotEmpty()) { "$path.$key nu poate fi gol" }
        return entries
    }

    private fun integer(section: ConfigurationSection, key: String, path: String, default: Int? = null): Int {
        if (!section.contains(key)) return requireNotNull(default) { "$path.$key lipseste" }
        require(section.isInt(key)) { "$path.$key trebuie sa fie un numar intreg" }
        return section.getInt(key)
    }

    private fun material(value: String?, path: String): Material {
        val material = value?.let(Material::matchMaterial)
        require(material != null && material !in setOf(Material.AIR, Material.CAVE_AIR, Material.VOID_AIR)) {
            "$path contine un material invalid"
        }
        return material
    }

    private inline fun <reified T : Enum<T>> enumValue(value: String?, path: String): T =
        enumValues<T>().firstOrNull { it.name == value?.uppercase(Locale.ROOT) }
            ?: throw IllegalArgumentException("$path contine o valoare invalida")
}
