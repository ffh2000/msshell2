package ru.mstrike.msshell2.grpc

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import com.google.protobuf.ByteString
import com.google.protobuf.timestamp
import device.DeviceServiceGrpc
import device_v1.DeviceProto.*
import device_v1.DeviceServiceGrpcKt
import io.grpc.ManagedChannel
import kotlinx.coroutines.flow.Flow
import org.json.JSONObject
import timber.log.Timber
import java.net.InetAddress
import java.net.NetworkInterface

class DeviceRepository(
    private val context: Context,
    private val deviceId: String
) {
    private lateinit var stub: DeviceServiceGrpc.DeviceServiceCoroutineStub
    private lateinit var stream: io.grpc.stub.ClientCalls.ClientBidiStreamingCall<DeviceMessage, DeviceMessage>

    fun initialize(channel: ManagedChannel) {
        stub = DeviceServiceGrpcKt.DeviceServiceCoroutineStub(channel)
    }

    suspend fun startStream(): Flow<DeviceMessage> {
        stream = stub.deviceStream()
        return stream.inbound
    }

    suspend fun sendDeviceInfo() {
        val deviceInfo = buildDeviceInfo()
        val macAddress = getMacAddress()

        val message = DeviceMessage.newBuilder()
            .setMessageId("info-${System.nanoTime()}")
            .setDeviceId(deviceId)
            .setMacAddress(macAddress)  // REQUIRED: MAC as primary identifier
            .setType(MessageType.DEVICE_INFO)
            .setData(ByteString.copyFromUtf8(deviceInfo))
            .setTimestamp(timestamp { seconds = System.currentTimeMillis() / 1000 })
            .build()

        stream.send(message)
        Timber.d("Sent device info with MAC: $macAddress")
    }

    suspend fun sendPing() {
        val message = DeviceMessage.newBuilder()
            .setMessageId("ping-${System.nanoTime()}")
            .setDeviceId(deviceId)
            .setType(MessageType.PING)
            .setTimestamp(timestamp { seconds = System.currentTimeMillis() / 1000 })
            .build()

        stream.send(message)
    }

    suspend fun sendShellOutput(sessionId: String, data: ByteArray) {
        val outputData = ShellOutputData.newBuilder()
            .setSessionId(sessionId)
            .setData(ByteString.copyFrom(data))
            .build()

        val message = DeviceMessage.newBuilder()
            .setMessageId("shell-out-$sessionId-${System.nanoTime()}")
            .setDeviceId(deviceId)
            .setSessionId(sessionId)
            .setType(MessageType.SHELL_OUTPUT)
            .setData(outputData.toByteString())
            .setTimestamp(timestamp { seconds = System.currentTimeMillis() / 1000 })
            .build()

        stream.send(message)
    }

    suspend fun sendShellExit(sessionId: String, exitCode: Int) {
        val exitData = ShellExitData.newBuilder()
            .setSessionId(sessionId)
            .setExitCode(exitCode)
            .build()

        val message = DeviceMessage.newBuilder()
            .setMessageId("shell-exit-$sessionId-${System.nanoTime()}")
            .setDeviceId(deviceId)
            .setSessionId(sessionId)
            .setType(MessageType.SHELL_EXIT)
            .setData(exitData.toByteString())
            .setTimestamp(timestamp { seconds = System.currentTimeMillis() / 1000 })
            .build()

        stream.send(message)
    }

    suspend fun sendCommandResult(commandId: Int, command: String, result: String) {
        val resultData = JSONObject().apply {
            put("command", command)
            put("result", result)
            put("error", "")
            put("command_id", commandId)
        }.toString()

        val message = DeviceMessage.newBuilder()
            .setMessageId("result-${System.nanoTime()}")
            .setDeviceId(deviceId)
            .setType(MessageType.COMMAND_RESULT)
            .setData(ByteString.copyFromUtf8(resultData))
            .setTimestamp(timestamp { seconds = System.currentTimeMillis() / 1000 })
            .build()

        stream.send(message)
    }

    private fun buildDeviceInfo(): String {
        return JSONObject().apply {
            put("device_info", JSONObject().apply {
                put("device_id", deviceId)
                put("platform", "android")
                put("model", Build.MODEL)
                put("version", Build.VERSION.RELEASE)
                put("manufacturer", Build.MANUFACTURER)
                put("ip_address", getIpAddress())
                put("mac_address", getMacAddress())  // Hardware-based unique identifier
            })
            put("app_info", JSONObject().apply {
                put("app_name", context.packageName)
                put("package_name", context.packageName)
                put("version", getAppVersion())
                put("build_number", getBuildNumber())
            })
            put("connectivity", "grpc")
        }.toString()
    }

    private fun getMacAddress(): String {
        try {
            // Attempt 1: WiFi Manager (Android 6-9)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                val wifiInfo = wifiManager?.connectionInfo
                val macAddr = wifiInfo?.macAddress
                if (!macAddr.isNullOrEmpty() && macAddr != "02:00:00:00:00:00") {
                    return macAddr
                }
            }

            // Attempt 2: Network interfaces (works on some devices)
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                // Look for WiFi or Ethernet interfaces
                if (networkInterface.name.matches(Regex("wlan0|eth0"))) {
                    val mac = networkInterface.hardwareAddress
                    if (mac != null && mac.isNotEmpty()) {
                        return mac.joinToString(":") { "%02x".format(it) }
                    }
                }
            }

            // Fallback: Use Android ID as unique identifier
            val androidId = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ANDROID_ID
            )
            if (!androidId.isNullOrEmpty()) {
                // Convert Android ID to MAC-like format for consistency
                return "02:" + androidId.take(10).chunked(2).joinToString(":")
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to get MAC address")
        }

        // Final fallback: generate device-specific identifier
        val fallbackId = "${Build.MANUFACTURER}-${Build.MODEL}-${Build.SERIAL}".hashCode()
        return "02:00:" + "%08x".format(fallbackId).chunked(2).joinToString(":")
    }

    private fun getIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address is InetAddress) {
                        val hostAddress = address.hostAddress
                        if (hostAddress != null && !hostAddress.contains(":")) {
                            return hostAddress
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to get IP address")
        }
        return "unknown"
    }

    private fun getAppVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }

    private fun getBuildNumber(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode.toString()
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toString()
            }
        } catch (e: Exception) {
            "1"
        }
    }
}
