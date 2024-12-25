package com.cc.media3

import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.lifecycle.*
import com.shuyu.gsyvideoplayer.utils.CommonUtil
import com.shuyu.gsyvideoplayer.video.StandardGSYVideoPlayer

/**
 * Author:XX
 * Date:2024/12/20
 * Time:21:44
 */
@Suppress("DEPRECATION", "MemberVisibilityCanBePrivate")
open class MyGsyPlayer : StandardGSYVideoPlayer, LifecycleEventObserver {
  //<editor-fold defaultstate="collapsed" desc="多构造">
  constructor(c: Context) : super(c)
  constructor(c: Context, b: Boolean?) : super(c, b ?: false)
  constructor(c: Context, a: AttributeSet?) : super(c, a)
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="初始化">
  override fun init(context: Context?) {
    super.init(context)
    this.isNeedShowWifiTip = false
    (getContext() as? Activity)?.let { ac ->
      this.backButton?.setOnClickListener { ac.onBackPressed() } //返回
      this.fullscreenButton?.setOnClickListener { //全屏按钮
        if (this.mIfCurrentIsFullscreen) {
          exitMyWindowFullscreen()
        } else {
          startMyWindowFullscreen()
        }
      }
    }
  }
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="dp2px">
  protected fun dp2px(dpValue: Float): Int {
    val scale = Resources.getSystem().displayMetrics.density
    return (dpValue * scale + 0.5f).toInt()
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
  override fun onAttachedToWindow() { //全屏的时候会重新克隆一份播放器到Window.ID_ANDROID_CONTENT控件里面
    super.onAttachedToWindow()
    setLifecycleOwner(getMyLifecycleOwner())
  }

  override fun onDetachedFromWindow() { //在clearFullscreenLayout和resolveNormalVideoShow中间执行
    super.onDetachedFromWindow()
    setLifecycleOwner(null)
  }
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="全屏和非全屏底部操作间距处理">
  private var mOriginBottomHeight = 0
  private var mPL = 0
  private var mPT = 0
  private var mPR = 0
  private var mPB = 0
  protected fun changeBottomHeight(full: Boolean) {
    if (mOriginBottomHeight == 0) {
      mOriginBottomHeight = mBottomContainer.height
      mPL = mBottomContainer.paddingStart
      mPT = mBottomContainer.paddingTop
      mPR = mBottomContainer.paddingEnd
      mPB = mBottomContainer.paddingBottom
    }
    val add = if (full) dp2px(9f) else 0
    mBottomContainer.layoutParams.height = mOriginBottomHeight + (if (full) (2 * add) else 0)
    mBottomContainer.setPadding(mPL + add, mPT + add, mPR + add, mPB + add)
  }
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="进入小窗和退出小窗处理">
  protected fun startMyWindowSmall(showBar: Boolean = false) {
  }

  protected fun exitMyWindowSmall() {
  }
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="进入全屏和退出全屏处理">
  private var mOriginParent: ViewGroup? = null
  protected fun startMyWindowFullscreen() {
    mOriginParent = this.parent as? ViewGroup
    viewGroup?.findViewById<View>(fullId)?.removeParent()
    viewGroup.addView(FrameLayout(mContext).also { fl ->
      fl.setBackgroundColor(Color.BLACK)
      this.removeParent()
      this.mIfCurrentIsFullscreen = true
      this.id = fullId
      fl.addView(this, ViewGroup.LayoutParams(-1, -1))
      CommonUtil.hideSupportActionBar(context, true, true)
      resolveFullVideoShow(context, this, fl)
      mOrientationUtils?.resolveByClick() //横竖屏切换
    }, ViewGroup.LayoutParams(-1, -1))
    changeBottomHeight(true)
    cancelDismissControlViewTimer()
    startDismissControlViewTimer()
    mVideoAllCallBack?.onEnterFullscreen(mOriginUrl, this)
  }

  protected fun exitMyWindowFullscreen() {
    (this.parent as? View)?.removeParent()
    this.removeParent()
    viewGroup?.findViewById<View>(fullId)?.removeParent()
    this.mIfCurrentIsFullscreen = false
    this.id = NO_ID
    mOriginParent?.addView(this, ViewGroup.LayoutParams(-1, -1))
    CommonUtil.showSupportActionBar(context, true, true)
    mOrientationUtils?.resolveByClick() //横竖屏切换
    changeBottomHeight(false)
    cancelDismissControlViewTimer()
    startDismissControlViewTimer()
    mVideoAllCallBack?.onQuitFullscreen(mOriginUrl, this)
  }
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="外部调用控件返回">
  open fun onBackPressed(): Boolean {
    if (mIfCurrentIsFullscreen) {
      fullscreenButton?.performClick()
      return true
    }
    return false
  }
  //</editor-fold>
}