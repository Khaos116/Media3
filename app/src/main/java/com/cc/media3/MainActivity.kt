package com.cc.media3

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.fragment.app.FragmentActivity
import com.cc.media3.databinding.AcMainBinding
import com.shuyu.gsyvideoplayer.GSYVideoManager
import com.shuyu.gsyvideoplayer.cache.CacheFactory
import com.shuyu.gsyvideoplayer.player.PlayerFactory
import com.shuyu.gsyvideoplayer.utils.OrientationUtils
import com.shuyu.gsyvideoplayer.video.StandardGSYVideoPlayer
import tv.danmaku.ijk.media.exo2.Exo2PlayerManager
import tv.danmaku.ijk.media.exo2.ExoPlayerCacheManager
import kotlin.system.exitProcess

class MainActivity : FragmentActivity() {
  //<editor-fold defaultstate="collapsed" desc="变量">
  private lateinit var binding: AcMainBinding
  private var mVideoPlayer: StandardGSYVideoPlayer? = null
  private var mOrientationUtils: OrientationUtils? = null
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="onCreate">
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = AcMainBinding.inflate(layoutInflater)
    setContentView(binding.root)
    //EXOPlayer内核，支持格式更多
    PlayerFactory.setPlayManager(Exo2PlayerManager::class.java)
    CacheFactory.setCacheManager(ExoPlayerCacheManager::class.java)
    init()
  }
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="初始化">
  private val urls = mutableListOf(
    "问题视频推流" to "https://us2.linkvlc.shop/GO2?token=RS13",
    //https://blog.csdn.net/weixin_45728126/article/details/12881309
    "RTMP推流" to "rtmp://ns8.indexforce.com/home/mystream",
    //https://github.com/mpromonet/webrtc-streamer/blob/master/config.json
    "RTSP推流" to "rtsp://211.132.61.124/axis-media/media.amp",
    //https://github.com/movin2008/ShuiyeVideo/blob/master/app/src/main/assets/tvlive/%E7%9B%B4%E6%92%AD%E4%B8%AD%E5%9B%BD.tv
    "M3U8推流" to "http://gctxyc.liveplay.myqcloud.com/gc/wgw01_1/index.m3u8",
    //https://github.com/movin2008/ShuiyeVideo/blob/f7dc0ec72f5dfb7cd66e53594033fff65c68bf98/app/src/main/assets/tvlive/%E6%B8%AF%E6%BE%B3%E5%8F%B0.tv#L4
    "FLV推流" to "http://zhibo.hkstv.tv/livestream/mutfysrq.flv",
    //https://yingshi.tv/  [插件:Video DownloadHelper]
    "普通M3U8视频" to "https://m3u8.hmrvideo.com/play/f09a0503f1cf473c9134f54b0379f8ca.m3u8",
    //https://www.6huo.com/hd [https://www.yugaopian.cn/]
    "普通MP4视频" to "https://vod.pipi.cn/fec9203cvodtransbj1251246104/4d6a5a571397757897006080669/v.f42905.mp4",
  )

  private fun init() {
    mVideoPlayer = binding.videoPlayer
    mVideoPlayer?.isNeedShowWifiTip = false
    val p = urls[(Math.random() * urls.size).toInt()]
    mVideoPlayer?.let { videoPlayer ->
      videoPlayer.setUp(p.second, p.first.startsWith("普通"), p.first)
      //增加封面
      val imageView = ImageView(this)
      imageView.scaleType = ImageView.ScaleType.CENTER_CROP
      imageView.setImageResource(R.mipmap.ic_launcher)
      videoPlayer.thumbImageView = imageView
      //增加title
      videoPlayer.titleTextView.visibility = View.VISIBLE
      //设置返回键
      videoPlayer.backButton.visibility = View.VISIBLE
      //设置旋转
      mOrientationUtils = OrientationUtils(this, videoPlayer)
      //设置全屏按键功能,这是使用的是旋转屏幕，而不是全屏
      videoPlayer.fullscreenButton.setOnClickListener { mOrientationUtils?.resolveByClick() }
      //是否可以滑动调整
      videoPlayer.setIsTouchWiget(true)
      //设置返回按键功能
      videoPlayer.backButton.setOnClickListener { onBackPressed() }
      ///不需要屏幕旋转
      videoPlayer.isNeedOrientationUtils = false
      videoPlayer.startPlayLogic()
    }
  }
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="生命周期">
  override fun onPause() {
    super.onPause()
    mVideoPlayer?.onVideoPause()
  }

  override fun onResume() {
    super.onResume()
    mVideoPlayer?.onVideoResume()
  }

  override fun onDestroy() {
    super.onDestroy()
    GSYVideoManager.releaseAllVideos()
    mOrientationUtils?.releaseListener()
  }

  @Suppress("OVERRIDE_DEPRECATION")
  override fun onBackPressed() {
    if (mOrientationUtils?.screenType == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
      mVideoPlayer?.fullscreenButton?.performClick()
      return
    }
    //释放所有
    mVideoPlayer?.setVideoAllCallBack(null)
    finish()
    exitProcess(0)
  }
  //</editor-fold>
}