import sbt.Keys.compile
import sbt.Tests.{Group, SubProcess}
import uk.gov.hmrc.DefaultBuildSettings.{addTestReportOption, defaultSettings, scalaSettings}
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin.publishingSettings
import play.sbt.routes.RoutesKeys

val appName = "individuals-matching-api"
val hmrc = "uk.gov.hmrc"

lazy val appDependencies: Seq[ModuleID] = AppDependencies.compile ++ AppDependencies.test()
lazy val plugins: Seq[Plugins] = Seq.empty
lazy val playSettings: Seq[Setting[_]] = Seq.empty

def intTestFilter(name: String): Boolean = name startsWith "it"
def unitFilter(name: String): Boolean = name startsWith "unit"
def componentFilter(name: String): Boolean = name startsWith "component"

lazy val ComponentTest = config("component") extend Test

RoutesKeys.routesImport := Seq.empty
TwirlKeys.templateImports := Seq.empty

lazy val scoverageSettings = {
  import scoverage.ScoverageKeys
  Seq(
    // Semicolon-separated list of regexes matching classes to exclude
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
      ) ++ plugins: _*)
    .settings(playSettings: _*)
    .settings(scalaSettings: _*)
    .settings(scoverageSettings: _*)
    .settings(publishingSettings: _*)
    .settings(scalaVersion := "2.12.11")
    .settings(defaultSettings(): _*)
    .settings(scalafmtOnCompile := true)
    .settings(
      libraryDependencies ++= appDependencies,
      Test / testOptions := Seq(Tests.Filter(unitFilter)),
      retrieveManaged := true,
      update / evictionWarningOptions := EvictionWarningOptions.default.withWarnScalaVersionEviction(false)
    )
    .settings(Compile / unmanagedResourceDirectories += baseDirectory.value / "resources")
    .configs(IntegrationTest)
    .settings(inConfig(IntegrationTest)(Defaults.itSettings): _*)
    .settings(
      IntegrationTest / Keys.fork := false,
      IntegrationTest / unmanagedSourceDirectories := (IntegrationTest / baseDirectory)(base => Seq(base / "test")).value,
      IntegrationTest / testOptions := Seq(Tests.Filter(intTestFilter)),
      addTestReportOption(IntegrationTest, "int-test-reports"),
      IntegrationTest / testGrouping := oneForkedJvmPerTest((IntegrationTest / definedTests).value),
      IntegrationTest / parallelExecution := false
    )
    .configs(ComponentTest)
    .settings(inConfig(ComponentTest)(Defaults.testSettings): _*)
    .settings(
      ComponentTest / testOptions := Seq(Tests.Filter(componentFilter)),
      ComponentTest / unmanagedSourceDirectories := (ComponentTest / baseDirectory)(base => Seq(base / "test")).value,
      ComponentTest / testGrouping := oneForkedJvmPerTest((ComponentTest / definedTests).value),
      ComponentTest / parallelExecution := false
    )
    .settings(resolvers ++= Seq(
      Resolver.jcenterRepo
    ))
    .settings(majorVersion := 0)
    .settings(PlayKeys.playDefaultPort := 9653)

def oneForkedJvmPerTest(tests: Seq[TestDefinition]) =
  tests.map { test =>
    Group(test.name, Seq(test), SubProcess(ForkOptions().withRunJVMOptions(Vector(s"-Dtest.name=${test.name}"))))
  }

lazy val compileAll = taskKey[Unit]("Compiles sources in all configurations.")

compileAll := {
  val a = (Test / compile).value
  val b = (IntegrationTest / compile).value
  ()
}
