import play.core.PlayVersion
import play.sbt.PlayImport._
import sbt.Keys.libraryDependencies
import sbt._

object AppDependencies {

  lazy val bootstrapPlayVersion = "7.19.0"
  lazy val mongoVersion = "0.74.0"
  val apiDomainVersion = "0.11.0"
  val commonDomainVersion = "0.10.0"
  val appDomainVersion = "0.33.0-SNAPSHOT"

  val compile = Seq(
    "uk.gov.hmrc"             %% "bootstrap-frontend-play-28"         % bootstrapPlayVersion,
    "uk.gov.hmrc"             %% "play-frontend-hmrc"                 % "7.14.0-play-28",
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-play-28"                 % mongoVersion,
    "uk.gov.hmrc"             %% "http-metrics"                       % "2.7.0",
    "org.typelevel"           %% "cats-core"                          % "2.10.0",
    "com.typesafe.play"       %% "play-json"                          % "2.9.2",
    "uk.gov.hmrc"             %% "internal-auth-client-play-28"       % "1.2.0",
    "uk.gov.hmrc"             %% "api-platform-application-domain"    % appDomainVersion,
    "uk.gov.hmrc"             %% "api-platform-api-domain"            % apiDomainVersion
  )

  val test = Seq(
    "uk.gov.hmrc"                 %% "bootstrap-test-play-28"            % bootstrapPlayVersion,
    "uk.gov.hmrc.mongo"           %% "hmrc-mongo-test-play-28"           % mongoVersion,
    "org.jsoup"                   %  "jsoup"                             % "1.13.1",
    "org.mockito"                 %% "mockito-scala-scalatest"           % "1.17.29",
    "org.scalatest"               %% "scalatest"                         % "3.2.17",
    "com.vladsch.flexmark"        %  "flexmark-all"                      % "0.36.8",
    "com.github.tomakehurst"      %  "wiremock-jre8-standalone"          % "2.31.0",
    "uk.gov.hmrc"                 %% "api-platform-test-common-domain"   % commonDomainVersion,
  ).map(_ % "test, it")
}
