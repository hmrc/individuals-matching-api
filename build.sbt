import play.core.PlayVersion
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
  hmrc %% "microservice-bootstrap" % "10.4.0",
  hmrc %% "domain" % "5.3.0",
  hmrc %% "auth-client" % "2.19.0-play-25",
  hmrc %% "play-reactivemongo" % "6.4.0",
  hmrc %% "play-hal" % "1.8.0-play-25",
  hmrc %% "play-hmrc-api" % "3.4.0-play-25"
)

def test(scope: String = "test,it") = Seq(
  hmrc %% "hmrctest" % "3.5.0-play-25" % scope,
  hmrc %% "reactivemongo-test" % "2.0.0" % scope,
  "org.scalatest" %% "scalatest" % "2.2.6" % scope,
  "org.scalatestplus.play" %% "scalatestplus-play" % "2.0.0" %  scope,
  "org.pegdown" % "pegdown" % "1.6.0" % scope,
  "org.mockito" % "mockito-all" % "1.10.19" % scope,
  "org.scalaj" %% "scalaj-http" % "1.1.6" % scope,
  "com.github.tomakehurst" % "wiremock" % "2.6.0" % scope,
  "com.typesafe.play" %% "play-test" % PlayVersion.current % scope
)

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

def oneForkedJvmPerTest(tests: Seq[TestDefinition]) =
  tests map {
    test => Group(test.name, Seq(test), SubProcess(ForkOptions(runJVMOptions = Seq("-Dtest.name=" + test.name))))
  }