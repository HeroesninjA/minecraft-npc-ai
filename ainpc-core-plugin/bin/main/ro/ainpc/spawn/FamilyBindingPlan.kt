package ro.ainpc.spawn

import ro.ainpc.npc.AINPC

class FamilyBindingPlan(
    familyId: String?,
    members: List<Member>?
) {
    private val familyIdValue = clean(familyId)
    private val membersValue = java.util.List.copyOf(members ?: emptyList())

    fun familyId(): String = familyIdValue

    fun members(): List<Member> = membersValue

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is FamilyBindingPlan) {
            return false
        }

        return familyIdValue == other.familyIdValue &&
            membersValue == other.membersValue
    }

    override fun hashCode(): Int {
        var result = familyIdValue.hashCode()
        result = 31 * result + membersValue.hashCode()
        return result
    }

    override fun toString(): String =
        "FamilyBindingPlan[familyId=$familyIdValue, members=$membersValue]"

    class Member(
        npcKey: String?,
        private val npcIdValue: Int,
        npcName: String?,
        familyRole: String?
    ) {
        private val npcKeyValue = clean(npcKey)
        private val npcNameValue = clean(npcName)
        private val familyRoleValue = clean(familyRole)

        fun npcKey(): String = npcKeyValue

        fun npcId(): Int = npcIdValue

        fun npcName(): String = npcNameValue

        fun familyRole(): String = familyRoleValue

        override fun equals(other: Any?): Boolean {
            if (this === other) {
                return true
            }
            if (other !is Member) {
                return false
            }

            return npcKeyValue == other.npcKeyValue &&
                npcIdValue == other.npcIdValue &&
                npcNameValue == other.npcNameValue &&
                familyRoleValue == other.familyRoleValue
        }

        override fun hashCode(): Int {
            var result = npcKeyValue.hashCode()
            result = 31 * result + npcIdValue
            result = 31 * result + npcNameValue.hashCode()
            result = 31 * result + familyRoleValue.hashCode()
            return result
        }

        override fun toString(): String =
            "Member[npcKey=$npcKeyValue, npcId=$npcIdValue, npcName=$npcNameValue, familyRole=$familyRoleValue]"
    }

    companion object {
        @JvmStatic
        fun member(npcKey: String?, npc: AINPC?, familyRole: String?): Member =
            Member(
                clean(npcKey),
                npc?.databaseId ?: 0,
                npc?.name ?: "",
                familyRole
            )

        private fun clean(value: String?): String = value?.trim().orEmpty()
    }
}
