package com.cc.media3

import android.annotation.SuppressLint
import android.content.res.Resources
import android.graphics.Point
import android.os.Bundle
import android.util.Base64
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.FragmentActivity
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.datasource.*
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.drm.*
import androidx.media3.exoplayer.source.MediaSource
import com.cc.media3.databinding.AcMainBinding
import com.shuyu.gsyvideoplayer.cache.CacheFactory
import com.shuyu.gsyvideoplayer.listener.GSYSampleCallBack
import com.shuyu.gsyvideoplayer.player.PlayerFactory
import com.shuyu.gsyvideoplayer.utils.GSYVideoType
import tv.danmaku.ijk.media.exo2.*
import java.io.File

//https://www.skylinewebcams.com/zh/webcam/vietnam.html
class MainActivity : FragmentActivity() {
  //<editor-fold defaultstate="collapsed" desc="变量">
  private lateinit var binding: AcMainBinding
  private var mVideoPlayer: MyGsyPlayer? = null

  //https://github.com/Mgsportstv/mgsportstv/blob/e5fefa8/Player/mpdmgfoot.html
  //https://github.com/Streaming2024/TV
  private val mDrmUrlInfo = mutableListOf(
    Triple("【DRM-CLEARKEY】SSC1", "https://ssc-1-enc.edgenextcdn.net/out/v1/c696e4819b55414388a1a487e8a45ca1/index.mpd", "d84c325f36814f39bbe59080272b10c3:550727de4c96ef1ecff874905493580f"),
    //Triple("【DRM-CLEARKEY】CRIC BUZZ", "https://live2.shoq.com.pk/live/eds/Criclife2/DASH/Criclife2.mpd", "4301796d6d67374043c4a43c18dff7ea:96a3dc8766317aa169149a604928ebb6"),
    Triple("【DRM-WIDEVINE】License Header", "https://media.axprod.net/TestVectors/v7-MultiDRM-SingleKey/Manifest_1080p.mpd", "https://drm-widevine-licensing.axtest.net/AcquireLicense"),
  )
  private val mHeaders = mutableListOf(
    Pair(
      "https://media.axprod.net/TestVectors/v7-MultiDRM-SingleKey/Manifest_1080p.mpd",
      hashMapOf(
        "X-AxDRM-Message" to "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ2ZXJzaW9uIjoxLCJjb21fa2V5X2lkIjoiYjMzNjRlYjUtNTFmNi00YWUzLThjOTgtMzNjZWQ1ZTMxYzc4IiwibWVzc2FnZSI6eyJ0eXBlIjoiZW50aXRsZW1lbnRfbWVzc2FnZSIsImtleXMiOlt7ImlkIjoiOWViNDA1MGQtZTQ0Yi00ODAyLTkzMmUtMjdkNzUwODNlMjY2IiwiZW5jcnlwdGVkX2tleSI6ImxLM09qSExZVzI0Y3Iya3RSNzRmbnc9PSJ9XX19.4lWwW46k-oWcah8oN18LPj5OLS5ZU-_AQv7fe0JhNjA"
      )
    )
  )
  private val mUrls = mutableListOf(
    "问题视频推流" to "https://us2.linkvlc.shop/GO2?token=RS13",
    "RTMP推流" to "rtmp://f13h.mine.nu/sat/tv071", //台视 https://github.com/suxuang/myIPTV/blob/main/ipv6.m3u
    "RTSP推流" to "rtsp://211.132.61.124/axis-media/media.amp", //日本千叶县旭市 https://github.com/mpromonet/webrtc-streamer/blob/master/config.json
    "HLS(M3U8)推流" to "http://aktv.top/AKTV/live/aktv/null-3/AKTV.m3u8", //鳳凰中文 https://github.com/tianya7981/tvbox-/blob/main/20241028
    "FLV推流" to "http://ali.hlspull.yximgs.com/live/awei_cwjd.flv", //重温经典 https://github.com/vbskycn/iptv/blob/master/tv/hd.txt
    "MPD推流" to "https://d25tgymtnqzu8s.cloudfront.net/smil:sukan/manifest.mpd", //SUKAN https://github.com/zzq12345/jiemuyuan/blob/67ec6cb/Astro.m3u
    "普通M3U8视频" to "https://m3u8.hmrvideo.com/play/f09a0503f1cf473c9134f54b0379f8ca.m3u8", //https://yingshi.tv/  [插件:Video DownloadHelper]
    "普通MP4视频" to "https://vod.pipi.cn/fec9203cvodtransbj1251246104/4d6a5a571397757897006080669/v.f42905.mp4", //https://www.6huo.com/hd  [https://www.yugaopian.cn/]
  ).apply {
    addAll(mDrmUrlInfo.map { a -> Pair(a.first, a.second) })
  }
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="onCreate">
  @SuppressLint("UnsafeOptInUsageError")
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = AcMainBinding.inflate(layoutInflater)
    setContentView(binding.root)
    //EXOPlayer内核，支持格式更多
    PlayerFactory.setPlayManager(Exo2PlayerManager::class.java)
    CacheFactory.setCacheManager(ExoPlayerCacheManager::class.java)
    ExoSourceManager.setExoMediaSourceInterceptListener(object : ExoMediaSourceInterceptListener {
      override fun getMediaSource(dataSource: String?, preview: Boolean, cacheEnable: Boolean, isLooping: Boolean, cacheDir: File?): MediaSource? {
        return if (dataSource?.endsWith(".mpd") == true) dealDrmVideo(dataSource) else null
      }

      override fun getHttpDataSourceFactory(userAgent: String?, listener: TransferListener?, connectTimeoutMillis: Int, readTimeoutMillis: Int, mapHeadData: MutableMap<String, String>?, allowCrossProtocolRedirects: Boolean): DataSource.Factory? {
        return null
      }

      override fun cacheWriteDataSinkFactory(CachePath: String?, url: String?): DataSink.Factory? {
        return null
      }
    })
    GSYVideoType.setShowType(GSYVideoType.SCREEN_TYPE_DEFAULT) //默认显示比例
    //GSYVideoType.setShowType(GSYVideoType.SCREEN_TYPE_FULL) //全屏裁减显示，为了显示正常
    GSYVideoType.setRenderType(GSYVideoType.SUFRACE) //SurfaceView，动画切换等时候效果比较差
    //GSYVideoType.setRenderType(GSYVideoType.GLSURFACE)
    //GSYVideoType.setRenderType(GSYVideoType.TEXTURE)//发现https://media.axprod.net/TestVectors/v7-MultiDRM-SingleKey/Manifest_1080p.mpd无画面
    init()
  }
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="初始化">
  private fun init() {
    binding.tvEnter.setOnClickListener {
      mVideoPlayer?.let { p ->
        if (binding.tvEnter.text.toString() == "进入小窗模式") {
          binding.tvEnter.text = "退出小窗模式"
          p.showSmallVideo(Point((p.width * 0.6).toInt(), (p.height * 0.6).toInt()), false, false)
        } else {
          binding.tvEnter.text = "进入小窗模式"
          p.hideSmallVideo()
          p.onVideoResume()
        }
      }
    }
    binding.rlVideoView.layoutParams.height = (9f / 16 * Resources.getSystem().displayMetrics.widthPixels).toInt()
    mVideoPlayer = when ((Math.random() * 100 + 1).toInt() % 3) {
      1 -> MyGsyLivePlayer(this)
      2 -> MyGsyVideoPlayer(this)
      else -> MyGsyWebPlayer(this)
    }
    mVideoPlayer?.setVideoAllCallBack(object : GSYSampleCallBack() {
      override fun onEnterSmallWidget(url: String?, vararg objects: Any?) {
        super.onEnterSmallWidget(url, *objects)
        binding.tvEnter.text = "退出小窗模式"
      }

      override fun onQuitSmallWidget(url: String?, vararg objects: Any?) {
        super.onQuitSmallWidget(url, *objects)
        binding.tvEnter.text = "进入小窗模式"
      }
    })
    binding.rlVideoView.addView(mVideoPlayer, ViewGroup.LayoutParams(-1, -1))
    mVideoPlayer?.isNeedShowWifiTip = false
    val p = mUrls[(Math.random() * mUrls.size).toInt()]
    mVideoPlayer?.let { videoPlayer ->
      //增加封面
      videoPlayer.thumbImageView = ImageView(this).apply {
        scaleType = ImageView.ScaleType.CENTER_CROP
        setImageResource(R.mipmap.ic_launcher)
      }
      (videoPlayer as? MyGsyVideoPlayer)?.let { player ->
        val b = mUrls[6]
        player.setUp(b.second, b.first.startsWith("普通"), b.first)
        mHeaders.firstOrNull { f -> f.first == b.second }?.second?.let { header -> videoPlayer.mapHeadData = header }
        player.startPlayLogic()
      }
      (videoPlayer as? MyGsyLivePlayer)?.let { player ->
        player.setLinesAndHeader(mUrls.map { a -> a.second }.take(6).toMutableList(), "测试线路切换", hashMapOf())
        player.playByLineIndex(0)
      }
      (videoPlayer as? MyGsyWebPlayer)?.let { player ->
        player.startPlay("file:///android_asset/test_video_play.html", "测试H5播放视频")
      }
    }
  }
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="生命周期">

  @Suppress("OVERRIDE_DEPRECATION")
  override fun onBackPressed() {
    if (mVideoPlayer?.onBackPressed() == true) return
    super.onBackPressed()
    //finish()
    //exitProcess(0)
  }
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="处理Dash(DRM)直播流">
  @SuppressLint("UnsafeOptInUsageError")
  private fun dealDrmVideo(url: String): MediaSource? {
    mDrmUrlInfo.firstOrNull { t -> t.second == url }?.let { t ->
      val licenseKey = t.third
      if (licenseKey.startsWith("http")) {
        val playerItem = MediaItem.Builder()
          .setUri(url)
          .setDrmConfiguration(
            MediaItem.DrmConfiguration.Builder(C.WIDEVINE_UUID)
              .setLicenseUri(licenseKey)
              .setMultiSession(true)
              .setLicenseRequestHeaders(mHeaders.firstOrNull { f -> f.first == url }?.second ?: emptyMap())
              .build()
          )
          .build()
        return DashMediaSource.Factory(DefaultHttpDataSource.Factory()).createMediaSource(playerItem)
      } else if (licenseKey.split(":").size == 2) {
        val licenseParts = licenseKey.split(":")
        val drmKeyId = licenseParts[0]
        val drmKey = licenseParts[1]
        val drmKeyBytes = drmKey.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        val encodedDrmKey = Base64.encodeToString(drmKeyBytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
        val drmKeyIdBytes = drmKeyId.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        val encodedDrmKeyId = Base64.encodeToString(drmKeyIdBytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
        val drmBody = "{\"keys\":[{\"kty\":\"oct\",\"k\":\"${encodedDrmKey}\",\"kid\":\"${encodedDrmKeyId}\"}],\"type\":\"temporary\"}"
        return DashMediaSource.Factory(DefaultHttpDataSource.Factory())
          .setDrmSessionManagerProvider {
            DefaultDrmSessionManager.Builder()
              .setPlayClearSamplesWithoutKeys(true)
              .setMultiSession(true)
              .setKeyRequestParameters(mHeaders.firstOrNull { f -> f.first == url }?.second ?: emptyMap())
              .setUuidAndExoMediaDrmProvider(C.CLEARKEY_UUID, FrameworkMediaDrm.DEFAULT_PROVIDER)
              .build(LocalMediaDrmCallback(drmBody.toByteArray()))
          }
          .createMediaSource(MediaItem.fromUri(url))
      }
    }
    return null
  }
  //</editor-fold>
}