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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            val channel = NotificationChannel(
                CHANNEL_CERTIFICATES,
                getString(R.string.notification_channel_certificates),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.notification_channel_certificates_desc)
                setSound(soundUri, audioAttributes)
                enableVibration(true)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_CERTIFICATES = "mvp_certificates"
    }
}