package com.cc.media3

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.graphics.Point
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.lifecycle.*
import com.shuyu.gsyvideoplayer.utils.CommonUtil
import com.shuyu.gsyvideoplayer.video.StandardGSYVideoPlayer
import com.shuyu.gsyvideoplayer.video.base.GSYBaseVideoPlayer
import com.shuyu.gsyvideoplayer.view.SmallVideoTouch

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
      this.mFullscreenButton?.setOnClickListener { //全屏按钮
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

  //<editor-fold defaultstate="collapsed" desc="进入全屏和退出全屏处理">
  private var mFullBeforeParent: ViewGroup? = null
  protected fun startMyWindowFullscreen() {
    mFullBeforeParent = this.parent as? ViewGroup
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
    mFullBeforeParent?.addView(this, ViewGroup.LayoutParams(-1, -1))
    CommonUtil.showSupportActionBar(context, true, true)
    mOrientationUtils?.resolveByClick() //横竖屏切换
    changeBottomHeight(false)
    cancelDismissControlViewTimer()
    startDismissControlViewTimer()
    mVideoAllCallBack?.onQuitFullscreen(mOriginUrl, this)
  }
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="进入小窗和退出小窗处理">
  private var mSmallBeforeParent: ViewGroup? = null
  protected var mIfCurrentIsSmallScreen = false
  open fun startMyWindowSmall() {
    mSmallBeforeParent = this.parent as? ViewGroup
    val ratio = 0.6f
    val newW = ((mSmallBeforeParent?.width ?: 0) * ratio).toInt()
    val newH = ((mSmallBeforeParent?.height ?: 0) * ratio).toInt()
    val pL = CommonUtil.getScreenWidth(mContext) - newW
    val pT = CommonUtil.getScreenHeight(mContext) - newH
    viewGroup?.findViewById<View>(smallId)?.removeParent()
    viewGroup.addView(FrameLayout(mContext).also { fl ->
      this.removeParent()
      this.mIfCurrentIsSmallScreen = true
      this.id = smallId
      fl.addView(this, MarginLayoutParams(newW, newH).also { lp ->
        lp.marginStart = pL / 2
        lp.topMargin = pT / 2
      })
    }, ViewGroup.LayoutParams(-1, -1))
    this.setIsTouchWiget(false)
    this.setSmallVideoTextureView(SmallVideoTouch(this, pL, pT)) //改为移动控件效果
    cancelDismissControlViewTimer()
    changeUiToClear()
    gsyVideoManager.setLastListener(this)
    gsyVideoManager.setListener(this)
    mSmallClose?.visibility = View.VISIBLE
    mSmallClose?.setOnClickListener { exitMyWindowSmall() }
    mVideoAllCallBack?.onEnterSmallWidget(mOriginUrl, this)
  }

  override fun showSmallVideo(size: Point?, actionBar: Boolean, statusBar: Boolean): GSYBaseVideoPlayer {
    return super.showSmallVideo(size, actionBar, statusBar)
  }

  override fun hideSmallVideo() {
    super.hideSmallVideo()
  }

  @SuppressLint("ClickableViewAccessibility")
  open fun exitMyWindowSmall() {
    (this.parent as? View)?.removeParent()
    this.removeParent()
    viewGroup?.findViewById<View>(smallId)?.removeParent()
    this.mIfCurrentIsSmallScreen = false
    this.id = NO_ID
    mSmallBeforeParent?.addView(this, ViewGroup.LayoutParams(-1, -1))
    this.setIsTouchWiget(true)
    this.setSmallVideoTextureView(this) //恢复手势操作事件
    this.mProgressBar?.setOnTouchListener(this) //恢复OnTouchListener事件,setSmallVideoTextureView去除
    this.mFullscreenButton?.setOnTouchListener(this) //恢复OnTouchListener事件,setSmallVideoTextureView去除
    if (this is MyGsyVideoPlayer) {
      mProgressBar?.visibility = View.VISIBLE
      mCurrentTimeTextView?.visibility = View.VISIBLE
    }
    mFullscreenButton?.visibility = View.VISIBLE
    mSmallClose?.visibility = View.GONE
    changeUiToPlayingShow()
    startDismissControlViewTimer()
    gsyVideoManager.setListener(this)
    gsyVideoManager.setLastListener(null)
    mVideoAllCallBack?.onQuitSmallWidget(mOriginUrl, this)
  }
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="操作UI显示和隐藏">
  override fun setViewShowState(view: View?, visibility: Int) {
    if (mIfCurrentIsSmallScreen) {
      when (view) {
        mStartButton,
        mLoadingProgressBar -> super.setViewShowState(view, visibility)

        mBottomProgressBar -> super.setViewShowState(view, if (this is MyGsyVideoPlayer) View.VISIBLE else View.INVISIBLE)

        mSmallClose -> super.setViewShowState(view, View.VISIBLE)

        else -> super.setViewShowState(view, View.INVISIBLE)
      }
    } else if (mIfCurrentIsFullscreen) {
      when (view) {
        mSmallClose -> super.setViewShowState(view, View.INVISIBLE)
        else -> super.setViewShowState(view, visibility)
      }
    } else {
      super.setViewShowState(view, visibility)
    }
  }
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="外部调用控件返回">
  open fun onBackPressed(): Boolean {
    if (mIfCurrentIsFullscreen) {
      exitMyWindowFullscreen()
      return true
    } else if (mIfCurrentIsSmallScreen) {
      exitMyWindowSmall()
      return true
    }
    return false
  }
  //</editor-fold>
}