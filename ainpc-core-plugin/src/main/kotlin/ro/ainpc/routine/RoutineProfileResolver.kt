package ro.ainpc.routine

import ro.ainpc.AINPCPlugin
import ro.ainpc.npc.AINPC

class RoutineProfileResolver(private val plugin: AINPCPlugin? = null) {
    private val pluginProfileLoader: BehaviorProfileLoader? by lazy { plugin?.let { BehaviorProfileLoader(it) } }
    private val builtInProfileLoader: BehaviorProfileLoader? by lazy { loadBuiltInProfileLoader() }

    fun resolveProfile(npc: AINPC?): BehaviorProfile? {
        val loader = profileLoader()
        if (loader != null && plugin != null) {
            loader.loadAll()
        }
        val occupation = npc?.occupation
        val loadedProfile = if (occupation != null) loader?.findProfileForOccupation(occupation) else null
        return loadedProfile ?: loader?.defaultProfile()
    }

    fun defaultProfile(): BehaviorProfile? {
        return profileLoader()?.defaultProfile()
    }

    fun previewPoints(npc: AINPC?): List<BehaviorProfile.PreviewPoint> {
        val profile = resolveProfile(npc)
        val defaultProfile = defaultProfile()
        return profile?.previewPoints?.takeIf { it.isNotEmpty() }
            ?: defaultProfile?.previewPoints?.takeIf { it.isNotEmpty() }
            ?: emptyList()
    }

    private fun profileLoader(): BehaviorProfileLoader? {
        return pluginProfileLoader ?: builtInProfileLoader
    }

    private fun loadBuiltInProfileLoader(): BehaviorProfileLoader? {
        val yaml = javaClass.getResourceAsStream("/behavior_profiles.yml")?.use { input ->
            input.readBytes().toString(Charsets.UTF_8)
        } ?: return null
        return BehaviorProfileLoader(null).also { it.parseYamlString(yaml) }
    }
}
