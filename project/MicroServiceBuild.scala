import play.core.PlayVersion
import play.sbt.PlayImport._
import sbt._

object MicroServiceBuild extends Build with MicroService {

  val appName = "individuals-matching-api"

  override lazy val appDependencies: Seq[ModuleID] = compile ++ test()

  val compile = Seq(
    "uk.gov.hmrc" %% "play-reactivemongo" % "6.0.0",
    ws,
    "uk.gov.hmrc" %% "microservice-bootstrap" % "5.16.0",
    "uk.gov.hmrc" %% "play-authorisation" % "4.3.0",
    "uk.gov.hmrc" %% "play-health" % "2.1.0",
    "uk.gov.hmrc" %% "play-config" % "4.3.0",
    "uk.gov.hmrc" %% "play-hmrc-api" % "1.4.0",
    "uk.gov.hmrc" %% "logback-json-logger" % "3.1.0",
    "uk.gov.hmrc" %% "domain" % "4.1.0",
    "uk.gov.hmrc" %% "play-hal" % "1.2.0",
    "uk.gov.hmrc" %% "play-auth" % "2.2.1"
  )

  def test(scope: String = "test,it") = Seq(
    "uk.gov.hmrc" %% "hmrctest" % "2.3.0" % scope,
    "org.scalatest" %% "scalatest" % "2.2.6" % scope,
    "org.scalatestplus.play" %% "scalatestplus-play" % "2.0.0" %  scope,
    "uk.gov.hmrc" %% "reactivemongo-test" % "2.0.0" % scope,
    "org.pegdown" % "pegdown" % "1.6.0" % scope,
    "org.mockito" % "mockito-all" % "1.10.19" % scope,
    "org.scalaj" %% "scalaj-http" % "1.1.6" % scope,
    "com.github.tomakehurst" % "wiremock" % "2.6.0" % scope,
    "com.typesafe.play" %% "play-test" % PlayVersion.current % scope
  )

}
