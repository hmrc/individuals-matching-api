import play.sbt.routes.RoutesKeys
import sbt.Tests.{Group, SubProcess}
import uk.gov.hmrc.DefaultBuildSettings.integrationTestSettings

val appName = "individuals-matching-api"

lazy val appDependencies: Seq[ModuleID] = AppDependencies.compile ++ AppDependencies.test()

def componentFilter(name: String): Boolean = name startsWith "component"

lazy val ComponentTest = config("component") extend Test

RoutesKeys.routesImport := Seq.empty
TwirlKeys.templateImports := Seq.empty

lazy val microservice =
  Project(appName, file("."))
    .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
    .settings(CodeCoverageSettings.settings *)
    .settings(scalaVersion := "2.13.8")
    .settings(scalafmtOnCompile := true)
    .settings(
      libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test()
    )
    .settings(Compile / unmanagedResourceDirectories += baseDirectory.value / "resources")
    .configs(IntegrationTest)
    .settings(integrationTestSettings() *)
    .configs(ComponentTest)
    .settings(inConfig(ComponentTest)(Defaults.testSettings) *)
    .settings(
      ComponentTest / testOptions := Seq(Tests.Filter(componentFilter)),
      ComponentTest / unmanagedSourceDirectories := (ComponentTest / baseDirectory)(base => Seq(base / "test")).value,
      ComponentTest / testGrouping := oneForkedJvmPerTest((ComponentTest / definedTests).value),
      ComponentTest / parallelExecution := false
    )
    .settings(majorVersion := 0)
    .settings(PlayKeys.playDefaultPort := 9653)

def oneForkedJvmPerTest(tests: Seq[TestDefinition]) =
  tests.map { test =>
    Group(test.name, Seq(test), SubProcess(ForkOptions().withRunJVMOptions(Vector(s"-Dtest.name=${test.name}"))))
  }
