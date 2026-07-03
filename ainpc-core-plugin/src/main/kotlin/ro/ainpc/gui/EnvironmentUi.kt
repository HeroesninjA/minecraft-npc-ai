package ro.ainpc.gui

import org.bukkit.Material
import ro.ainpc.environment.EnvironmentContext

object EnvironmentUi {
    fun icon(env: EnvironmentContext): Material = when {
        env.isExtreme() -> Material.REDSTONE_BLOCK
        env.isStorming() -> Material.REDSTONE_TORCH
        env.isRaining() -> Material.WATER_BUCKET
        env.timeOfDay == EnvironmentContext.TimeOfDay.NIGHT || env.timeOfDay == EnvironmentContext.TimeOfDay.LATE_NIGHT -> Material.CLOCK
        env.weather == EnvironmentContext.Weather.SNOW -> Material.SNOW_BLOCK
        env.season == EnvironmentContext.Season.WINTER -> Material.ICE
        env.season == EnvironmentContext.Season.SPRING -> Material.CHERRY_SAPLING
        env.season == EnvironmentContext.Season.SUMMER -> Material.SUNFLOWER
        env.season == EnvironmentContext.Season.AUTUMN -> Material.RED_MUSHROOM
        else -> Material.COMPASS
    }

    fun title(env: EnvironmentContext): String = "&bMediu: ${env.season.displayName}"

    fun lore(env: EnvironmentContext): List<String> = listOf(
        "&7Timp: &f${env.timeOfDay.displayName}",
        "&7Vreme: &f${env.weather.displayName}",
        "&7Anotimp: &f${env.season.displayName}",
        "&7Temperatura: &f${env.temperature.displayName}",
        "&7Ziua: &f${env.dayNumber}",
        if (env.specialEvents.isNotEmpty()) {
            "&7Evenimente: &f${env.specialEvents.joinToString(", ")}"
        } else {
            "&8Fara evenimente active"
        },
        "&8Click: /ainpc environment"
    )
}
