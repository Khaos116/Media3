package com.cc.media3

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.fragment.app.FragmentActivity
import com.cc.media3.databinding.AcMainBinding
import com.shuyu.gsyvideoplayer.GSYVideoManager
import com.shuyu.gsyvideoplayer.player.PlayerFactory
import com.shuyu.gsyvideoplayer.utils.OrientationUtils
import com.shuyu.gsyvideoplayer.video.StandardGSYVideoPlayer
import tv.danmaku.ijk.media.exo2.Exo2PlayerManager
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
    init()
  }
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="初始化">
  private val urls = mutableListOf(
    "问题视频推流" to "https://us2.linkvlc.shop/GO2?token=RS13",
    "RTMP推流" to "rtmp://liteavapp.qcloud.com/live/liteavdemoplayerstreamid",
    "RTSP推流" to "rtsp://stream.strba.sk:1935/strba/VYHLAD_JAZERO.stream",
    "M3U8推流" to "http://220.161.87.62:8800/hls/0/index.m3u8",
    "普通M3U8视频" to "http://devimages.apple.com.edgekey.net/streaming/examples/bipbop_4x3/gear3/prog_index.m3u8",
  )

  private fun init() {
    mVideoPlayer = binding.videoPlayer
    mVideoPlayer?.isNeedShowWifiTip = false
    val p = urls[(Math.random() * urls.size).toInt()]
    mVideoPlayer?.let { videoPlayer ->
      videoPlayer.setUp(p.second, true, p.first)
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