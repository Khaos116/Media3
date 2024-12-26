package com.cc.media3

import android.util.Log
import android.view.View
import android.view.ViewManager
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleOwner

/**
 * Date:2024/12/20
 * Time:21:59
 */

//从父控件移除
fun View.removeParent() {
  val parentTemp = parent
  if (parentTemp is ViewManager) parentTemp.removeView(this)
}

//获取所有父类
fun View?.getMyParents(): MutableList<View> {
  val parents = mutableListOf<View>()
  var myParent: View? = this?.parent as? View //找父控件
  for (i in 0..Int.MAX_VALUE) {
    if (myParent != null) {
      parents.add(myParent) //添加到父控件列表
      myParent = myParent.parent as? View //继续向上查找父控件
    } else break //找不到View的父控件即结束
  }
  return parents
}

//找到View所在的fragment
fun View?.getMyFragment(): Fragment? {
  (this?.context as? FragmentActivity)?.let { ac ->
    //找到所有上级View
    val parents = getMyParents()
    //找到一级(activity嵌套的fragment)fragment
    val fragments = ac.supportFragmentManager.fragments
    //再找二级(fragment嵌套的fragment)fragment
    val list = mutableListOf<Fragment>()
    list.addAll(fragments)
    fragments.forEach { c -> list.addAll(c.getAllChildFragments()) }
    if (list.isNotEmpty()) for (i in list.size - 1 downTo 0) {
      list[i].view?.let { v -> if (parents.contains(v)) return list[i] }
    }
  }
  return null //如果都找不到，则应该不是放在fragment中，可能直接放在activity中了
}

//找到所有子fragment
fun Fragment.getAllChildFragments(): MutableList<Fragment> {
  val list = mutableListOf<Fragment>()
  if (!this.isAdded) return list
  val fragments = this.childFragmentManager.fragments
  if (fragments.isNotEmpty()) {
    list.addAll(fragments)
    fragments.forEach { f -> list.addAll(f.getAllChildFragments()) }
  }
  return list
}

//获取生命周期管理
fun View?.getMyLifecycleOwner(): LifecycleOwner? {
  return (this?.getMyFragment()) ?: (this?.context as? LifecycleOwner) ?: ((this?.parent as? View)?.context as? LifecycleOwner)
}

@Suppress("NOTHING_TO_INLINE")
inline fun String?.logE() {
  Log.e("EXT_LOG", this ?: "")
}