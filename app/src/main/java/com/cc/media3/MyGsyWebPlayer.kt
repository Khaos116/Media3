package com.cc.media3

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.ImageView
import androidx.core.view.isVisible
import com.shuyu.gsyvideoplayer.video.base.GSYBaseVideoPlayer
import kotlin.math.abs

/**
 * Author:XX
 * Date:2024/12/23
 * Time:22:02
 */
class MyGsyWebPlayer : MyGsyPlayer {
  //<editor-fold defaultstate="collapsed" desc="多构造">
  constructor(c: Context) : super(c)
  constructor(c: Context, b: Boolean) : super(c, b)
  constructor(c: Context, a: AttributeSet?) : super(c, a)
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="重写的XML(需要保留原来的ID)">
  override fun getLayoutId() = R.layout.layout_my_gsy_web
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="新增控件">
  private var mWebView: WebView? = null
  private var mRefresh: ImageView? = null
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="初始化">
  @SuppressLint("SetJavaScriptEnabled")
  override fun init(context: Context?) {
    super.init(context)
    (mStartButton as? ImageView)?.setImageResource(0)
    mRefresh = findViewById(R.id.ivReStart)
    mRefresh?.setOnClickListener { mWebView?.reload() }
    mWebView = findViewById(R.id.webView)
    mWebView?.settings?.let { ws ->
      ws.javaScriptCanOpenWindowsAutomatically = true //兼容弹窗
      //字体大小不跟随系统
      ws.textZoom = 100
      //支持javascript
      ws.javaScriptEnabled = true
      //设置可以支持缩放
      ws.setSupportZoom(false)
      //设置内置的缩放控件
      ws.builtInZoomControls = false
      //隐藏原生的缩放控件
      ws.displayZoomControls = true
      //扩大比例的缩放
      ws.useWideViewPort = true
      //允许WebView使用File协议
      ws.allowFileAccess = true
      //自适应屏幕
      ws.layoutAlgorithm = WebSettings.LayoutAlgorithm.SINGLE_COLUMN
      ws.loadWithOverviewMode = true
      ws.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
      ws.userAgentString = WebSettings.getDefaultUserAgent(context)
    }
  }
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="全屏按钮">
  override fun getEnlargeImageRes() = R.drawable.svg_player_live_full_enter //全屏按钮
  override fun getShrinkImageRes() = R.drawable.svg_player_live_full_exit //退出全屏按钮
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="全屏处理">
  override fun dealEnterFullPlayer(player: GSYBaseVideoPlayer) { //进入全屏
    super.dealEnterFullPlayer(player)
    (player as? MyGsyWebPlayer)?.let { p ->
      mWebView?.onPause()
      p.startPlay(mWebView?.url ?: "", mTitleTextView?.text?.toString() ?: "")
    }
  }

  override fun clearFullscreenLayout() { //退出全屏
    super.clearFullscreenLayout()
    (mFullPlayer as? MyGsyWebPlayer)?.let { p ->
      p.mWebView?.destroy()
      this.mWebView?.onResume()
    }
  }
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="外部调用播放">
  fun startPlay(h5Url: String, title: String) {
    "WEB播放地址:$h5Url".logE()
    this.titleTextView?.text = title
    this.mWebView?.loadUrl(h5Url)
  }
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="生命周期">
  override fun onVideoResume() {
    super.onVideoResume()
    mWebView?.onResume()
  }

  override fun onVideoPause() {
    super.onVideoPause()
    mWebView?.onPause()
  }

  override fun release() {
    super.release()
    mWebView?.destroy()
  }
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="控件显示和隐藏">
  override fun setViewShowState(view: View?, visibility: Int) {
    when (view) {
      mTopContainer,
      mBottomContainer -> super.setViewShowState(view, visibility)

      else -> super.setViewShowState(view, View.INVISIBLE)
    }
  }
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="处理开始和暂停图标">
  override fun updateStartImage() {
    (mStartButton as? ImageView)?.setImageResource(0)
  }
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="拦截按压">
  private var mDownTime = 0L
  private var mDownTempX = 0f
  private var mDownTempY = 0f
  override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
    when (ev?.action) {
      MotionEvent.ACTION_DOWN -> {
        mDownTime = System.currentTimeMillis()
        mDownTempX = ev.x
        mDownTempY = ev.y
      }

      MotionEvent.ACTION_UP,
      MotionEvent.ACTION_CANCEL -> {
        if (System.currentTimeMillis() - mDownTime <= 200 && abs(ev.x - mDownTempX) <= 20 && abs(ev.y - mDownTempY) <= 20) {
          if (mBottomContainer?.isVisible == true) {
            cancelDismissControlViewTimer()
            changeUiToClear()
          } else {
            changeUiToPlayingShow()
            startDismissControlViewTimer()
          }
        }
        mDownTime = 0L
        mDownTempX = 0f
        mDownTempY = 0f
      }
    }
    return super.dispatchTouchEvent(ev)
  }
  //</editor-fold>
}