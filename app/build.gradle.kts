import java.text.SimpleDateFormat

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.jetbrains.kotlin.android)
}

android {
  namespace = "com.cc.media3"
  compileSdk = 35

  defaultConfig {
    applicationId = "com.cc.media3"
    minSdk = 24
    //noinspection OldTargetApi
    targetSdk = 34
    versionCode = 100
    versionName = "1.0.0"
    resValue("string", "buildTime", SimpleDateFormat("yyyyMMdd_HHmm").format(System.currentTimeMillis()))
    resourceConfigurations.addAll(listOf("zh", "zh-rCN")) //可能三方会使用zh-rCN，所以需要保留(如果随便写一种没有的语言，则只会打默认的文字资源到APK)
    vectorDrawables {
      useSupportLibrary = true
    }
  }

  //https://github.com/owntracks/android/blob/43db0ad8428fa30e3edb1e27c9c08143e3e81693/project/app/build.gradle.kts
  signingConfigs {
    register("release") {
      storeFile = File("${rootDir}/mvi.jks")
      storePassword = "mvi123456"
      keyAlias = "mvi"
      keyPassword = "mvi123456"
      enableV1Signing = true
      enableV2Signing = true
      enableV3Signing = true
      enableV4Signing = true
    }
  }

  buildTypes {
    debug {
      signingConfig = signingConfigs.findByName("release")
      isShrinkResources = false //是否移除无用资源
      isMinifyEnabled = false //是否开启混淆
      applicationIdSuffix = ".debug"
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
    release {
      signingConfig = signingConfigs.findByName("release")
      isShrinkResources = true //是否移除无用资源
      isMinifyEnabled = true //是否开启混淆
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
  }
  kotlinOptions {
    jvmTarget = "1.8"
  }
  buildFeatures {
    buildConfig = true
    viewBinding = true
  }
  //https://github.com/whitechi73/OpenShamrock/blob/4adbc12a0bfa2220f230fcc0a7f92b7309d409eb/app/build.gradle.kts#L113
  packaging {
    resources {
      excludes += "/META-INF/{AL2.0,LGPL2.1}"
      excludes += "/META-INF/INDEX.LIST"
      excludes += "/META-INF/versions/9/OSGI-INF/MANIFEST.MF"
    }
  }
}

dependencies {
  implementation(libs.third.gsyvideoplayer.java)
  implementation(libs.third.gsyvideoplayer.exo2)
}

//<editor-fold defaultstate="collapsed" desc="打包处理">
//打包处理 https://github.com/Xposed-Modules-Repo/mufanc.tools.applock/blob/5507e105cb4fc30667f5b9d78c0eecf5348fd732/app/build.gradle.kts#L79
android.applicationVariants.all { //这里会走"渠道数"x2(Debug+Release)的次数
  outputs.all {
    //正式版还是测试版
    val apkBuildType = buildType.name.replaceFirstChar { it.uppercase() }
    //打包完成后执行APK复制到指定位置
    assembleProvider.get().doLast {
      //使用ApkParser库解析APK文件的清单信息
      val apkParser = net.dongliu.apk.parser.ApkFile(outputFile)
      val apkName = apkParser.apkMeta.label
      val apkVersion = apkParser.apkMeta.versionName
      val buildEndTime = SimpleDateFormat("yyyyMMdd_HHmm").format(System.currentTimeMillis())
      val apkFileName = "${apkName}_${apkBuildType}_${apkVersion}_${buildEndTime}.apk"
      val destDir = if ("Debug" == apkBuildType) {
        File(rootDir, "APK/${apkBuildType}").also {
          if (!it.exists()) it.mkdirs()
          com.android.utils.FileUtils.deleteDirectoryContents(it)
        }
      } else {
        File(rootDir, "APK/${apkBuildType}").also { if (!it.exists()) it.mkdirs() }
      }
      outputFile.copyTo(File(destDir, apkFileName), true)
    }
  }
}
//</editor-fold>