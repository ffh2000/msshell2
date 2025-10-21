package ru.mstrike.msshell2

import android.content.Context
import android.net.Uri
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString

class WebSocketTerminal(
    val context: Context,
    val optionsStorage: OptionsStorage
) : WebSocketListener() {

    var shell: Shell? = null
    private lateinit var webSocket: WebSocket
    public var connected = false
    var deviceInfo: Map<String, String> = mutableMapOf()
    lateinit var networkInfo: NetworkInfo
    val wsCommandExecListener = WSCommandExecListener(this)
    val outputLine = StringBuilder()

    fun connect() {
        Log.i(WebSocketTerminal.TAG, "Соединяюсь с ${Config.address}")
        val client = OkHttpClient()
        val request = Request.Builder()
            .url(Config.address)
            .build()
        webSocket = client.newWebSocket(request, this)
    }

    fun send(text: String) {
        if (text == "\n") {
            Log.d(TAG, "$outputLine")
            outputLine.clear()
        } else
            outputLine.append(text)
        webSocket.send(text)
    }

    override fun onOpen(webSocket: WebSocket, response: Response) {
        connected = true
        shell = Shell(wsCommandExecListener)
        Log.i(TAG, "✅ Соединение открыто")
        networkInfo = NetworkInfo()
        deviceInfo = networkInfo.getInfo()
        send("--- SHELL INFO ---\n")
        send("MAC:${deviceInfo[DeviceInfo.MAC_KEY]}\n")
        send("IP:${deviceInfo[DeviceInfo.IP_KEY]}\n")
        send("PROMPT:${deviceInfo[DeviceInfo.PROMPT_KEY]}\n")
        send("MSSHELL2_UUID:${optionsStorage.uuid}\n")
        send("PANEL_ID:${getMSVisionOption(MSVisionOption.CODE)}\n")
        send("--- END ---\n")
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        Log.i(TAG, text)
        shell?.sendToShell(text)
    }

    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
//        Log.i(TAG, "📦 Получены байты: ${bytes.hex()}")
        shell?.sendToShell(bytes.toString())
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        Log.i(TAG, "⚠️ Закрывается: $code / $reason")
        webSocket.close(1000, null)
        shell?.kill()
        shell = null
        connected = false
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        Log.i(TAG, "❌ Ошибка: ${t.message}")
        shell?.kill()
        shell = null
        connected = false
    }

    private fun getMSVisionOption(option: MSVisionOption): String? {
        try {
            val projection = arrayOf(option.optionName)
            val cursor =
                context.contentResolver.query(PROVIDER_URI, projection, null, null, null)
            cursor?.let {
                val colValue = it.getColumnIndex(COLUMN_VALUE)
                if (colValue >= 0) {
                    it.moveToFirst()
                    return it.getString(colValue)
                } else
                    return null
            } ?: return null
        } catch (th: Throwable) {
            return null
        }
    }

    companion object {
        const val TAG = "MSShell2"
        val PROVIDER_URI = Uri.parse("content://ru.mstrike.msvision.options")
        val COLUMN_VALUE = "value"
    }

}