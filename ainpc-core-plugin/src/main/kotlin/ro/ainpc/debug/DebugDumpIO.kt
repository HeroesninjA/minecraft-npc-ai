package ro.ainpc.debug

import com.google.gson.Gson
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

object DebugDumpIO {
    @JvmStatic
    @Throws(IOException::class)
    fun writeJson(path: Path, value: Any, gson: Gson) {
        writeText(path, gson.toJson(value))
    }

    @JvmStatic
    @Throws(IOException::class)
    fun writeText(path: Path, content: String) {
        Files.writeString(path, content, StandardCharsets.UTF_8)
    }

    @JvmStatic
    fun readRecentServerLog(dataFolderPath: Path, recentLogLines: Int): String {
        var serverRoot: Path? = dataFolderPath.parent
        if (serverRoot != null) {
            serverRoot = serverRoot.parent
        }
        if (serverRoot == null) {
            return "Server root indisponibil.\n"
        }

        val latestLog = serverRoot.resolve("logs").resolve("latest.log")
        if (!Files.exists(latestLog)) {
            return "latest.log indisponibil la ${latestLog.toAbsolutePath()}\n"
        }

        return try {
            val lines = Files.readAllLines(latestLog, StandardCharsets.UTF_8)
            val fromIndex = maxOf(0, lines.size - recentLogLines)
            lines.subList(fromIndex, lines.size).joinToString(System.lineSeparator()) + System.lineSeparator()
        } catch (exception: IOException) {
            "Nu pot citi latest.log: ${exception.message}\n"
        }
    }
}
