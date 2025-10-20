package ru.mstrike.msshell2

import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.NetworkInterface
import kotlin.collections.iterator

/**
 * Реализация [DeviceInfo] для получения сетевых настроек устройства
 */
class NetworkInfo : DeviceInfo {

    override fun getInfo(): Map<String, String> {
        val result = HashMap<String, String>()
        getInterfaceInfo(result)
        getPrompt(result)
        return result
    }

    private fun getPrompt(map: HashMap<String, String>) {
        try {
            val process = Runtime.getRuntime().exec("sh")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val writer = BufferedWriter(OutputStreamWriter(process.outputStream))
            // Отправим команду, например 'echo PROMPT'
            writer.write("echo \$PS1\n")  // $PS1 - стандартная переменная оболочки Linux для prompt
            writer.flush()
            val line = reader.readLine()
            map[DeviceInfo.PROMPT_KEY] = line + " "
            writer.close()
            reader.close()
            process.destroy()
        } catch (th: Throwable) {
            map[DeviceInfo.PROMPT_KEY] = "$ "
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun getInterfaceInfo(map: HashMap<String, String>) {
        try {
            val reg = "\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}".toRegex()
            val interfaces = NetworkInterface.getNetworkInterfaces()
            for (i in interfaces)
                for (address in i.inetAddresses)
                    if (!address.isLoopbackAddress && reg.matches(address.hostAddress)) {
                        val l = mutableListOf<String>()
                        for (bt in i.hardwareAddress) l.add(bt.toHexString().uppercase())
                        map[DeviceInfo.IP_KEY] = address.hostAddress ?: ""
                        map[DeviceInfo.MAC_KEY] = l.joinToString(":")
                    }
        } catch (_: Exception) {
        }
    }
}