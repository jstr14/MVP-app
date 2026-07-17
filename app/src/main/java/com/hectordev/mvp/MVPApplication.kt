package com.hectordev.mvp

import android.app.Application
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
        Coil.setImageLoader(
            ImageLoader.Builder(this)
                .components {
                    if (SDK_INT >= 28) add(ImageDecoderDecoder.Factory())
                    else add(GifDecoder.Factory())
                }
                .build()
        )
    }
}