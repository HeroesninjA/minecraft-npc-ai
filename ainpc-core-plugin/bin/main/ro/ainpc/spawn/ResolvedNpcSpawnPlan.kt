package ro.ainpc.spawn

import org.bukkit.Location
import ro.ainpc.npc.AINPC

class ResolvedNpcSpawnPlan(
    plan: NpcSpawnPlan?,
    spawnLocation: Location?,
    homeAnchor: AINPC.OwnedLocation?,
    workAnchor: AINPC.OwnedLocation?,
    socialAnchor: AINPC.OwnedLocation?
) {
    private val planValue: NpcSpawnPlan = plan ?: throw NullPointerException("plan")
    private val spawnLocationValue = spawnLocation?.clone()
    private val homeAnchorValue = homeAnchor
    private val workAnchorValue = workAnchor
    private val socialAnchorValue = socialAnchor

    fun plan(): NpcSpawnPlan = planValue

    fun spawnLocation(): Location? = spawnLocationValue

    fun homeAnchor(): AINPC.OwnedLocation? = homeAnchorValue

    fun workAnchor(): AINPC.OwnedLocation? = workAnchorValue

    fun socialAnchor(): AINPC.OwnedLocation? = socialAnchorValue

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is ResolvedNpcSpawnPlan) {
            return false
        }

        return planValue == other.planValue &&
            spawnLocationValue == other.spawnLocationValue &&
            homeAnchorValue == other.homeAnchorValue &&
            workAnchorValue == other.workAnchorValue &&
            socialAnchorValue == other.socialAnchorValue
    }

    override fun hashCode(): Int {
        var result = planValue.hashCode()
        result = 31 * result + (spawnLocationValue?.hashCode() ?: 0)
        result = 31 * result + (homeAnchorValue?.hashCode() ?: 0)
        result = 31 * result + (workAnchorValue?.hashCode() ?: 0)
        result = 31 * result + (socialAnchorValue?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String =
        "ResolvedNpcSpawnPlan[plan=$planValue, spawnLocation=$spawnLocationValue, homeAnchor=$homeAnchorValue, " +
            "workAnchor=$workAnchorValue, socialAnchor=$socialAnchorValue]"
}
