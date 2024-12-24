package com.cc.media3

import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.*
import com.shuyu.gsyvideoplayer.video.StandardGSYVideoPlayer
import com.shuyu.gsyvideoplayer.video.base.GSYBaseVideoPlayer
import com.shuyu.gsyvideoplayer.video.base.GSYVideoPlayer
import java.io.File

/**
 * Author:XX
 * Date:2024/12/20
 * Time:21:44
 */
@Suppress("DEPRECATION", "MemberVisibilityCanBePrivate")
open class MyGsyPlayer : StandardGSYVideoPlayer, LifecycleEventObserver {
  //<editor-fold defaultstate="collapsed" desc="多构造">
  constructor(c: Context) : super(c)
  constructor(c: Context, a: AttributeSet?) : super(c, a)
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="初始化">
  protected var mFullPlayer: GSYBaseVideoPlayer? = null
  override fun init(context: Context?) {
    super.init(context)
    this.isNeedShowWifiTip = false
    (getContext() as? Activity)?.let { ac ->
      this.backButton?.setOnClickListener { ac.onBackPressed() } //返回
      this.fullscreenButton?.setOnClickListener { //全屏按钮
        this.mShowFullAnimation = false //有动画会有延迟，导致mOrientationUtils没有初始化
        val player = super.startWindowFullscreen(ac, true, true) //通过UI进行全屏播放
        mFullPlayer = player
        dealFullPlayer(player)
        mOrientationUtils?.resolveByClick() //横竖屏切换
      }
    }
  }
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="进入全屏">
  open fun dealFullPlayer(player: GSYBaseVideoPlayer) {
  }
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="Lifecycle生命周期">
  private var mLifecycle: Lifecycle? = null
  private var mNeedResumePlay = false

  //通过Lifecycle内部自动管理暂停和播放(如果不需要后台播放)
  private fun setLifecycleOwner(owner: LifecycleOwner?) {
    if (owner == null) {
      mLifecycle?.removeObserver(this)
      mLifecycle = null
    } else {
      mLifecycle?.removeObserver(this)
      mLifecycle = owner.lifecycle
      mLifecycle?.addObserver(this)
    }
  }

  override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
    when (event) {
      Lifecycle.Event.ON_RESUME -> {
        if (mNeedResumePlay) {
          mNeedResumePlay = false
          this.onVideoResume()
          "恢复播放".logE()
        }
      }

      Lifecycle.Event.ON_PAUSE -> {
        mNeedResumePlay = this.isInPlayingState
        this.onVideoPause()
        "暂停播放".logE()
      }

      Lifecycle.Event.ON_DESTROY -> {
        this.setVideoAllCallBack(null)
        this.release()
        mOrientationUtils?.releaseListener()
        "释放播放资源".logE()
      }

      else -> {}
    }
  }
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="自感应生命周期">
  private var isNeedRecoverPlay = false

  override fun onAttachedToWindow() { //全屏的时候会重新克隆一份播放器到Window.ID_ANDROID_CONTENT控件里面
    super.onAttachedToWindow()
    setLifecycleOwner(getMyLifecycleOwner())
  }

  override fun onDetachedFromWindow() { //在clearFullscreenLayout和resolveNormalVideoShow中间执行
    super.onDetachedFromWindow()
    setLifecycleOwner(null)
    this.onVideoPause() //所以这里离开全屏的时候会调用
  }
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="退出全屏和恢复">
  override fun clearFullscreenLayout() {
    super.clearFullscreenLayout()
    isNeedRecoverPlay = isInPlayingState
  }

  override fun resolveNormalVideoShow(oldF: View?, vp: ViewGroup?, gsyVideoPlayer: GSYVideoPlayer?) {
    super.resolveNormalVideoShow(oldF, vp, gsyVideoPlayer)
    if (isNeedRecoverPlay) this.onVideoResume()
    isNeedRecoverPlay = false
  }
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="外部调用控件返回">
  open fun onBackPressed(): Boolean {
    if (mIfCurrentIsFullscreen) {
      mFullPlayer?.fullscreenButton?.performClick()
      return true
    }
    return false
  }
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="dp2px">
  protected fun dp2px(dpValue: Float): Int {
    val scale = Resources.getSystem().displayMetrics.density
    return (dpValue * scale + 0.5f).toInt()
  }
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="打印播放地址">
  override fun setUp(url: String?, cacheWithPlay: Boolean, cachePath: File?, title: String?, changeState: Boolean): Boolean {
    "当前播放地址: $url".logE()
    return super.setUp(url, cacheWithPlay, cachePath, title, changeState)
  }
  //</editor-fold>
}