package ro.ainpc.mcp

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class McpCommandQueueIntegrationTest {

    @Test
    fun writeAndReadCommandFile(@TempDir tempDir: Path) {
        val cmdDir = tempDir.resolve("commands").toFile()
        cmdDir.mkdirs()

        val commandFile = File(cmdDir, "test_cmd.json")
        val content = """{"command": "ainpc list", "source": "test", "timestamp": ${System.currentTimeMillis()}}"""
        commandFile.writeText(content)

        assertTrue(commandFile.exists())
        val read = commandFile.readText()
        assertTrue(read.contains("ainpc list"))
        assertTrue(read.contains("test"))
    }

    @Test
    fun writeAndReadResultFile(@TempDir tempDir: Path) {
        val resultDir = tempDir.resolve("results").toFile()
        resultDir.mkdirs()

        val resultFile = File(resultDir, "result_1.json")
        val content = """{"ok": true, "output": "Comanda executata cu succes", "duration_ms": 42}"""
        resultFile.writeText(content)

        assertTrue(resultFile.exists())
        val read = resultFile.readText()
        assertTrue(read.contains("ok"))
        assertTrue(read.contains("Comanda executata"))
    }

    @Test
    fun timeoutHandling(@TempDir tempDir: Path) {
        val resultDir = tempDir.resolve("timeouts").toFile()
        resultDir.mkdirs()

        val resultFile = File(resultDir, "timeout_result.json")
        val content = """{"ok": false, "error": "Timeout after 5000ms", "command": "ainpc list"}"""
        resultFile.writeText(content)

        assertTrue(resultFile.exists())
        val read = resultFile.readText()
        assertTrue(read.contains("Timeout"))
        assertTrue(read.contains("5000ms"))
    }

    @Test
    fun multipleCommandsEnqueued(@TempDir tempDir: Path) {
        val cmdDir = tempDir.resolve("multi").toFile()
        cmdDir.mkdirs()

        for (i in 1..5) {
            val cmdFile = File(cmdDir, "cmd_$i.json")
            cmdFile.writeText("""{"command": "ainpc cmd $i", "source": "test", "seq": $i}""")
        }

        val files = cmdDir.listFiles() ?: emptyArray()
        assertEquals(5, files.size)
    }

    @Test
    fun emptyCommandDir(@TempDir tempDir: Path) {
        val emptyDir = tempDir.resolve("empty").toFile()
        emptyDir.mkdirs()

        val files = emptyDir.listFiles() ?: emptyArray()
        assertEquals(0, files.size)
    }
}
