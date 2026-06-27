package com.cc.media3

import android.annotation.SuppressLint
import android.content.Context
import androidx.media3.exoplayer.DefaultRenderersFactory
import tv.danmaku.ijk.media.exo2.Exo2PlayerManager
import tv.danmaku.ijk.media.exo2.IjkExo2MediaPlayer

/**
 * Custom ExoPlayer manager to enable decoder fallback.
 * This fixes "Error 0x80001019" (Insufficient Resources) by allowing ExoPlayer 
 * to use software decoders when hardware ones are unavailable.
 */
class MyExoPlayerManager : Exo2PlayerManager() {
    @SuppressLint("UnsafeOptInUsageError")
    override fun initVideoPlayer(context: Context, msg: android.os.Message, optionModelList: MutableList<com.shuyu.gsyvideoplayer.model.VideoOptionModel>?, cacheManager: com.shuyu.gsyvideoplayer.cache.ICacheManager?) {
        super.initVideoPlayer(context, msg, optionModelList, cacheManager)
        // Use the public getter since the field is private in the Java base class
        (mediaPlayer as? IjkExo2MediaPlayer)?.let { ijkPlayer ->
            val renderersFactory = DefaultRenderersFactory(context)
            // Enable decoder fallback to handle hardware resource exhaustion (Error 0x80001019)
            renderersFactory.setEnableDecoderFallback(true)
            // Set extension mode to PREFER to match GSY's default behavior
            renderersFactory.setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
            ijkPlayer.rendererFactory = renderersFactory
        }
    }
}