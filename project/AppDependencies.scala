import play.sbt.PlayImport.ws
import sbt.*

object AppDependencies {

  val hmrc = "uk.gov.hmrc"

  val hmrcMongo = "uk.gov.hmrc.mongo"
  val hmrcMongoVersion = "1.3.0"
  val hmrcBootstrapVersion = "7.21.0"

  val compile: Seq[ModuleID] = Seq(
    ws,
    hmrc                %% "bootstrap-backend-play-28" % hmrcBootstrapVersion,
    hmrc                %% "domain"                    % "8.3.0-play-28",
    hmrc                %% "play-hal"                  % "3.4.0-play-28",
    hmrc                %% "play-hmrc-api"             % "7.1.0-play-28",
    hmrc                %% "json-encryption"           % "5.2.0-play-28",
    "com.typesafe.play" %% "play-json-joda"            % "2.9.2",
    hmrcMongo           %% "hmrc-mongo-play-28"        % hmrcMongoVersion
  )

  def test(scope: String = "test,it"): Seq[ModuleID] = Seq(
    "uk.gov.hmrc"            %% "bootstrap-test-play-28"   % hmrcBootstrapVersion % scope,
    "org.mockito"            %% "mockito-scala"            % "1.17.7"             % scope,
    "org.scalatestplus"      %% "scalacheck-1-17"          % "3.2.16.0"           % scope,
    "com.vladsch.flexmark"   % "flexmark-all"              % "0.64.0"             % scope,
    "org.scalaj"             %% "scalaj-http"              % "2.4.2"              % scope,
    "org.pegdown"            % "pegdown"                   % "1.6.0"              % scope,
    "com.github.tomakehurst" % "wiremock-jre8"             % "2.27.2"             % scope,
    hmrcMongo                %% "hmrc-mongo-test-play-28"  % hmrcMongoVersion     % scope
  )
}
