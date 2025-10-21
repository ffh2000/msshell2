package ru.mstrike.msshell2

import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.io.InterruptedIOException
import java.io.Reader
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
            outputStreamThread = createOutputStreamThread(reader)
            outputErrorStreamThread = createOutputErrorStreamThread(errorReader)
            outputStreamThread.start()
            outputErrorStreamThread.start()
            try {
                process.waitFor()
                outputStreamThread.join()
                outputErrorStreamThread.join()
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
            } catch (e: Throwable) {
                Thread.currentThread().interrupt()
            }
            if (process.isAlive) {
                process.destroyForcibly() // Гарантированно завершает процесс
                try {
                    process.waitFor(500, TimeUnit.MILLISECONDS) // Подождать завершения
                } catch (e: Throwable) {
                }
            }
            outputStreamThread.interrupt()
            outputErrorStreamThread.interrupt()
            this@Shell.process = null
        }
    }, "shellTread")

    init {
        shellTread.start()
    }

    /**
     * Создает поток (нить), обслуживающий поток вывода в консоль.
     */
    private fun createOutputStreamThread(reader: Reader): Thread {
        return Thread({
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
                reader.close()
            } catch (e: InterruptedIOException) {
                Thread.currentThread().interrupt()
                reader.close()
            }
        }, "outputStreamThread")
    }

    private fun createOutputErrorStreamThread(errorReader: Reader): Thread {
        return Thread({
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
                errorReader.close()
            } catch (e: InterruptedIOException) {
                Thread.currentThread().interrupt()
                errorReader.close()
            }
        }, "outputErrorStreamThread")
    }

    /**
     * Пришедший символ или строку направляет в поток ввода процесса
     */
    fun sendToShell(command: String) {
        outputStream?.writeBytes(command)
        outputStream?.flush()
    }

    /**
     * Останавливает текущую консоль.
     *
     * После этого ее надо пересоздавать. Восставноить работу нельзя. т.е. это деструктор.
     */
    fun kill() {
        shellTread.interrupt()
    }

}