import sbt.*

object AppDependencies {

  lazy val bootstrapPlayVersion = "10.5.0"
  val apiDomainVersion          = "0.20.0"
  val appDomainVersion          = "0.95.0"
  val playfrontendVersion       = "12.26.0"
  lazy val mongoVersion         = "2.11.0"
  val mockitoScalaVersion       = "2.0.0"

  val compile = Seq(
    "uk.gov.hmrc"       %% "bootstrap-frontend-play-30"      % bootstrapPlayVersion,
    "uk.gov.hmrc"       %% "play-frontend-hmrc-play-30"      % playfrontendVersion,
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-30"              % mongoVersion,
    "uk.gov.hmrc"       %% "http-metrics"                    % "2.9.0",
    "org.typelevel"     %% "cats-core"                       % "2.10.0",
    "uk.gov.hmrc"       %% "internal-auth-client-play-30"    % "3.1.0",
    "uk.gov.hmrc"       %% "api-platform-application-domain" % appDomainVersion,
    "uk.gov.hmrc"       %% "api-platform-api-domain"         % apiDomainVersion
  )

  val test = Seq(
    "uk.gov.hmrc"       %% "bootstrap-test-play-30"                   % bootstrapPlayVersion,
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-test-play-30"                  % mongoVersion,
    "org.jsoup"          % "jsoup"                                    % "1.15.4",
    "org.mockito"       %% "mockito-scala-scalatest"                  % mockitoScalaVersion,
    "uk.gov.hmrc"       %% "api-platform-application-domain-fixtures" % appDomainVersion
  ).map(_ % "test")
}
