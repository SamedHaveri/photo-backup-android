package com.example.photobackup.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.util.Log


class AlarmJobStarter : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val pm = context!!.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "photobackup:alarmhere")
        wl.acquire()

        if (!MediaContentJob.isScheduled(context)) {
            Log.d("Alarm", "Job not started .. starting ")
            MediaContentJob.scheduleJob(context)
        } else {
            Log.d("Alarm", "Job already running ")
        }
        wl.release()
    }

    companion object {
        fun setAlarm(context: Context) {
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, AlarmJobStarter::class.java)
            val pi = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
            am.setRepeating(AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis(),
                (1000 * 60 * 10).toLong(),
                pi) // Millisec * Second * Minute
        }

        fun cancelAlarm(context: Context) {
            val intent = Intent(context, AlarmJobStarter::class.java)
            val sender =
                PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(sender)
            sender.cancel()
        }

        fun isAlarmRunning(context: Context): Boolean {
            val intent = Intent(context.applicationContext, AlarmJobStarter::class.java)
            val isBackupServiceAlarmSet: Boolean
            isBackupServiceAlarmSet = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.getBroadcast(context.applicationContext,
                    0,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE)
                PendingIntent.getBroadcast(context.applicationContext,
                    0,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE) != null
            } else {
                PendingIntent.getBroadcast(context.applicationContext,
                    0,
                    intent,
                    PendingIntent.FLAG_NO_CREATE)
                PendingIntent.getBroadcast(context.applicationContext,
                    0,
                    intent,
                    PendingIntent.FLAG_NO_CREATE) != null
            }
            Log.d("Alarm Job Starter",
                "Alarm is " + (if (isBackupServiceAlarmSet) "" else "not ") + "set already")
            return isBackupServiceAlarmSet
        }
    }
}