package ru.mstrike.msshell2

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build

/**
 * Слушатель для системных сообщений на автозагрузку
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action.equals(Intent.ACTION_BOOT_COMPLETED)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.checkSelfPermission("android.permission.RECEIVE_BOOT_COMPLETED") ==
                    PackageManager.PERMISSION_GRANTED
                startApp(context)
            } else
                startApp(context)
        }
    }

    private fun startApp(context: Context) {
        val i = Intent(context, MainActivity::class.java)
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(i)
    }
}