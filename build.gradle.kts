@file:Suppress("DEPRECATION")

import kotlinx.coroutines.*
import java.text.SimpleDateFormat

plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.jetbrains.kotlin.android) apply false
}

buildscript {
  dependencies {
    classpath(libs.classpath.apk.parser)
  }
}

gradle.taskGraph.whenReady {
  allTasks.forEach { task ->//测试版跳过Lint和Test相关的task,以加速编译
    if (task.name.contains("DebugAndroidTest")) {
      task.enabled = false
    }
    else if (task.name.contains("Debug")) {
      task.enabled = !task.name.contains("Lint") && !task.name.contains("Upload")
    }
  }
}

gradle.buildFinished {
  val hasRun = project.gradle.taskGraph.allTasks.any { t -> t.name.lowercase().contains("assemble") }
  CoroutineScope(Dispatchers.IO).launch {
    if (hasRun) {
      System.err.println("5秒后释放java进程(${SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(System.currentTimeMillis())})")
      delay(5 * 1000L) // 将延迟时间转换为毫秒
    }
    else {
      System.err.println("立即释放java进程(${SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(System.currentTimeMillis())})")
    }
    ProcessBuilder("TaskKill", "/F", "/T", "/IM", "java.exe").start().waitFor()
  }
}