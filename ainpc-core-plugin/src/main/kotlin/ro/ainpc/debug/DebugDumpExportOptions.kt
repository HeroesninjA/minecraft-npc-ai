package ro.ainpc.debug

/**
 * Debugdumpprivacymode.
 */
enum class DebugDumpPrivacyMode(val cliValue: String) {
    STANDARD("standard"),
    PRIVACY_SAFE("privacy-safe"),
}

/**
 * Debugdumpexportoptions.
 */
data class DebugDumpExportOptions(
    val playerUuidFragment: String? = null,
    val privacyMode: DebugDumpPrivacyMode = DebugDumpPrivacyMode.STANDARD,
) {
    companion object {
        @JvmStatic
        fun parse(arguments: List<String>, scope: String): DebugDumpExportOptions {
            require(arguments.size <= 2) { "Exportul accepta cel mult un fragment UUID si optiunea privacy-safe." }
            var playerUuidFragment: String? = null
            var privacyMode = DebugDumpPrivacyMode.STANDARD
            arguments.forEach { rawArgument ->
                val argument = rawArgument.trim()
                require(argument.isNotEmpty()) { "Argumentele goale nu sunt acceptate." }
                if (argument.equals(DebugDumpPrivacyMode.PRIVACY_SAFE.cliValue, ignoreCase = true)) {
                    require(privacyMode == DebugDumpPrivacyMode.STANDARD) { "privacy-safe a fost specificat de doua ori." }
                    privacyMode = DebugDumpPrivacyMode.PRIVACY_SAFE
                } else {
                    require(scope.equals("all", ignoreCase = true)) {
                        "Filtrul UUID este disponibil doar pentru /ainpc debugdump all."
                    }
                    require(playerUuidFragment == null) { "Exportul accepta un singur fragment UUID." }
                    DebugDumpPlayerUuidFilter.requireValid(argument)
                    playerUuidFragment = argument
                }
            }
            return DebugDumpExportOptions(playerUuidFragment, privacyMode)
        }
    }
}
