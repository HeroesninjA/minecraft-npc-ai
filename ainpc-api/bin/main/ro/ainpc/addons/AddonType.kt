package ro.ainpc.addons

enum class AddonType(
    val id: String,
    val displayName: String
) {
    CORE("core", "Nucleu platforma"),
    SCENARIO("scenario", "Scenariu principal"),
    STORY("story", "Story addon"),
    RESOURCE("resource", "Addon de resurse"),
    TEXTURE("texture", "Addon de textura"),
    DATAPACK("datapack", "Compatibilitate datapack"),
    FEATURE("feature", "Feature addon"),
    INTEGRATION("integration", "Addon de integrare");

    companion object {
        @JvmStatic
        fun fromId(value: String?): AddonType {
            if (value.isNullOrBlank()) {
                return FEATURE
            }

            val normalized = value.trim().lowercase()
            when (normalized) {
                "resources", "resource_pack", "resource-pack" -> return RESOURCE
                "textures", "texture_pack", "texture-pack" -> return TEXTURE
                "resource_texture", "resource-texture", "resources_textures", "resources-textures" -> return RESOURCE
                "data_pack", "data-pack" -> return DATAPACK
            }

            for (type in entries) {
                if (type.id.equals(value, ignoreCase = true) || type.name.equals(value, ignoreCase = true)) {
                    return type
                }
            }

            return FEATURE
        }
    }
}
