package ru.mstrike.msshell2

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString

class WebSocketTerminal(
    val optionsStorage: OptionsStorage
) : WebSocketListener() {

    private lateinit var webSocket: WebSocket
    public var connected = false
    var deviceInfo: Map<String, String> = mutableMapOf()
    lateinit var networkInfo: NetworkInfo

    fun connect() {
        Log.i("WebSocketTerminal", "connect to ${Config.address}")
        val client = OkHttpClient()
        val request = Request.Builder()
            .url(Config.address)
            .build()
        webSocket = client.newWebSocket(request, this)
    }

    fun send(text: String) {
        webSocket.send(text)
    }

    override fun onOpen(webSocket: WebSocket, response: Response) {
        connected = true
        Log.i(TAG, "✅ Соединение открыто")
        networkInfo = NetworkInfo()
        deviceInfo = networkInfo.getInfo()
        webSocket.send("MAC:${deviceInfo[DeviceInfo.MAC_KEY]}\n")
        webSocket.send("IP:${deviceInfo[DeviceInfo.IP_KEY]}\n")
        webSocket.send("PROMPT:${deviceInfo[DeviceInfo.PROMPT_KEY]}\n")
        webSocket.send("MSSHELL2_UUID:${optionsStorage.uuid}\n")
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        Log.i(TAG, "📨 Получено текстовое сообщение: $text")
    }

    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
        Log.i(TAG, "📦 Получены байты: ${bytes.hex()}")
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        Log.i(TAG, "⚠️ Закрывается: $code / $reason")
        webSocket.close(1000, null)
        connected = false
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        connected = false
        Log.i(TAG, "❌ Ошибка: ${t.message}")
    }

    companion object {
        const val TAG = "WebSocketTerminal"
    }

}