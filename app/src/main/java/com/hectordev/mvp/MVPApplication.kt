package com.hectordev.mvp

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import coil.Coil
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import com.giphy.sdk.ui.Giphy
import dagger.hilt.android.HiltAndroidApp
import androidx.core.net.toUri

@HiltAndroidApp
class MVPApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Giphy.configure(this, BuildConfig.GIPHY_API_KEY)
        createNotificationChannels()
        Coil.setImageLoader(
            ImageLoader.Builder(this)
                .components {
                    if (SDK_INT >= 28) add(ImageDecoderDecoder.Factory())
                    else add(GifDecoder.Factory())
                }
                .build()
        )
    }

    private fun createNotificationChannels() {
        if (SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val certChannel = NotificationChannel(
                CHANNEL_CERTIFICATES,
                getString(R.string.notification_channel_certificates),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.notification_channel_certificates_desc)
                setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), audioAttributes)
                enableVibration(true)
            }

            val alarmAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            val nuclearAlarmUri = "android.resource://$packageName/${R.raw.nuclear_alarm}".toUri()
            val emergencyChannel = NotificationChannel(
                CHANNEL_EMERGENCY,
                getString(R.string.notification_channel_emergency),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.notification_channel_emergency_desc)
                setSound(nuclearAlarmUri, alarmAttributes)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 400, 200, 400, 200, 400)
            }

            notificationManager.createNotificationChannel(certChannel)
            notificationManager.createNotificationChannel(emergencyChannel)
        }
    }

    companion object {
        const val CHANNEL_CERTIFICATES = "mvp_certificates"
        const val CHANNEL_EMERGENCY = "mvp_emergency"
        const val NOTIFICATION_ID_EMERGENCY = 1001
    }
}