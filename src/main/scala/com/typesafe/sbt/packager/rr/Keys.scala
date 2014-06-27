package com.typesafe.sbt.packager.rr

import sbt._

trait BundleKeys {
  val bundleConf = taskKey[String]("The bundle configuration file contents")
  val bundleType = settingKey[Configuration]("The type of configuration that this bundling relates to. By default Universal is used.")
  val clusterRole = settingKey[String]("""The role of the component in the Reactive Runtime cluster. Defaults to "web-server"""")
  val endpoints = settingKey[Map[String, (String, String)]]("""Provides a port mapping between an external facing endpoing and an internal one. The default is Map("web" -> ("http://0.0.0.0:9000" -> "http://0.0.0.0:9000"))""")
  val startCommand = settingKey[Seq[String]]("Command line args required to start the component. Paths are expressed relative to the component's bin folder. The default is to use the bash script in the bin folder.")
}

object Keys extends BundleKeys