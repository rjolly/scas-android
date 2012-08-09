import sbt._

import Keys._
import AndroidKeys._

object General {
  val settings = Defaults.defaultSettings ++ Seq (
    name := "meditor",
    version := "1.0",
    versionCode := 0,
    scalaVersion := "2.9.2",
    platformName in Android := "android-8"
  )

  val proguardSettings = Seq (
    useProguard in Android := true
  )

  lazy val fullAndroidSettings =
    General.settings ++
    AndroidProject.androidSettings ++
    TypedResources.settings ++
    proguardSettings ++
    AndroidManifestGenerator.settings ++
    AndroidMarketPublish.settings ++ Seq (
      keyalias in Android := "mykey"
    )
}

object AndroidBuild extends Build {
  lazy val main = Project (
    "meditor",
    file("."),
    settings = General.fullAndroidSettings
  )
}
