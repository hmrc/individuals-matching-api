import play.sbt.PlayImport.ws
import sbt._

object AppDependencies {

  val hmrc = "uk.gov.hmrc"
  val akka = "com.typesafe.akka"

  val akkaVersion = "2.6.15"
  val akkaHttpVersion = "10.2.6"

  val overrides = Seq(
    akka %% "akka-stream" % akkaVersion,
    akka %% "akka-protobuf" % akkaVersion,
    akka %% "akka-slf4j" % akkaVersion,
    akka %% "akka-actor" % akkaVersion,
    akka %% "akka-actor-typed" % akkaVersion,
    akka %% "akka-serialization-jackson" % akkaVersion,
    akka %% "akka-http-core" % akkaHttpVersion
  )

  val compile = Seq(
    ws,
    hmrc                %% "bootstrap-backend-play-28"  % "5.12.0",
    hmrc                %% "domain"                     % "6.2.0-play-28",
    hmrc                %% "play-hal"                   % "2.1.0-play-27",
    hmrc                %% "play-hmrc-api"              % "6.4.0-play-28",
    hmrc                %% "mongo-caching"              % "7.0.0-play-28",
    hmrc                %% "json-encryption"            % "4.10.0-play-28",
    "com.typesafe.play" %% "play-json-joda"             % "2.9.2"
  )

  def test(scope: String = "test,it") = Seq(
    "org.scalatestplus.play" %% "scalatestplus-play"       % "5.1.0"             % scope,
    "org.scalatestplus"      %% "mockito-3-4"              % "3.2.1.0"           % scope,
    "com.vladsch.flexmark"   % "flexmark-all"              % "0.35.10"           % scope,
    "org.scalaj"             %% "scalaj-http"              % "2.4.2"             % scope,
    "org.pegdown"            % "pegdown"                   % "1.6.0"             % scope,
    "com.github.tomakehurst" % "wiremock-jre8"             % "2.27.2"            % scope,
    hmrc                     %% "reactivemongo-test"       % "5.0.0-play-28"    % scope,
    hmrc                     %% "service-integration-test" % "1.1.0-play-28"     % scope
  )



}
