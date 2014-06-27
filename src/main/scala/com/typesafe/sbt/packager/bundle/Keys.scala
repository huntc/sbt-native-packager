package com.typesafe.sbt.packager.bundle

import sbt._

trait BundleKeys {
  val bundleType = settingKey[Configuration]("The type of configuration that this bundling relates to. By default Universal is used.")
}

object Keys extends BundleKeys