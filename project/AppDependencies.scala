import sbt.*

object AppDependencies {

  lazy val bootstrapPlayVersion = "10.7.0"
  val commonDomainVersion       = "1.4.0"
  val apiDomainVersion          = "1.8.0"
  val appDomainVersion          = "1.6.0"
  val playfrontendVersion       = "13.16.0"
  lazy val mongoVersion         = "2.14.0"

  val compile = Seq(
    "uk.gov.hmrc"       %% "bootstrap-frontend-play-30"      % bootstrapPlayVersion,
    "uk.gov.hmrc"       %% "play-frontend-hmrc-play-30"      % playfrontendVersion,
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-30"              % mongoVersion,
    "org.typelevel"     %% "cats-core"                       % "2.13.0",
    "uk.gov.hmrc"       %% "internal-auth-client-play-30"    % "4.4.0",
    "uk.gov.hmrc"       %% "api-platform-common-domain"      % commonDomainVersion,
    "uk.gov.hmrc"       %% "api-platform-application-domain" % appDomainVersion,
    "uk.gov.hmrc"       %% "api-platform-api-domain"         % apiDomainVersion
  )

  val test = Seq(
    "uk.gov.hmrc"       %% "bootstrap-test-play-30"                   % bootstrapPlayVersion,
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-test-play-30"                  % mongoVersion,
    "org.jsoup"          % "jsoup"                                    % "1.22.1",
    "uk.gov.hmrc"       %% "api-platform-common-domain-fixtures"      % commonDomainVersion,
    "uk.gov.hmrc"       %% "api-platform-application-domain-fixtures" % appDomainVersion
  ).map(_ % "test")
}
