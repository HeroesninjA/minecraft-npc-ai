package ro.ainpc.api

interface NpcEconomyApi {
    fun getBalance(npcKey: String): Int
    fun getSalary(occupation: String?): Int
    fun paySalaryForWork(npcUuid: java.util.UUID, npcDbId: Int): Boolean
    fun deposit(npcKey: String, amount: Int): Boolean
    fun withdraw(npcKey: String, amount: Int): Boolean
    fun getBalanceCount(): Int
    fun getTotalEconomyValue(): Int
}
