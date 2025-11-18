package ru.mstrike.msshell2.shell

import android.util.Log
import kotlinx.coroutines.*
import java.io.InputStream
import java.io.OutputStream

/**
 * Сессия, работающая с процессом командной строки
 */
class AndroidShellSession(
    val sessionId: String,
    private val shellType: String = "sh"
) {

    var isActive = true
    private var process: Process? = null
    private var outputReader: Job? = null
    private var errorReader: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null
    private var errorStream: InputStream? = null

    fun start(rows: Int, cols: Int, onOutput: (ByteArray) -> Unit, onExit: (Int) -> Unit) {
        Log.d(this.javaClass.name, "Starting shell session: $sessionId (shell: $shellType, size: ${cols}x${rows})")

        try {
            // Запуск shell процесса
            // Android не поддерживает PTY, используем обычный ProcessBuilder
            process = ProcessBuilder("/system/bin/$shellType", "-i")
                .redirectErrorStream(true) // Объединяем stderr с stdout
                .start()

            outputStream = process!!.outputStream
            inputStream = process!!.inputStream
            errorStream = process!!.errorStream

            // Читаем output постоянно
            outputReader = scope.launch {
                readStream(inputStream!!, "stdout", onOutput)
            }

            // Ждем завершения процесса
            scope.launch {
                try {
                    val exitCode = process!!.waitFor()
                    Log.d(this.javaClass.name, "Shell session $sessionId exited with code: $exitCode")
                    onExit(exitCode)
                } catch (e: Exception) {
                    Log.e(this.javaClass.name, "Error waiting for shell process\n$e")
                    onExit(-1)
                }
            }

            Log.d(this.javaClass.name, "Shell session $sessionId started successfully")
        } catch (e: Exception) {
            Log.e(this.javaClass.name, "Failed to start shell session: $sessionId\n$e")
            onExit(-1)
        }
    }

    private suspend fun readStream(
        stream: InputStream,
        streamName: String,
        onOutput: (ByteArray) -> Unit
    ) {
        val buffer = ByteArray(4096)
        try {
            while (isActive) {
                val bytesRead = withContext(Dispatchers.IO) {
                    stream.read(buffer)
                }

                if (bytesRead == -1) {
                    Log.d(this.javaClass.name, "$streamName: EOF reached for session $sessionId")
                    break
                }

                if (bytesRead > 0) {
                    val data = buffer.copyOf(bytesRead)
                    onOutput(data)
                }
            }
        } catch (e: Exception) {
            if (e !is CancellationException) {
                Log.e(this.javaClass.name, "Error reading $streamName for session $sessionId\n$e")
            }
        }
    }

    fun writeInput(data: ByteArray) {
        try {
            outputStream?.write(data)
            outputStream?.flush()
        } catch (e: Exception) {
            Log.e(this.javaClass.name, "Failed to write input to shell session: $sessionId\n$e")
        }
    }

    fun resize(rows: Int, cols: Int) {
        // Android не поддерживает PTY resize нативно
        // Это ограничение без root или библиотеки Termux
        Log.i(this.javaClass.name, "Resize not supported on Android without PTY (session: $sessionId)")
    }

    fun close() {
        Log.d(this.javaClass.name, "Closing shell session: $sessionId")

        outputReader?.cancel()
        errorReader?.cancel()

        try {
            outputStream?.close()
            inputStream?.close()
            errorStream?.close()
        } catch (e: Exception) {
            Log.e(this.javaClass.name, "Error closing streams\n$e")
        }

        process?.destroy()

        // Даем процессу 2 секунды на graceful shutdown
        scope.launch {
            delay(2000)
            process?.destroyForcibly()
        }

        scope.cancel()
    }

    fun isAlive(): Boolean = process?.isAlive ?: false
}
