import sbt._

object AppDependencies {

  lazy val bootstrapPlayVersion = "9.0.0"
  lazy val mongoVersion = "2.1.0"
  val apiDomainVersion = "0.17.0"
  val commonDomainVersion = "0.17.0-SNAPSHOT"
  val appDomainVersion = "0.62.0-SNAPSHOT"

  val compile = Seq(
    "uk.gov.hmrc"             %% "bootstrap-frontend-play-30"         % bootstrapPlayVersion,
    "uk.gov.hmrc"             %% "play-frontend-hmrc-play-30"         % "10.5.0",
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-play-30"                 % mongoVersion,
    "uk.gov.hmrc"             %% "http-metrics"                       % "2.8.0",
    "org.typelevel"           %% "cats-core"                          % "2.10.0",
    "uk.gov.hmrc"             %% "internal-auth-client-play-30"       % "3.0.0",
    "uk.gov.hmrc"             %% "api-platform-common-domain"         % commonDomainVersion,   // Override transitive dep
    "uk.gov.hmrc"             %% "api-platform-application-domain"    % appDomainVersion,
    "uk.gov.hmrc"             %% "api-platform-api-domain"            % apiDomainVersion
  )

  val test = Seq(
    "uk.gov.hmrc"                 %% "bootstrap-test-play-30"                 % bootstrapPlayVersion,
    "uk.gov.hmrc.mongo"           %% "hmrc-mongo-test-play-30"                % mongoVersion,
    "org.jsoup"                   %  "jsoup"                                  % "1.15.4",
    "org.mockito"                 %% "mockito-scala-scalatest"                % "1.17.29",
    "uk.gov.hmrc"                 %% "api-platform-common-domain-fixtures"    % commonDomainVersion,   // Override transitive dep
    "uk.gov.hmrc"                 %% "api-platform-test-application-domain"   % appDomainVersion
  ).map(_ % "test")
}
