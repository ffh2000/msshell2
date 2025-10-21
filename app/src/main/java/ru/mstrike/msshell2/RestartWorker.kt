package ru.mstrike.msshell2

import android.content.Context
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/**
 * Worker проверяет, запущено ли MainActivity.
 *
 * Если нет, то стартует его.
 *
 * Разработан в рамках борьбы за живучесть приложения.
 */
class RestartWorker constructor(
    val context: Context,
    parameters: WorkerParameters,
) : Worker(context, parameters) {

    override fun doWork(): Result {
        try {
            Log.d("ForegroundService", "RestartWorker.doWork: проверяю что бы сервис работал")
            if (!TerminalService.isRunning) {
                Log.d("ForegroundService", "RestartWorker.doWork: Сервис не работает, запускаю")
                TerminalService.start(context) //не знаю, сработает ли. Должен по идее.
//Вариант старта через MainActivity. Экран будет мигать
//                val options = ActivityOptions.makeBasic()
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) options.setLockTaskEnabled(true)
//                val packageManager = context.packageManager
//                val launchIntent = packageManager.getLaunchIntentForPackage(context.packageName)
//                if (launchIntent != null) {
//                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
//                    context.startActivity(launchIntent, options.toBundle())
//                }
            }
        } finally {
        }
        return Result.success()
    }

    companion object {

        const val WORKER_TAG = "restart_ms_shell2_worker"

        /**
         * Запускает Worker на регулярную работу.
         *
         * Если такой Worker уже есть в системе, то не стартуется.
         */
        fun start(context: Context) {
            val request = PeriodicWorkRequestBuilder<RestartWorker>(15, TimeUnit.MINUTES)
                .setInitialDelay(2, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORKER_TAG, //тэг для определения уникальности
                ExistingPeriodicWorkPolicy.KEEP, //если уже сущестует, то не создавать
                request
            )
//            if (BuildConfig.DEBUG) Log.d("RestartWorker", "Контроль работы MSVision запущен.")
        }

    }
}