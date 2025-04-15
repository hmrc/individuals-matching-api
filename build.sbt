import uk.gov.hmrc.DefaultBuildSettings.addTestReportOption

val appName = "individuals-matching-api"

lazy val ComponentTest = config("component") extend Test

lazy val ItTest = config("it") extend Test

lazy val microservice =
  Project(appName, file("."))
    .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
    .disablePlugins(JUnitXmlReportPlugin) // Required to prevent https://github.com/scalatest/scalatest/issues/1427
    .settings(onLoadMessage := "")
    .settings(CodeCoverageSettings.settings *)
    .settings(scalaVersion := "3.3.5")
    .settings(scalafmtOnCompile := true)
    .settings(
      libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test()
    )
    .settings(Compile / unmanagedResourceDirectories += baseDirectory.value / "resources")
    .configs(ItTest)
    .settings(inConfig(ItTest)(Defaults.testSettings) *)
    .settings(
      ItTest / unmanagedSourceDirectories := (ItTest / baseDirectory)(base => Seq(base / "test")).value,
      ItTest / testOptions := Seq(Tests.Filter((name: String) => name startsWith "it")),
      addTestReportOption(ItTest, "int-test-reports"),
      // Disable default sbt Test options (might change with new versions of bootstrap)
      ItTest / testOptions -= Tests
        .Argument("-o", "-u", "target/int-test-reports", "-h", "target/int-test-reports/html-report"),
      ItTest / testOptions += Tests.Argument(
        TestFrameworks.ScalaTest,
        "-oNCHPQR",
        "-u",
        "target/int-test-reports",
        "-h",
        "target/int-test-reports/html-report"
      )
    )
    .configs(ComponentTest)
    .settings(inConfig(ComponentTest)(Defaults.testSettings) *)
    .settings(
      ComponentTest / testOptions := Seq(Tests.Filter((name: String) => name startsWith "component")),
      ComponentTest / unmanagedSourceDirectories := (ComponentTest / baseDirectory)(base => Seq(base / "test")).value,
      // Disable default sbt Test options (might change with new versions of bootstrap)
      ComponentTest / testOptions -= Tests
        .Argument("-o", "-u", "target/component-test-reports", "-h", "target/component-test-reports/html-report"),
      ComponentTest / testOptions += Tests.Argument(
        TestFrameworks.ScalaTest,
        "-oNCHPQR",
        "-u",
        "target/component-test-reports",
        "-h",
        "target/component-test-reports/html-report"
      )
    )
    .settings(majorVersion := 0)
    .settings(
      scalacOptions += "-Wconf:src=routes/.*:s",
      scalacOptions += "-Wconf:cat=unused-imports&src=txt/.*:s"
    )
    .settings(PlayKeys.playDefaultPort := 9653)
    .settings(Test / testOptions := Seq(Tests.Filter((name: String) => name.startsWith("unit"))))
    // Disable default sbt Test options (might change with new versions of bootstrap)
    .settings(
      Test / testOptions -= Tests
        .Argument("-o", "-u", "target/test-reports", "-h", "target/test-reports/html-report")
    )
    // Suppress successful events in Scalatest in standard output (-o)
    // Options described here: https://www.scalatest.org/user_guide/using_scalatest_with_sbt
    .settings(
      Test / testOptions += Tests.Argument(
        TestFrameworks.ScalaTest,
        "-oNCHPQR",
        "-u",
        "target/test-reports",
        "-h",
        "target/test-reports/html-report"
      )
    )
