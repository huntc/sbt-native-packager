package com.typesafe.sbt.packager
package bundle

import sbt._

import universal._
import universal.Keys._
import bundle.Keys._
import org.apache.ivy.util.ChecksumHelper
import java.security.MessageDigest
import java.nio.charset.Charset

trait BundlePlugin extends Plugin with UniversalPlugin {
  val Bundle = config("bundle") extend Universal

  def bundleSettings: Seq[Setting[_]] = Seq(
    bundleType := Universal,
    dist in Bundle := Def.taskDyn {
      Def.task {
        createDist(bundleType.value)
      }.value
    }.value,
    stage in Bundle := Def.taskDyn {
      Def.task {
        stageBundle(bundleType.value)
      }.value
    }.value,
    stagingDirectory in Bundle := (target in Bundle).value / "stage",
    target in Bundle := target.value / "bundle"
  )

  val Sha256 = "SHA-256"
  val Utf8 = "UTF-8"

  val utf8Charset = Charset.forName(Utf8)
  val digest = MessageDigest.getInstance(Sha256)

  private def createConfig(target: File): File = {
    //TODO: Write the actual config
    val configFile = target / "bundle.conf"
    IO.touch(configFile)
    configFile
  }

  private def createDist(c: Configuration): Def.Initialize[Task[File]] = Def.task {
    val bundleTarget = (target in Bundle).value
    val configTarget = bundleTarget / "tmp"
    def relParent(p: (File, String)): (File, String) = (p._1, (name in Universal).value + java.io.File.separator + p._2)
    val bundleMappings =
      createConfig(configTarget).pair(relativeTo(configTarget)) ++
        (mappings in c).value.map(relParent)
    val tgz = Archives.makeTgz(bundleTarget, (name in Universal).value, bundleMappings)
    val hash = Hash.toHex(digest.digest(IO.read(tgz, utf8Charset).getBytes(utf8Charset)))
    val hashTgz = tgz.getParentFile / (hash + "-" + tgz.getName)
    IO.move(tgz, hashTgz)
    hashTgz
  }

  private def stageBundle(c: Configuration): Def.Initialize[Task[File]] = Def.task {
    val bundleTarget = (stagingDirectory in Bundle).value
    createConfig(bundleTarget)
    val componentTarget = bundleTarget / (name in Universal).value
    IO.copy((mappings in c).value.map(p => (p._1, componentTarget / p._2)))
    componentTarget
  }
}
