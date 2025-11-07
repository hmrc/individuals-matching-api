import sbt.*

object AppDependencies {
  val hmrc = "uk.gov.hmrc"

  val hmrcMongo = s"$hmrc.mongo"
  val playVersion = "play-30"
  val hmrcMongoVersion = "2.10.0"
  val hmrcBootstrapVersion = "10.4.0"

  val compile: Seq[ModuleID] = Seq(
    hmrc      %% s"bootstrap-backend-$playVersion" % hmrcBootstrapVersion,
    hmrc      %% s"domain-$playVersion"            % "13.0.0",
    hmrc      %% s"play-hal-$playVersion"          % "4.1.0",
    hmrcMongo %% s"hmrc-mongo-$playVersion"        % hmrcMongoVersion
  )

  def test(scope: String = "test, it"): Seq[ModuleID] = Seq(
    hmrc                %% s"bootstrap-test-$playVersion" % hmrcBootstrapVersion % scope,
    "org.scalatestplus" %% "scalacheck-1-17"              % "3.2.18.0"           % scope,
    "com.codacy"        %% "scalaj-http"                  % "2.5.0"              % scope
  )
}
