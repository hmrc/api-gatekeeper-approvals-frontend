import play.core.PlayVersion
import play.sbt.PlayImport._
import sbt.Keys.libraryDependencies
import sbt._

object AppDependencies {

  val compile = Seq(
    "uk.gov.hmrc"             %% "bootstrap-frontend-play-28" % "5.17.0",
    "uk.gov.hmrc"             %% "play-frontend-hmrc"         % "1.31.0-play-28",
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-play-28"         % "0.58.0",
    "uk.gov.hmrc"             %% "http-metrics"               % "2.5.0-play-28",
    "uk.gov.hmrc"             %% "play-json-union-formatter"  % "1.15.0-play-28",
    "org.typelevel"           %% "cats-core"                  % "2.6.1",
    "uk.gov.hmrc"             %% "time"                       % "3.25.0",
    "com.beachape"            %% "enumeratum"                 % "1.5.12",
    "com.beachape"            %% "enumeratum-play"            % "1.5.12",
    "com.typesafe.play"       %% "play-json"                  % "2.9.2",
    "com.typesafe.play"       %% "play-json-joda"             % "2.9.2"
  )

  val test = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-28"     % "5.17.0",
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-test-play-28"    % "0.58.0",
    "org.jsoup"               %  "jsoup"                      % "1.13.1",
    "org.mockito"             %% "mockito-scala-scalatest"    % "1.16.46",
    "com.vladsch.flexmark"    %  "flexmark-all"               % "0.36.8",
    "com.github.tomakehurst"  %  "wiremock-jre8-standalone"   % "2.31.0"
  ).map(_ % "test, it")
}
