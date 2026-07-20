package ro.ainpc.debug

enum class DebugDumpPrivacyMode(val cliValue: String) {
    STANDARD("standard"),
    PRIVACY_SAFE("privacy-safe"),
}

data class DebugDumpExportOptions(
    val playerName: String? = null,
    val privacyMode: DebugDumpPrivacyMode = DebugDumpPrivacyMode.STANDARD,
) {
    companion object {
        @JvmStatic
        fun parse(arguments: List<String>): DebugDumpExportOptions {
            require(arguments.size <= 2) { "Exportul accepta cel mult un player si optiunea privacy-safe." }
            var playerName: String? = null
            var privacyMode = DebugDumpPrivacyMode.STANDARD
            arguments.forEach { rawArgument ->
                val argument = rawArgument.trim()
                require(argument.isNotEmpty()) { "Argumentele goale nu sunt acceptate." }
                if (argument.equals(DebugDumpPrivacyMode.PRIVACY_SAFE.cliValue, ignoreCase = true)) {
                    require(privacyMode == DebugDumpPrivacyMode.STANDARD) { "privacy-safe a fost specificat de doua ori." }
                    privacyMode = DebugDumpPrivacyMode.PRIVACY_SAFE
                } else {
                    require(playerName == null) { "Exportul accepta un singur filtru de player." }
                    playerName = argument
                }
            }
            return DebugDumpExportOptions(playerName, privacyMode)
        }
    }
}
