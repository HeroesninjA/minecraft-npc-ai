package ro.ainpc.routine

import com.destroystokyo.paper.entity.Pathfinder
import org.bukkit.Location

internal object RoutineNavigation {
    fun start(pathfinder: Pathfinder, target: Location, speed: Double, arrivalRadius: Double): Boolean {
        if (!isValid(target) || !speed.isFinite() || speed <= 0.0 || !arrivalRadius.isFinite() || arrivalRadius <= 0.0) return false
        pathfinder.setCanOpenDoors(true)
        pathfinder.setCanPassDoors(true)
        val path = pathfinder.findPath(target) ?: return false
        if (!path.canReachFinalPoint()) return false
        val endpoint = path.finalPoint ?: return false
        if (!isValid(endpoint) || endpoint.world != target.world || endpoint.distanceSquared(target) > arrivalRadius * arrivalRadius) return false
        return pathfinder.moveTo(path, speed)
    }

    private fun isValid(location: Location): Boolean =
        location.world != null && location.x.isFinite() && location.y.isFinite() && location.z.isFinite()
}
