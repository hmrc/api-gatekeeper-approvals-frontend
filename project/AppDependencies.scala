import play.core.PlayVersion
import play.sbt.PlayImport._
import sbt.Keys.libraryDependencies
import sbt._

object AppDependencies {

  val compile = Seq(
    "uk.gov.hmrc"             %% "bootstrap-frontend-play-28" % "5.17.0",
    "uk.gov.hmrc"             %% "play-frontend-hmrc"         % "1.31.0-play-28",
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-play-28"         % "0.58.0",
    "org.typelevel"           %% "cats-core"                  % "2.6.1"
  )

  val test = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-28"     % "5.17.0",
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-test-play-28"    % "0.58.0",
    "org.jsoup"               %  "jsoup"                      % "1.13.1",
    "org.mockito"             %% "mockito-scala-scalatest"    % "1.16.46",
    "com.vladsch.flexmark"    %  "flexmark-all"               % "0.36.8"
  ).map(_ % Test)
}
