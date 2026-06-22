package ro.ainpc.api.settlement

data class SettlementLayoutProfile(
    val profileId: String,
    val displayName: String,
    val houseDensity: Double,
    val marketSize: Int,
    val roadWidth: Int,
    val minDistanceBetweenHouses: Double,
    val minDistanceToMarket: Double,
    val workplaceRatio: Double,
    val socialSpotsPerCapita: Double,
    val description: String = ""
) {
    companion object {
        @JvmStatic
        fun compact(): SettlementLayoutProfile = SettlementLayoutProfile(
            profileId = "compact",
            displayName = "Compact",
            houseDensity = 0.7,
            marketSize = 3,
            roadWidth = 3,
            minDistanceBetweenHouses = 4.0,
            minDistanceToMarket = 20.0,
            workplaceRatio = 0.3,
            socialSpotsPerCapita = 0.1,
            description = "Asezare inghesuita, case apropiate, potrivita pentru sate fortificate."
        )

        @JvmStatic
        fun spacious(): SettlementLayoutProfile = SettlementLayoutProfile(
            profileId = "spacious",
            displayName = "Spacious",
            houseDensity = 0.3,
            marketSize = 5,
            roadWidth = 5,
            minDistanceBetweenHouses = 12.0,
            minDistanceToMarket = 40.0,
            workplaceRatio = 0.4,
            socialSpotsPerCapita = 0.2,
            description = "Aseare aerisita, case spatioase, drumuri largi, potrivita pentru sate linistite."
        )

        @JvmStatic
        fun rural(): SettlementLayoutProfile = SettlementLayoutProfile(
            profileId = "rural",
            displayName = "Rural",
            houseDensity = 0.15,
            marketSize = 4,
            roadWidth = 3,
            minDistanceBetweenHouses = 20.0,
            minDistanceToMarket = 60.0,
            workplaceRatio = 0.6,
            socialSpotsPerCapita = 0.15,
            description = "Asezare imprastiata, cu ferme si gradini mari, potrivita pentru sate agricole."
        )

        @JvmStatic
        fun fortified(): SettlementLayoutProfile = SettlementLayoutProfile(
            profileId = "fortified",
            displayName = "Fortificat",
            houseDensity = 0.6,
            marketSize = 4,
            roadWidth = 4,
            minDistanceBetweenHouses = 6.0,
            minDistanceToMarket = 25.0,
            workplaceRatio = 0.35,
            socialSpotsPerCapita = 0.1,
            description = "Asezare protejata, cu ziduri si drumuri inguste, potrivita pentru avanposturi."
        )
    }
}
