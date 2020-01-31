import play.core.PlayVersion
import sbt.Keys.compile
import sbt.Tests.{Group, SubProcess}
import uk.gov.hmrc.DefaultBuildSettings.{addTestReportOption, defaultSettings, scalaSettings}
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin.publishingSettings

val appName = "individuals-matching-api"
val hmrc = "uk.gov.hmrc"

lazy val appDependencies: Seq[ModuleID] = compile ++ test()
lazy val plugins : Seq[Plugins] = Seq.empty
lazy val playSettings : Seq[Setting[_]] = Seq.empty

def intTestFilter(name: String): Boolean = name startsWith "it"
def unitFilter(name: String): Boolean = name startsWith "unit"
def componentFilter(name: String): Boolean = name startsWith "component"

lazy val ComponentTest = config("component") extend Test

val compile = Seq(
  ws,
  hmrc %% "bootstrap-play-26" % "1.3.0",
  hmrc %% "domain" % "5.6.0-play-26",
  hmrc %% "auth-client" % "2.32.2-play-26",
  hmrc %% "simple-reactivemongo" % "7.23.0-play-26",
  hmrc %% "play-hal" % "1.9.0-play-26",
  hmrc %% "play-hmrc-api" % "3.9.0-play-26",
  "com.typesafe.play" %% "play-json-joda" % "2.6.10"
)

def test(scope: String = "test,it") = Seq(
  hmrc %% "hmrctest" % "3.9.0-play-26" % scope,
  hmrc %% "reactivemongo-test" % "4.16.0-play-26" % scope,
  "org.scalatest" %% "scalatest" % "3.0.8" % scope,
  "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" %  scope,
  "org.pegdown" % "pegdown" % "1.6.0" % scope,
  "org.mockito" % "mockito-all" % "1.10.19" % scope,
  "org.scalaj" %% "scalaj-http" % "2.4.2" % scope,
  "com.github.tomakehurst" % "wiremock-jre8" % "2.25.1" % scope,
  "com.typesafe.play" %% "play-test" % PlayVersion.current % scope
)

lazy val microservice = Project(appName, file("."))
  .enablePlugins(Seq(play.sbt.PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin, SbtArtifactory) ++ plugins : _*)
  .settings(playSettings : _*)
  .settings(scalaSettings: _*)
  .settings(publishingSettings: _*)
  .settings(defaultSettings(): _*)
  .settings(scalafmtOnCompile := true)
  .settings(
    libraryDependencies ++= appDependencies,
    testOptions in Test := Seq(Tests.Filter(unitFilter)),
    retrieveManaged := true,
    evictionWarningOptions in update := EvictionWarningOptions.default.withWarnScalaVersionEviction(false)
  )
  .settings(unmanagedResourceDirectories in Compile += baseDirectory.value / "resources")
  .configs(IntegrationTest)
  .settings(inConfig(IntegrationTest)(Defaults.itSettings): _*)
  .settings(
    Keys.fork in IntegrationTest := false,
    unmanagedSourceDirectories in IntegrationTest := (baseDirectory in IntegrationTest)(base => Seq(base / "test")).value,
    testOptions in IntegrationTest := Seq(Tests.Filter(intTestFilter)),
    addTestReportOption(IntegrationTest, "int-test-reports"),
    testGrouping in IntegrationTest := oneForkedJvmPerTest((definedTests in IntegrationTest).value),
    parallelExecution in IntegrationTest := false)
  .configs(ComponentTest)
  .settings(inConfig(ComponentTest)(Defaults.testSettings): _*)
  .settings(
    testOptions in ComponentTest := Seq(Tests.Filter(componentFilter)),
    unmanagedSourceDirectories   in ComponentTest := (baseDirectory in ComponentTest)(base => Seq(base / "test")).value,
    testGrouping in ComponentTest := oneForkedJvmPerTest((definedTests in ComponentTest).value),
    parallelExecution in ComponentTest := false
  )
  .settings(resolvers ++= Seq(
    Resolver.bintrayRepo("hmrc", "releases"),
    Resolver.jcenterRepo
  ))
  .settings(majorVersion := 0)
  .settings(PlayKeys.playDefaultPort := 9653)

def oneForkedJvmPerTest(tests: Seq[TestDefinition]) =
  tests map {
    test => Group(test.name, Seq(test), SubProcess(ForkOptions(runJVMOptions = Seq("-Dtest.name=" + test.name))))
  }

lazy val compileAll = taskKey[Unit]("Compiles sources in all configurations.")

compileAll := {
  val a = (compile in Test).value
  val b = (compile in IntegrationTest).value
  ()
}