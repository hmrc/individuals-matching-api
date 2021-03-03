import play.core.PlayVersion
import sbt.Keys.compile
import sbt.Tests.{Group, SubProcess}
import uk.gov.hmrc.DefaultBuildSettings.{addTestReportOption, defaultSettings, scalaSettings}
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin.publishingSettings

val appName = "individuals-matching-api"
val hmrc = "uk.gov.hmrc"

lazy val appDependencies: Seq[ModuleID] = compile ++ test()
lazy val plugins: Seq[Plugins] = Seq.empty
lazy val playSettings: Seq[Setting[_]] = Seq.empty

def intTestFilter(name: String): Boolean = name startsWith "it"
def unitFilter(name: String): Boolean = name startsWith "unit"
def componentFilter(name: String): Boolean = name startsWith "component"

lazy val ComponentTest = config("component") extend Test

val akkaVersion = "2.5.23"

val akkaHttpVersion = "10.0.15"

dependencyOverrides += "com.typesafe.akka" %% "akka-stream" % akkaVersion

dependencyOverrides += "com.typesafe.akka" %% "akka-protobuf" % akkaVersion

dependencyOverrides += "com.typesafe.akka" %% "akka-slf4j" % akkaVersion

dependencyOverrides += "com.typesafe.akka" %% "akka-actor" % akkaVersion

dependencyOverrides += "com.typesafe.akka" %% "akka-http-core" % akkaHttpVersion

val compile = Seq(
  ws,
  hmrc                %% "bootstrap-backend-play-26" % "3.4.0",
  hmrc                %% "domain"                    % "5.10.0-play-26",
  hmrc                %% "auth-client"               % "3.3.0-play-26",
  hmrc                %% "simple-reactivemongo"      % "7.31.0-play-26",
  hmrc                %% "play-hal"                  % "2.1.0-play-26",
  hmrc                %% "play-hmrc-api"             % "5.3.0-play-26",
  "com.typesafe.play" %% "play-json-joda"            % "2.6.14"
)

def test(scope: String = "test,it") = Seq(
  hmrc                     %% "hmrctest"           % "3.10.0-play-26"    % scope,
  hmrc                     %% "reactivemongo-test" % "4.22.0-play-26"    % scope,
  "org.scalatest"          %% "scalatest"          % "3.0.9"             % scope,
  "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.3"             % scope,
  "org.pegdown"            % "pegdown"             % "1.6.0"             % scope,
  "org.mockito"            % "mockito-all"         % "1.10.19"           % scope,
  "org.scalaj"             %% "scalaj-http"        % "2.4.2"             % scope,
  "com.github.tomakehurst" % "wiremock-jre8"       % "2.27.2"            % scope,
  "com.typesafe.play"      %% "play-test"          % PlayVersion.current % scope
)

lazy val scoverageSettings = {
  import scoverage.ScoverageKeys
  Seq(
    // Semicolon-separated list of regexs matching classes to exclude
    ScoverageKeys.coverageExcludedPackages := "<empty>;Reverse.*;" +
      ".*BuildInfo.;uk.gov.hmrc.BuildInfo;.*Routes;.*RoutesPrefix*;",
    ScoverageKeys.coverageMinimum := 80,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true
  )
}

lazy val microservice =
  Project(appName, file("."))
    .enablePlugins(Seq(
      play.sbt.PlayScala,
      SbtAutoBuildPlugin,
      SbtGitVersioning,
      SbtDistributablesPlugin,
      SbtArtifactory) ++ plugins: _*)
    .settings(playSettings: _*)
    .settings(scalaSettings: _*)
    .settings(scoverageSettings: _*)
    .settings(publishingSettings: _*)
    .settings(scalaVersion := "2.12.11")
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
      parallelExecution in IntegrationTest := false
    )
    .configs(ComponentTest)
    .settings(inConfig(ComponentTest)(Defaults.testSettings): _*)
    .settings(
      testOptions in ComponentTest := Seq(Tests.Filter(componentFilter)),
      unmanagedSourceDirectories in ComponentTest := (baseDirectory in ComponentTest)(base => Seq(base / "test")).value,
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
  tests.map { test =>
    new Group(test.name, Seq(test), SubProcess(ForkOptions().withRunJVMOptions(Vector(s"-Dtest.name=${test.name}"))))
  }

lazy val compileAll = taskKey[Unit]("Compiles sources in all configurations.")

compileAll := {
  val a = (compile in Test).value
  val b = (compile in IntegrationTest).value
  ()
}
