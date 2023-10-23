import play.core.PlayVersion
import play.sbt.PlayImport._
import sbt.Keys.libraryDependencies
import sbt._

object AppDependencies {

  lazy val bootstrapPlayVersion = "7.19.0"
  lazy val mongoVersion = "0.74.0"

  val compile = Seq(
    "uk.gov.hmrc"             %% "bootstrap-frontend-play-28"         % bootstrapPlayVersion,
    "uk.gov.hmrc"             %% "play-frontend-hmrc"                 % "7.14.0-play-28",
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-play-28"                 % mongoVersion,
    "uk.gov.hmrc"             %% "http-metrics"                       % "2.7.0",
    "org.typelevel"           %% "cats-core"                          % "2.6.1",
    "com.beachape"            %% "enumeratum"                         % "1.7.0",
    "com.beachape"            %% "enumeratum-play"                    % "1.7.0",
    "com.typesafe.play"       %% "play-json"                          % "2.9.2",
    "com.typesafe.play"       %% "play-json-joda"                     % "2.9.2",
    "uk.gov.hmrc"             %% "internal-auth-client-play-28"       % "1.2.0",
    "uk.gov.hmrc"             %% "api-platform-application-domain"    % "0.21.0",
    "uk.gov.hmrc"             %% "api-platform-api-domain"            % "0.7.0"
  )

  val test = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-28"             % bootstrapPlayVersion,
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-test-play-28"            % mongoVersion,
    "org.jsoup"               %  "jsoup"                              % "1.13.1",
    "org.mockito"             %% "mockito-scala-scalatest"            % "1.17.22",
    "com.vladsch.flexmark"    %  "flexmark-all"                       % "0.36.8",
    "com.github.tomakehurst"  %  "wiremock-jre8-standalone"           % "2.31.0"
  ).map(_ % "test, it")
}
