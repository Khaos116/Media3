package com.cc.media3

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.*
import com.shuyu.gsyvideoplayer.video.StandardGSYVideoPlayer
import com.shuyu.gsyvideoplayer.video.base.GSYVideoPlayer

/**
 * Author:XX
 * Date:2024/12/20
 * Time:21:44
 */
@Suppress("DEPRECATION")
open class MyGsyPlayer : StandardGSYVideoPlayer, LifecycleEventObserver {
  //<editor-fold defaultstate="collapsed" desc="多构造">
  constructor(c: Context) : super(c)
  constructor(c: Context, a: AttributeSet?) : super(c, a)
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="全屏处理">
  override fun init(context: Context?) {
    super.init(context)
    (getContext() as? Activity)?.let { ac ->
      this.backButton?.setOnClickListener { ac.onBackPressed() } //返回
      this.fullscreenButton?.setOnClickListener { //全屏按钮
        this.mShowFullAnimation = false //有动画会有延迟，导致mOrientationUtils没有初始化
        super.startWindowFullscreen(ac, true, true) //通过UI进行全屏播放
        mOrientationUtils?.resolveByClick() //横竖屏切换
      }
    }
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
    this.onVideoPause()//所以这里离开全屏的时候会调用
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
  fun onBackPressed(): Boolean {
    if (mIfCurrentIsFullscreen) {
      mOrientationUtils?.resolveByClick() //横竖屏切换
      return true
    }
    return false
  }
  //</editor-fold>
}