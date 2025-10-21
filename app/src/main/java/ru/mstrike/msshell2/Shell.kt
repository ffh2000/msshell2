package ru.mstrike.msshell2

import android.util.Log
import ru.mstrike.msshell2.WebSocketTerminal.Companion.TAG
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.io.InterruptedIOException
import java.util.concurrent.TimeUnit

class Shell(
    val listener: CommandExecListener
) {

    var process: Process? = null
    lateinit var outputStreamThread: Thread
    lateinit var outputErrorStreamThread: Thread
    var outputStream: DataOutputStream? = null

    val shellTread = Thread({
        process = Runtime.getRuntime().exec("sh")
        process?.let { process ->
            outputStream = DataOutputStream(process.outputStream)
            var reader = BufferedReader(InputStreamReader(process.inputStream))
            var errorReader = BufferedReader(InputStreamReader(process.errorStream))
            outputStreamThread = Thread({
                try {
                    reader.use {
                        var ch: Int = it.read()
                        while (ch != -1 && !Thread.currentThread().isInterrupted) {
                            listener.onOutput(ch.toChar().toString())
                            ch = it.read()
                        }
                    }
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                    Log.i(TAG, "❌ Поток outputStreamThread: ${Thread.currentThread()} прерван.")
                    reader.close()
                } catch (e: InterruptedIOException) {
                    Thread.currentThread().interrupt()
                    Log.i(TAG, "❌ Поток outputStreamThread: ${Thread.currentThread()} прерван.")
                    reader.close()
                }
            }, "outputStreamThread")
            outputErrorStreamThread = Thread({
                try {
                    errorReader.use {
                        var ch: Int = it.read()
                        while (ch != -1 && !Thread.currentThread().isInterrupted) {
                            listener.onErrorOutput(ch.toChar().toString())
                            ch = it.read()
                        }
                    }
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                    Log.i(
                        TAG,
                        "❌ Поток outputErrorStreamThread: ${Thread.currentThread()} прерван."
                    )
                    errorReader.close()
                } catch (e: InterruptedIOException) {
                    Thread.currentThread().interrupt()
                    Log.i(TAG, "❌ Поток outputStreamThread: ${Thread.currentThread()} прерван.")
                    errorReader.close()
                }
            }, "outputErrorStreamThread")
            outputStreamThread.start()
            outputErrorStreamThread.start()
//            outputStream.writeBytes("exit\n")
//            outputStream.flush()
            try {
                process.waitFor()
                outputStreamThread.join()
                outputErrorStreamThread.join()
            } catch (e: InterruptedException) {
                Log.i(TAG, "❌ Поток shellTread: ${Thread.currentThread()} прерван.")
                Thread.currentThread().interrupt()
            } catch (e: Throwable) {
                Log.i(TAG, "❌ Поток shellTread: неизвестная ошибка: ${e.message}")
                Thread.currentThread().interrupt()
            }
            outputStreamThread.interrupt()
            outputErrorStreamThread.interrupt()
            process?.let { process ->
                if (process.isAlive()) {
                    process.destroyForcibly() // Гарантированно завершает процесс
                    try {
                        process.waitFor(500, TimeUnit.MILLISECONDS) // Подождать завершения
                    } catch (e: Throwable) {
                    }
                }
            }
            this@Shell.process = null
            Log.i(TAG, "❌ Поток shellTread: завершил работу")
        }
    }, "shellTread")

    init {
        shellTread.start()
    }

    /**
     * В конце команды надо дописывать "&& exit\n" иначе не произойдет выхода из процесса
     * и подвиснет навсегда.
     */
    fun execSuShell(command: String) {
        outputStream?.writeBytes(command)
        outputStream?.flush()
    }

    fun kill() {
        shellTread.interrupt()
    }

}