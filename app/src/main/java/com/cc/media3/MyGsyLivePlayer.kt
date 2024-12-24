package com.cc.media3

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.core.view.get
import androidx.core.view.isVisible
import com.shuyu.gsyvideoplayer.video.base.GSYBaseVideoPlayer

/**
 * Author:XX
 * Date:2024/12/20
 * Time:16:57
 */
@Suppress("DEPRECATION")
class MyGsyLivePlayer : MyGsyPlayer {
  //<editor-fold defaultstate="collapsed" desc="多构造">
  constructor(c: Context) : super(c)
  constructor(c: Context, b: Boolean?) : super(c)
  constructor(c: Context, a: AttributeSet?) : super(c, a)
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="重写的XML(需要保留原来的ID)">
  override fun getLayoutId() = R.layout.layout_my_gsy_live
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="新增加的控件">
  private var mStartButton2: ImageView? = null
  private var mCurrentRefresh: ImageView? = null
  private var mIvBack2: ImageView? = null
  private var mCurrentLine: TextView? = null

  //播放失败的显示
  private var mViewNoSignal: View? = null
  private var mTvChangeNoSignal: View? = null
  private var mTvRefreshNoSignal: View? = null

  //切换线路的显示
  private var mViewChangeLine: View? = null
  private var mChangeLineParent: LinearLayout? = null
  private var mTvLine1: TextView? = null
  private var mTvLine2: TextView? = null

  //URL
  private var mUrls: MutableList<String> = mutableListOf()

  //Header
  private var mHeaders: HashMap<String, String> = hashMapOf()
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="初始化新增控件">
  override fun init(context: Context?) {
    super.init(context)
    this.isLooping = true //循环播放
    this.setIsTouchWiget(true) //允许滑动调整
    this.isNeedOrientationUtils = false //不需要屏幕旋转
    this.titleTextView.visibility = View.VISIBLE //显示title
    this.backButton.visibility = View.VISIBLE //显示返回键
    //无信号
    mViewNoSignal = findViewById(R.id.rlNoSignal)
    mTvChangeNoSignal = findViewById(R.id.tvChangeLive)
    mTvRefreshNoSignal = findViewById(R.id.tvRefreshLive)
    mTvChangeNoSignal?.setOnClickListener {
      mViewNoSignal?.visibility = View.GONE
      mViewChangeLine?.visibility = View.VISIBLE
    }
    mTvRefreshNoSignal?.setOnClickListener {
      mViewNoSignal?.visibility = View.GONE
      if (!mOriginUrl.isNullOrBlank()) reStartPlay(mOriginUrl)
    }
    mIvBack2 = findViewById(R.id.backNoSignal)
    mIvBack2?.setOnClickListener {
      mViewNoSignal?.visibility = View.GONE
      (context as? Activity)?.onBackPressed()
    }
    //线路切换
    mViewChangeLine = findViewById(R.id.llLineChange)
    mChangeLineParent = findViewById(R.id.llLine)
    mTvLine1 = findViewById(R.id.tvLine1)
    mTvLine2 = findViewById(R.id.tvLine2)
    mViewChangeLine?.setOnClickListener {
      mViewChangeLine?.visibility = View.GONE
      if (mBottomContainer?.visibility != View.VISIBLE) {
        onClickUiToggle(null)
        startDismissControlViewTimer()
      }
    }
    mChangeLineParent?.setOnClickListener { mViewChangeLine?.performClick() }
    mTvLine1?.setOnClickListener { playByLineIndex(0) }
    mTvLine2?.setOnClickListener { playByLineIndex(1) }
    //显示线路
    mCurrentLine = findViewById(R.id.tvLine)
    mCurrentLine?.setOnClickListener { mViewChangeLine?.visibility = View.VISIBLE }
    //重新加载
    mCurrentRefresh = findViewById(R.id.ivReStart)
    mCurrentRefresh?.setOnClickListener { if (!mOriginUrl.isNullOrBlank()) reStartPlay(mOriginUrl) }
    //播放暂停
    mStartButton2 = findViewById(R.id.start2)
    mStartButton2?.setOnClickListener { mStartButton?.performClick() }
  }
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="进入全屏后的处理">
  override fun dealFullPlayer(player: GSYBaseVideoPlayer) {
    super.dealFullPlayer(player)
    (player as? MyGsyLivePlayer)?.let { p ->
      p.setLinesAndHeader(mUrls, mTitleTextView?.text?.toString(), mHeaders)
      p.mCurrentLine?.text = this.mCurrentLine?.text ?: "线路1"
      p.mCurrentLine?.visibility = View.VISIBLE
      p.mChangeLineParent?.let { pp ->
        val count = pp.childCount
        val index = p.mUrls.indexOf(p.mOriginUrl)
        for (i in 0 until count) {
          (pp[i] as? TextView)?.setBackgroundResource(if (i == index) R.drawable.shape_half_line_btn_bg_sel else R.drawable.shape_half_line_btn_bg_normal)
        }
      }
    }
  }
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="全屏按钮">
  override fun getEnlargeImageRes() = R.drawable.svg_player_live_full_enter //全屏按钮
  override fun getShrinkImageRes() = R.drawable.svg_player_live_full_exit //退出全屏按钮
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="处理开始和暂停图标">
  override fun updateStartImage() {
    when (mCurrentState) {
      CURRENT_STATE_PLAYING -> {
        (mStartButton as? ImageView)?.setImageResource(0)
        mStartButton2?.setImageResource(R.drawable.svg_player_live_pause)
        mViewNoSignal?.visibility = View.GONE
      }

      CURRENT_STATE_ERROR -> {
        (mStartButton as? ImageView)?.setImageResource(R.drawable.svg_player_video_start_big)
        mStartButton2?.setImageResource(R.drawable.svg_player_live_start)
        mViewChangeLine?.visibility = View.GONE
        mViewNoSignal?.visibility = View.VISIBLE
      }

      else -> {
        (mStartButton as? ImageView)?.setImageResource(R.drawable.svg_player_video_start_big)
        mStartButton2?.setImageResource(R.drawable.svg_player_live_start)
      }
    }
  }
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="外部设置线路">
  @SuppressLint("SetTextI18n")
  fun setLinesAndHeader(url: MutableList<String>, title: String?, header: HashMap<String, String>) {
    "播放线路:\n${url.mapIndexed { index, s -> "${index + 1}.$s" }.joinToString("\n")}".logE()
    mUrls = url
    mHeaders = header
    mTitleTextView?.text = title ?: ""
    mChangeLineParent?.let { p ->
      if (p.childCount > 2) { //如果超过2个控件，则移除其他控件
        for (i in p.childCount downTo 2) {
          p.removeView(p.getChildAt(i))
        }
      }
    }
    if (url.size <= 2) {
      when (url.size) {
        2 -> {
          mTvLine1?.visibility = View.VISIBLE
          mTvLine2?.visibility = View.VISIBLE
        }

        1 -> {
          mTvLine1?.visibility = View.VISIBLE
          mTvLine2?.visibility = View.GONE
        }

        else -> {
          mTvLine1?.visibility = View.GONE
          mTvLine2?.visibility = View.GONE
        }
      }
    } else {
      mTvLine1?.visibility = View.VISIBLE
      mTvLine2?.visibility = View.VISIBLE
      val params = MarginLayoutParams(-2, dp2px(28f))
      params.marginStart = dp2px(12f)
      params.marginEnd = dp2px(12f)
      for (i in 0 until url.size - 2) {
        val tv = TextView(context)
        tv.setTextColor(Color.WHITE)
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        tv.includeFontPadding = false
        tv.setPadding(dp2px(16f), 0, dp2px(16f), 0)
        tv.gravity = Gravity.CENTER
        tv.text = "线路${i + 3}"
        tv.setBackgroundResource(R.drawable.shape_half_line_btn_bg_normal)
        tv.setOnClickListener { playByLineIndex(i + 2) }
        mChangeLineParent?.addView(tv, params)
      }
    }
  }
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="设置选中线路">
  @SuppressLint("SetTextI18n")
  fun playByLineIndex(index: Int) {
    if (mViewChangeLine?.visibility == View.VISIBLE) {
      mViewChangeLine?.visibility = View.GONE
      if (mBottomContainer?.visibility != View.VISIBLE) {
        onClickUiToggle(null)
        startDismissControlViewTimer()
      }
    }
    mCurrentLine?.text = "线路${index + 1}"
    mCurrentLine?.visibility = View.VISIBLE
    if (mUrls.size > index && mOriginUrl != mUrls[index]) reStartPlay(mUrls[index])
  }
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="重新播放">
  private fun reStartPlay(url: String) {
    mViewNoSignal?.visibility = View.GONE
    if (isInPlayingState) onVideoPause()
    this.onVideoReset()
    this.setUp(url, false, mTitleTextView?.text?.toString())
    if (mHeaders.isNotEmpty()) this.mapHeadData = mHeaders
    this.startPlayLogic()
    val count = mChangeLineParent?.childCount ?: 0
    val index = mUrls.indexOf(url)
    for (i in 0 until count) {
      (mChangeLineParent?.get(i) as? TextView)?.setBackgroundResource(if (i == index) R.drawable.shape_half_line_btn_bg_sel else R.drawable.shape_half_line_btn_bg_normal)
    }
  }
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="处理底部进度条显示问题">
  override fun setViewShowState(view: View?, visibility: Int) {
    if (view == mBottomProgressBar && visibility == View.VISIBLE) {
      super.setViewShowState(view, View.INVISIBLE)
    } else {
      super.setViewShowState(view, visibility)
    }
  }
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="返回按钮处理">
  override fun onBackPressed(): Boolean {
    if (mViewNoSignal?.isVisible == true) {
      mViewNoSignal?.visibility = View.GONE
      return true
    } else if (mViewChangeLine?.isVisible == true) {
      mViewChangeLine?.visibility = View.GONE
      return true
    } else {
      return super.onBackPressed()
    }
  }
  //</editor-fold>
}