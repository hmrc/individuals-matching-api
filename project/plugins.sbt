resolvers += Resolver.url(
  "hmrc-sbt-plugin-releases",
  url("https://dl.bintray.com/hmrc/sbt-plugin-releases"))(
  Resolver.ivyStylePatterns)
resolvers += "Typesafe Releases" at "https://repo.typesafe.com/typesafe/releases/"
resolvers += "HMRC Releases" at "https://artefacts.tax.service.gov.uk/artifactory/hmrc-releases/"

addSbtPlugin("uk.gov.hmrc" % "sbt-auto-build" % "2.6.0")

addSbtPlugin("uk.gov.hmrc" % "sbt-git-versioning" % "2.1.0")

addSbtPlugin("uk.gov.hmrc" % "sbt-distributables" % "2.0.0")

addSbtPlugin("uk.gov.hmrc" % "sbt-artifactory" % "1.0.0")

addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.6.23" exclude("org.slf4j", "slf4j-simple"))

addSbtPlugin("com.lucidchart" % "sbt-scalafmt" % "1.16")