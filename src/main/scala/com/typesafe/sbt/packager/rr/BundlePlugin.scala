package com.typesafe.sbt.packager
package rr

import sbt._

import universal._
import universal.Keys._
import rr.Keys._
import java.security.MessageDigest
import java.nio.charset.Charset

trait BundlePlugin extends Plugin with UniversalPlugin {
  val ReactiveRuntime = config("rr") extend Universal

  def bundleSettings: Seq[Setting[_]] = Seq(
    bundleConf := getConfig.value,
    bundleType := Universal,
    clusterRole := "web-server",
    dist in ReactiveRuntime := Def.taskDyn {
      Def.task {
        createDist(bundleType.value)
      }.value
    }.value,
    endpoints := Map("web" -> ("http://0.0.0.0:9000" -> "http://0.0.0.0:9000")),
    stage in ReactiveRuntime := Def.taskDyn {
      Def.task {
        stageBundle(bundleType.value)
      }.value
    }.value,
    stagingDirectory in ReactiveRuntime := (target in ReactiveRuntime).value / "stage",
    startCommand := Seq((file("bin") / (normalizedName in Universal).value).getPath),
    target in ReactiveRuntime := target.value / "reactive-runtime"
  )

  val Sha256 = "SHA-256"
  val Utf8 = "UTF-8"

  val utf8Charset = Charset.forName(Utf8)
  val digest = MessageDigest.getInstance(Sha256)

  private def getConfig: Def.Initialize[Task[String]] = Def.task {

    def format(s: Seq[String]): String = s.map(s => "\"" + s + "\"").mkString("[", ",", "]")

    def formatEndpoints(endpoints: Map[String, (String, String)]): String = {
      val formatted = for ((label, (from, to)) <- endpoints) yield label + " = " + format(Seq(from, to))
      formatted.mkString("{", ",", "}")
    }

    s"""
         |version = "1.0.0"
         |components = {
         |  "${(name in Universal).value}" = {
         |    description      = "${projectInfo.value.description}"
         |    cluster-role     = "${clusterRole.value}"
         |    file-system-type = "${bundleType.value}"
         |    start-command    = ${format(startCommand.value)}
         |    endpoints        = ${formatEndpoints(endpoints.value)}
         |  }
         |}
       """.stripMargin
  }

  private def createDist(bundleTypeConfig: Configuration): Def.Initialize[Task[File]] = Def.task {
    val bundleTarget = (target in ReactiveRuntime).value
    val configTarget = bundleTarget / "tmp"
    def relParent(p: (File, String)): (File, String) = (p._1, (name in Universal).value + java.io.File.separator + p._2)
    val configFile = writeConfig(configTarget, bundleConf.value)
    val bundleMappings =
      configFile.pair(relativeTo(configTarget)) ++
        (mappings in bundleTypeConfig).value.map(relParent)
    val tgz = Archives.makeTgz(bundleTarget, (name in Universal).value, bundleMappings)
    val hash = Hash.toHex(digest.digest(IO.read(tgz, utf8Charset).getBytes(utf8Charset)))
    val hashTgz = tgz.getParentFile / (tgz.getName + "-" + hash)
    IO.move(tgz, hashTgz)
    hashTgz
  }

  private def stageBundle(bundleTypeConfig: Configuration): Def.Initialize[Task[File]] = Def.task {
    val bundleTarget = (stagingDirectory in ReactiveRuntime).value
    writeConfig(bundleTarget, bundleConf.value)
    val componentTarget = bundleTarget / (name in Universal).value
    IO.copy((mappings in bundleTypeConfig).value.map(p => (p._1, componentTarget / p._2)))
    componentTarget
  }

  private def writeConfig(target: File, contents: String): File = {
    val configFile = target / "bundle.conf"
    IO.write(configFile, contents, utf8Charset)
    configFile
  }
}
