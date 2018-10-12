import play.routes.compiler.StaticRoutesGenerator
import play.sbt.PlayImport.PlayKeys
import sbt.Keys._
import sbt.Tests.{Group, SubProcess}
import sbt._
import uk.gov.hmrc.SbtArtifactory.autoImport.makePublicallyAvailableOnBintray
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin._
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion

trait MicroService {

  import uk.gov.hmrc._
  import DefaultBuildSettings._
  import play.sbt.routes.RoutesKeys.routesGenerator
  import uk.gov.hmrc.{SbtArtifactory, SbtAutoBuildPlugin}
  import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin
  import uk.gov.hmrc.versioning.SbtGitVersioning

  val appName: String

  lazy val appDependencies : Seq[ModuleID] = ???
  lazy val plugins : Seq[Plugins] = Seq.empty
  lazy val playSettings : Seq[Setting[_]] = Seq.empty

  def intTestFilter(name: String): Boolean = name startsWith "it"
  def unitFilter(name: String): Boolean = name startsWith "unit"
  def componentFilter(name: String): Boolean = name startsWith "component"

  lazy val microservice = Project(appName, file("."))
    .enablePlugins(Seq(play.sbt.PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin, SbtArtifactory) ++ plugins : _*)
    .settings(playSettings : _*)
    .settings(scalaSettings: _*)
    .settings(publishingSettings: _*)
    .settings(defaultSettings(): _*)
    .settings(
      libraryDependencies ++= appDependencies,
      testOptions in Test := Seq(Tests.Filter(unitFilter)),
      retrieveManaged := true,
      evictionWarningOptions in update := EvictionWarningOptions.default.withWarnScalaVersionEviction(false),
      routesGenerator := StaticRoutesGenerator
    )
    .settings(unmanagedResourceDirectories in Compile += baseDirectory.value / "resources")
    .configs(IntegrationTest)
    .settings(inConfig(IntegrationTest)(Defaults.itSettings): _*)
    .settings(
      Keys.fork in IntegrationTest := false,
      unmanagedSourceDirectories in IntegrationTest <<= (baseDirectory in IntegrationTest)(base => Seq(base / "test")),
      testOptions in IntegrationTest := Seq(Tests.Filter(intTestFilter)),
      addTestReportOption(IntegrationTest, "int-test-reports"),
      testGrouping in IntegrationTest := oneForkedJvmPerTest((definedTests in IntegrationTest).value),
      parallelExecution in IntegrationTest := false)
    .configs(ComponentTest)
    .settings(inConfig(ComponentTest)(Defaults.testSettings): _*)
    .settings(
      testOptions in ComponentTest := Seq(Tests.Filter(componentFilter)),
      unmanagedSourceDirectories   in ComponentTest <<= (baseDirectory in ComponentTest)(base => Seq(base / "test")),
      testGrouping in ComponentTest := oneForkedJvmPerTest((definedTests in ComponentTest).value),
      parallelExecution in ComponentTest := false
    )
    .settings(resolvers ++= Seq(
      Resolver.bintrayRepo("hmrc", "releases"),
      Resolver.jcenterRepo
    ))
    .settings(majorVersion := 0)
    .settings(PlayKeys.playDefaultPort := 9653)

  lazy val ComponentTest = config("component") extend Test

  def oneForkedJvmPerTest(tests: Seq[TestDefinition]) =
    tests map {
      test => new Group(test.name, Seq(test), SubProcess(ForkOptions(runJVMOptions = Seq("-Dtest.name=" + test.name))))
    }
}
