package ro.ainpc.world.scan

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.scheduler.BukkitTask
import ro.ainpc.AINPCPlugin
import ro.ainpc.utils.ConfigKeys
import java.util.ArrayDeque
import java.util.logging.Level

class VanillaVillageScanService(
    private val plugin: AINPCPlugin,
    private val scanner: VanillaVillageScanner = VanillaVillageScanner(),
) {
    private val jobs = ArrayDeque<ScanJob>()
    private var task: BukkitTask? = null
    private var blockBudgetPerTick = DEFAULT_BLOCKS_PER_TICK

    init {
        reloadFromConfig()
    }

    fun reloadFromConfig() {
        val configured = plugin.config.getInt(ConfigKeys.WORLD_SCAN_BLOCKS_PER_TICK, DEFAULT_BLOCKS_PER_TICK)
        blockBudgetPerTick = normalizeVillageScanBlockBudget(configured)
        if (configured != blockBudgetPerTick) {
            plugin.logger.warning(
                "${ConfigKeys.WORLD_SCAN_BLOCKS_PER_TICK}=$configured a fost limitat la $blockBudgetPerTick."
            )
        }
    }

    fun submit(
        center: Location?,
        horizontalRadius: Int,
        verticalRadius: Int,
        onComplete: (VanillaVillageScanResult) -> Unit,
        onFailure: (Throwable) -> Unit,
    ): VanillaVillageScanTicket {
        check(Bukkit.isPrimaryThread()) {
            "Scanarile vanilla trebuie programate de pe thread-ul principal Bukkit."
        }
        val session = scanner.beginScan(center, horizontalRadius, verticalRadius)
        val ticket = VanillaVillageScanTicket(
            totalBlocks = session.totalBlocks,
            blockBudgetPerTick = blockBudgetPerTick,
            queuePosition = jobs.size + 1,
        )
        jobs.addLast(ScanJob(session, onComplete, onFailure))
        ensureTask()
        return ticket
    }

    fun shutdown() {
        task?.cancel()
        task = null
        jobs.clear()
    }

    private fun ensureTask() {
        if (task != null) return
        task = plugin.server.scheduler.runTaskTimer(plugin, Runnable { processTick() }, 1L, 1L)
    }

    private fun processTick() {
        check(Bukkit.isPrimaryThread()) {
            "Coada de scanare vanilla trebuie executata pe thread-ul principal Bukkit."
        }
        var remainingBudget = blockBudgetPerTick
        while (remainingBudget > 0 && jobs.isNotEmpty()) {
            val job = jobs.first()
            try {
                val processed = job.session.scanNext(remainingBudget)
                remainingBudget -= processed
                if (!job.session.isComplete) break

                jobs.removeFirst()
                complete(job)
            } catch (error: Throwable) {
                jobs.removeFirst()
                fail(job, error)
            }
        }
        if (jobs.isEmpty()) {
            task?.cancel()
            task = null
        }
    }

    private fun complete(job: ScanJob) {
        try {
            job.onComplete(job.session.result())
        } catch (error: Throwable) {
            fail(job, error)
        }
    }

    private fun fail(job: ScanJob, error: Throwable) {
        plugin.logger.log(Level.WARNING, "Scanarea vanilla incrementala a esuat.", error)
        try {
            job.onFailure(error)
        } catch (callbackError: Throwable) {
            plugin.logger.log(Level.WARNING, "Callback-ul de esec al scanarii vanilla a esuat.", callbackError)
        }
    }

    private data class ScanJob(
        val session: VanillaVillageScanSession,
        val onComplete: (VanillaVillageScanResult) -> Unit,
        val onFailure: (Throwable) -> Unit,
    )

    companion object {
        const val DEFAULT_BLOCKS_PER_TICK = 4096
        const val MIN_BLOCKS_PER_TICK = 256
        const val MAX_BLOCKS_PER_TICK = 65536
    }
}

data class VanillaVillageScanTicket(
    val totalBlocks: Long,
    val blockBudgetPerTick: Int,
    val queuePosition: Int,
)

internal fun normalizeVillageScanBlockBudget(configured: Int): Int =
    configured.coerceIn(
        VanillaVillageScanService.MIN_BLOCKS_PER_TICK,
        VanillaVillageScanService.MAX_BLOCKS_PER_TICK,
    )
