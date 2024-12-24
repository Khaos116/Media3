package com.cc.media3

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.*
import androidx.core.view.get
import com.shuyu.gsyvideoplayer.video.base.GSYBaseVideoPlayer

/**
 * Author:XX
 * Date:2024/12/20
 * Time:16:57
 */

class MyGsyVideoPlayer : MyGsyPlayer {
  //<editor-fold defaultstate="collapsed" desc="多构造">
  constructor(c: Context) : super(c)
  constructor(c: Context, b: Boolean?) : super(c)
  constructor(c: Context, a: AttributeSet?) : super(c, a)
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="新增加的控件">
  private var mStartButton2: ImageView? = null
  private var mTvSpeed: TextView? = null
  private var mViewChangeSpeed: View? = null
  private var mChangeSpeedParent: LinearLayout? = null
  private lateinit var mSpeedViews: MutableList<TextView>
  private lateinit var mSpeedList: MutableList<String>
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

  //<editor-fold defaultstate="collapsed" desc="进入全屏后的处理">
  override fun dealEnterFullPlayer(player: GSYBaseVideoPlayer) {
    super.dealEnterFullPlayer(player)
    (player as? MyGsyVideoPlayer)?.let { p ->
      p.mTvSpeed?.text = this.mTvSpeed?.text ?: "x1.00"
      p.speed = this.speed
      p.mSpeedViews.forEach { tv ->
        tv.setBackgroundResource(if (tv.text.toString() == p.mTvSpeed?.text?.toString()) R.drawable.shape_half_line_btn_bg_sel else R.drawable.shape_half_line_btn_bg_normal)
      }
    }
  }
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="退出全屏倍速显示同步处理">
  override fun clearFullscreenLayout() {
    (mFullPlayer as? MyGsyVideoPlayer)?.let { p ->
      val index = mSpeedList.map { a -> a.toFloat() }.indexOf(p.speed)
      if (index >= 0) {
        selSpeed(mSpeedViews[index])
      }
    }
    super.clearFullscreenLayout()
  }
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="处理开始和暂停图标">
  override fun updateStartImage() {
    when (mCurrentState) {
      CURRENT_STATE_PLAYING -> {
        (mStartButton as? ImageView)?.setImageResource(0)
        mStartButton2?.setImageResource(R.drawable.svg_player_video_pluse)
      }

      CURRENT_STATE_ERROR -> {
        (mStartButton as? ImageView)?.setImageResource(R.drawable.svg_player_video_start_big)
        mStartButton2?.setImageResource(R.drawable.svg_player_video_start)
      }

      else -> {
        (mStartButton as? ImageView)?.setImageResource(R.drawable.svg_player_video_start_big)
        mStartButton2?.setImageResource(R.drawable.svg_player_video_start)
      }
    }
  }
  //</editor-fold>
}