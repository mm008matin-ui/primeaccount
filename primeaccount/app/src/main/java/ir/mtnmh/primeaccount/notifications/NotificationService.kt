package ir.mtnmh.primeaccount.notifications

import android.app.NotificationChannel
import android.app.NotificationManager as AndroidNotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import ir.mtnmh.primeaccount.MainActivity
import ir.mtnmh.primeaccount.R
import ir.mtnmh.primeaccount.core.firebase.NotificationManager

class NotificationService : FirebaseMessagingService() {
    private val TAG = "NotificationService"
    
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Federated FCM Refresh Token: $token")
        NotificationManager.saveFcmToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d(TAG, "Incoming push message body: ${message.notification?.body}")
        
        val title = message.notification?.title ?: message.data["title"] ?: "PrimeAccount"
        val body = message.notification?.body ?: message.data["body"] ?: "New activity updated"
        val type = message.data["type"] ?: "SYSTEM"

        // Store standard non-chat notifications inside the live database
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            NotificationManager.postNotification(
                userId = currentUser.uid,
                title = title,
                message = body,
                type = type
            )
        }

        sendNotification(title, body)
    }

    private fun sendNotification(title: String, messageBody: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "prime_notifications_channel"
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as AndroidNotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "PrimeAccount Channel",
                AndroidNotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }
}

