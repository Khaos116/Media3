package com.cc.media3

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView

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
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="重写的XML(需要保留原来的ID)">
  override fun getLayoutId() = R.layout.layout_my_gsy_video
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="初始化新增控件">
  override fun init(context: Context?) {
    super.init(context)
    this.isLooping = true //循环播放
    this.setIsTouchWiget(true) //允许滑动调整
    this.isNeedOrientationUtils = false //不需要屏幕旋转
    this.titleTextView.visibility = View.VISIBLE //显示title
    this.backButton.visibility = View.VISIBLE //显示返回键
    mStartButton2 = findViewById(R.id.start2)
    mStartButton2?.setOnClickListener { mStartButton?.performClick() }
    //</editor-fold>
  }

  override fun getEnlargeImageRes() = R.drawable.svg_player_video_full //全屏按钮
  override fun getShrinkImageRes() = R.drawable.svg_player_video_full //退出全屏按钮
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