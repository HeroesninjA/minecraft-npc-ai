package ro.ainpc.environment

data class EnvironmentContext(
    val worldName: String,
    val timeOfDay: TimeOfDay,
    val weather: Weather,
    val season: Season,
    val lightLevel: Int,
    val temperature: Temperature,
    val specialEvents: List<String>,
    val dayNumber: Int,
    val tickOfDay: Long
) {
    fun isNight(): Boolean = timeOfDay == TimeOfDay.NIGHT || timeOfDay == TimeOfDay.LATE_NIGHT
    fun isDay(): Boolean = !isNight()
    fun isStorming(): Boolean = weather == Weather.THUNDER
    fun isRaining(): Boolean = weather == Weather.RAIN || weather == Weather.THUNDER
    fun isExtreme(): Boolean = weather == Weather.THUNDER || temperature == Temperature.EXTREME_HEAT || temperature == Temperature.FREEZING

    fun toDescription(): String = buildString {
        append("Este $timeOfDay. ")
        append("Vremea: $weather. ")
        append("Anotimp: $season. ")
        append("Temperatura: $temperature. ")
        if (specialEvents.isNotEmpty()) {
            append("Evenimente: ${specialEvents.joinToString(", ")}. ")
        }
    }

    fun toPromptBlock(): String = buildString {
        appendLine("[Environment]")
        appendLine("time=$timeOfDay weather=$weather season=$season temperature=$temperature")
        if (specialEvents.isNotEmpty()) {
            appendLine("events=${specialEvents.joinToString(",")}")
        }
    }

    enum class TimeOfDay(val id: String, val displayName: String) {
        DAWN("dawn", "Zori"),
        MORNING("morning", "Dimineata"),
        AFTERNOON("afternoon", "Dupa-amiaza"),
        EVENING("evening", "Seara"),
        NIGHT("night", "Noapte"),
        LATE_NIGHT("late_night", "Miezul noptii");

        companion object {
            fun fromTick(tick: Long): TimeOfDay = when (tick % 24000) {
                in 0L..999L -> DAWN
                in 1000L..5999L -> MORNING
                in 6000L..11999L -> AFTERNOON
                in 12000L..13999L -> EVENING
                in 14000L..17999L -> NIGHT
                else -> LATE_NIGHT
            }
        }
    }

    enum class Weather(val id: String, val displayName: String) {
        CLEAR("clear", "Senin"),
        RAIN("rain", "Ploaie"),
        THUNDER("thunder", "Furtuna"),
        SNOW("snow", "Ninsoare"),
        FOG("fog", "Ceata");

        companion object {
            fun fromBukkit(hasStorm: Boolean, isThundering: Boolean, biome: String?): Weather {
                if (isThundering) return THUNDER
                if (hasStorm) {
                    if (biome != null && (biome.contains("SNOW") || biome.contains("ICE") || biome.contains("TAIGA") || biome.contains("TUNDRA"))) return SNOW
                    return RAIN
                }
                return CLEAR
            }
        }
    }

    enum class Season(val id: String, val displayName: String) {
        SPRING("spring", "Primavara"),
        SUMMER("summer", "Vara"),
        AUTUMN("autumn", "Toamna"),
        WINTER("winter", "Iarna");

        companion object {
            fun fromDay(dayNumber: Int): Season {
                val dayOfYear = ((dayNumber % 365) + 365) % 365
                return when (dayOfYear) {
                    in 0..59 -> WINTER
                    in 60..151 -> SPRING
                    in 152..242 -> SUMMER
                    in 243..333 -> AUTUMN
                    else -> WINTER
                }
            }
        }
    }

    enum class Temperature(val id: String, val displayName: String) {
        FREEZING("freezing", "Inghet"),
        COLD("cold", "Rece"),
        MILD("mild", "Blând"),
        WARM("warm", "Cald"),
        HOT("hot", "Fierbinte"),
        EXTREME_HEAT("extreme_heat", "Arzator");

        companion object {
            fun fromBiome(biome: String?, season: Season): Temperature {
                val biomeUpper = biome?.uppercase() ?: ""
                return when {
                    biomeUpper.contains("DESERT") || biomeUpper.contains("SAVANNA") || biomeUpper.contains("BADLANDS") ->
                        if (season == Season.SUMMER) EXTREME_HEAT else HOT
                    biomeUpper.contains("SNOW") || biomeUpper.contains("ICE") || biomeUpper.contains("FROZEN") || biomeUpper.contains("GLACIER") ->
                        if (season == Season.WINTER) FREEZING else COLD
                    biomeUpper.contains("TAIGA") || biomeUpper.contains("TUNDRA") || biomeUpper.contains("MOUNTAINS") ->
                        if (season == Season.WINTER) FREEZING else COLD
                    biomeUpper.contains("OCEAN") || biomeUpper.contains("RIVER") || biomeUpper.contains("SWAMP") ->
                        MILD
                    biomeUpper.contains("JUNGLE") ->
                        if (season == Season.SUMMER) EXTREME_HEAT else HOT
                    biomeUpper.contains("FOREST") || biomeUpper.contains("PLAINS") || biomeUpper.contains("MEADOW") ->
                        when (season) {
                            Season.WINTER -> COLD
                            Season.SPRING -> MILD
                            Season.SUMMER -> WARM
                            Season.AUTUMN -> MILD
                        }
                    else -> MILD
                }
            }
        }
    }

    companion object {
        fun empty(worldName: String = ""): EnvironmentContext = EnvironmentContext(
            worldName = worldName,
            timeOfDay = TimeOfDay.MORNING,
            weather = Weather.CLEAR,
            season = Season.SPRING,
            lightLevel = 15,
            temperature = Temperature.MILD,
            specialEvents = emptyList(),
            dayNumber = 0,
            tickOfDay = 0L
        )
    }
}
