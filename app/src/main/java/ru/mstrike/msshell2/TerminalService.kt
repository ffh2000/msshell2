package ru.mstrike.msshell2

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class TerminalService : Service() {

    var serviceScope: Job? = null
    var canceled = false

    lateinit var webSocketTerminal: WebSocketTerminal

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground()
        serviceScope = GlobalScope.launch {
            webSocketTerminal =
                WebSocketTerminal((applicationContext as Application).optionsStorage)
            while (!canceled) {
                if (!webSocketTerminal.connected) {
                    webSocketTerminal.connect()
                    execute()
                }
                delay(10000)
            }
        }
        return START_STICKY
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    fun execute() {

    }

    private fun startForeground() {
//        val dataPermission =
//            PermissionChecker.checkSelfPermission(this, android.p.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "notify",
                    NotificationManager.IMPORTANCE_DEFAULT
                )
                val notificationManager =
                    applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
            }
            val notification = NotificationCompat
                .Builder(this, CHANNEL_ID)
                .build()
            ServiceCompat.startForeground(
                this,
                100,
                notification,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                else 0,
            )
        } catch (th: Throwable) {
            Log.e("ForegroundService", "${th.message}")
        }
    }

    companion object {
        const val CHANNEL_ID = "Ms adminer 2"

        @JvmStatic
        fun start(context: Context) {
            val intent = Intent(context, TerminalService::class.java)
            context.startForegroundService(intent)
        }
    }
}