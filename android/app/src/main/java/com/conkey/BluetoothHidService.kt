package com.conkey

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder

class BluetoothHidService : Service() {

    companion object {
        const val CHANNEL_ID = "conkey_bt"
        const val NOTIF_ID = 1
        const val EXTRA_STATUS = "status"

        /**
         * Updates the ongoing notification text in place. No-op if the channel
         * doesn't exist yet or the service isn't running — the system simply
         * won't show a notification that was never started, which is fine.
         */
        fun updateNotification(context: Context, status: String) {
            val nm = context.getSystemService(NotificationManager::class.java) ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                nm.getNotificationChannel(CHANNEL_ID) == null) {
                return
            }
            val pi = PendingIntent.getActivity(
                context, 0,
                context.packageManager.getLaunchIntentForPackage(context.packageName),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Notification.Builder(context, CHANNEL_ID)
            } else {
                @Suppress("DEPRECATION")
                Notification.Builder(context)
            }
            nm.notify(
                NOTIF_ID,
                builder
                    .setContentTitle("ConKey")
                    .setContentText(status)
                    .setSmallIcon(android.R.drawable.ic_menu_send)
                    .setContentIntent(pi)
                    .setOngoing(true)
                    .build()
            )
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // A null intent means the OS restarted us after a process kill. The
        // native module and BT link are gone, so don't show a false
        // "Connected" notification — just stop.
        if (intent == null) {
            stopSelf()
            return START_NOT_STICKY
        }
        val status = intent.getStringExtra(EXTRA_STATUS) ?: "Connected"
        ensureChannel()
        startForeground(NOTIF_ID, buildNotif(status))
        // START_NOT_STICKY: if we're killed, the module re-creates us on the
        // next real connection event rather than the OS reviving a stale one.
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        super.onDestroy()
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(
                        CHANNEL_ID,
                        "ConKey Keyboard",
                        NotificationManager.IMPORTANCE_LOW
                    ).apply { description = "Keeps the Bluetooth keyboard connection active" }
                )
            }
        }
    }

    private fun buildNotif(status: String): Notification {
        val pi = PendingIntent.getActivity(
            this, 0,
            packageManager.getLaunchIntentForPackage(packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
        return builder
            .setContentTitle("ConKey")
            .setContentText(status)
            .setSmallIcon(android.R.drawable.ic_menu_send)
            .setContentIntent(pi)
            .setOngoing(true)
            .build()
    }
}
