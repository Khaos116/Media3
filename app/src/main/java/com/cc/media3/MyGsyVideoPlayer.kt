package com.cc.media3

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.*
import androidx.core.view.get
import androidx.core.view.isVisible

/**
 * Author:XX
 * Date:2024/12/20
 * Time:16:57
 */

@Suppress("DEPRECATION")
class MyGsyVideoPlayer : MyGsyPlayer {
  //<editor-fold defaultstate="collapsed" desc="多构造">
  constructor(c: Context) : super(c)
  constructor(c: Context, b: Boolean?) : super(c, b)
  constructor(c: Context, a: AttributeSet?) : super(c, a)
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="新增加的控件">
  //左下角播放按钮
  private var mStartButton2: ImageView? = null

  //倍速处理
  private var mTvSpeed: TextView? = null
  private var mViewChangeSpeed: View? = null
  private var mChangeSpeedParent: LinearLayout? = null
  private lateinit var mSpeedViews: MutableList<TextView>
  private lateinit var mSpeedList: MutableList<String>

  //播放失败的显示
  private var mIvBack2: ImageView? = null
  private var mViewNoSignal: View? = null
  private var mTvRefreshNoSignal: View? = null
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="重写的XML(需要保留原来的ID)">
  override fun getLayoutId() = R.layout.layout_my_gsy_video
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="初始化新增控件">
  @SuppressLint("SetTextI18n")
  override fun init(context: Context?) {
    super.init(context)
    mSpeedViews = mutableListOf()
    mSpeedList = mutableListOf("0.50", "0.75", "1.00", "1.25", "1.50", "1.75", "2.00")
    this.isLooping = true //循环播放
    this.setIsTouchWiget(true) //允许滑动调整
    this.isNeedOrientationUtils = false //不需要屏幕旋转
    this.titleTextView.visibility = View.VISIBLE //显示title
    this.backButton.visibility = View.VISIBLE //显示返回键
    mStartButton2 = findViewById(R.id.start2)
    mTvSpeed = findViewById(R.id.tvSpeed)
    mStartButton2?.setOnClickListener { mStartButton?.performClick() }
    mTvSpeed?.setOnClickListener { mViewChangeSpeed?.visibility = View.VISIBLE }
    //无信号
    mViewNoSignal = findViewById(R.id.rlNoSignal)
    mTvRefreshNoSignal = findViewById(R.id.tvRefreshLive)
    mTvRefreshNoSignal?.setOnClickListener {
      mViewNoSignal?.visibility = View.GONE
      if (!mOriginUrl.isNullOrBlank()) reStartPlay(mOriginUrl)
    }
    mIvBack2 = findViewById(R.id.backNoSignal)
    mIvBack2?.setOnClickListener {
      mViewNoSignal?.visibility = View.GONE
      (context as? Activity)?.onBackPressed()
    }
    //播放速度选择
    mViewChangeSpeed = findViewById(R.id.llSpeedChange)
    mChangeSpeedParent = findViewById(R.id.llSpeed)
    mViewChangeSpeed?.setOnClickListener {
      mViewChangeSpeed?.visibility = View.GONE
      if (mBottomContainer?.visibility != View.VISIBLE) {
        onClickUiToggle(null)
        startDismissControlViewTimer()
      }
    }
    mChangeSpeedParent?.let { p ->
      p.setOnClickListener { mViewChangeSpeed?.performClick() }
      for (i in 0 until p.childCount) if (p[i] is TextView) mSpeedViews.add(p[i] as TextView)
      mSpeedViews.forEachIndexed { index, tv ->
        tv.text = "x${mSpeedList[index]}"
        tv.setOnClickListener { selSpeed(tv) }
      }
      selSpeed(mSpeedViews[mSpeedList.map { a -> a.toFloat() }.indexOf(this.speed)])
    }
  }
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="设置速度选中">
  @SuppressLint("SetTextI18n")
  private fun selSpeed(v: View) {
    mSpeedViews.forEach { tv -> tv.setBackgroundResource(if (tv == v) R.drawable.shape_half_line_btn_bg_sel else R.drawable.shape_half_line_btn_bg_normal) }
    val txt = mSpeedList[mSpeedViews.indexOf(v)]
    this.speed = txt.toFloat()
    mTvSpeed?.text = "x$txt"
    mViewChangeSpeed?.performClick()
  }
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="全屏按钮">
  override fun getEnlargeImageRes() = R.drawable.svg_player_video_full //全屏按钮
  override fun getShrinkImageRes() = R.drawable.svg_player_video_full //退出全屏按钮
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="处理开始和暂停图标">
  override fun updateStartImage() {
    when (mCurrentState) {
      CURRENT_STATE_PLAYING -> {
        (mStartButton as? ImageView)?.setImageResource(0)
        mStartButton2?.setImageResource(R.drawable.svg_player_video_pluse)
        mViewNoSignal?.visibility = View.GONE
      }

      CURRENT_STATE_ERROR -> {
        (mStartButton as? ImageView)?.setImageResource(R.drawable.svg_player_video_start_big)
        mStartButton2?.setImageResource(R.drawable.svg_player_video_start)
        mViewChangeSpeed?.visibility = View.GONE
        mViewNoSignal?.visibility = View.VISIBLE
      }

      else -> {
        (mStartButton as? ImageView)?.setImageResource(R.drawable.svg_player_video_start_big)
        mStartButton2?.setImageResource(R.drawable.svg_player_video_start)
      }
    }
  }
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="开始播放">
  fun startPlay(url: String, playWithCache: Boolean, title: String) {
    setUp(url, playWithCache, title)
    startPlayLogic()
  }
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="重新播放">
  private fun reStartPlay(url: String) {
    mViewNoSignal?.visibility = View.GONE
    if (isInPlayingState) onVideoPause()
    this.onVideoReset()
    this.setUp(url, false, mTitleTextView?.text?.toString())
    this.startPlayLogic()
  }
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="返回按钮处理">
  override fun onBackPressed(): Boolean {
    if (mViewNoSignal?.isVisible == true) {
      mViewNoSignal?.visibility = View.GONE
      return true
    } else if (mViewChangeSpeed?.isVisible == true) {
      mViewChangeSpeed?.visibility = View.GONE
      return true
    } else {
      return super.onBackPressed()
    }
  }
  //</editor-fold>
}