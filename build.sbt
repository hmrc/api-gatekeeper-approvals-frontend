import sbt._
import bloop.integrations.sbt.BloopDefaults
import com.typesafe.sbt.digest.Import._
import com.typesafe.sbt.uglify.Import._
import net.ground5hark.sbt.concat.Import._
import uk.gov.hmrc.DefaultBuildSettings.integrationTestSettings
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin.publishingSettings

val appName = "api-gatekeeper-approvals-frontend"

Global / bloopAggregateSourceDependencies := true

ThisBuild / scalafixDependencies += "com.github.liancheng" %% "organize-imports" % "0.6.0"
ThisBuild / semanticdbEnabled := true
ThisBuild / semanticdbVersion := scalafixSemanticdb.revision

scalaVersion := "2.13.8"
 
lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
  .settings(
    majorVersion                     := 0,
    libraryDependencies              ++= AppDependencies.compile ++ AppDependencies.test,
    Assets / pipelineStages          := Seq(gzip)
  )
  .settings(
    Concat.groups := Seq(
      "javascripts/apis-app.js" -> group(
        (baseDirectory.value / "app" / "assets" / "javascripts") ** "*.js"
      )
    ),
    uglifyCompressOptions := Seq(
      "unused=false",
      "dead_code=true"
    ),
    uglify / includeFilter := GlobFilter("apis-*.js"),
    pipelineStages := Seq(digest),
    Assets / pipelineStages := Seq(
      concat,
      uglify
    )
  )
  .settings(
    TwirlKeys.templateImports ++= Seq(
      "views.html.helper.CSPNonce",
      "uk.gov.hmrc.govukfrontend.views.html.components._",
      "uk.gov.hmrc.hmrcfrontend.views.html.components._",
      "uk.gov.hmrc.hmrcfrontend.views.html.helpers._"
    )
  )
  .settings(ScoverageSettings(): _*)
  .configs(IntegrationTest)
  .settings(integrationTestSettings(): _*)
  .settings(
    routesImport ++= Seq(
      "uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models._",
      "uk.gov.hmrc.apiplatform.modules.common.domain.models._"
    )
  )
  .settings(
    Test / testOptions ++= Seq(Tests.Argument(TestFrameworks.ScalaTest, "-eT")),
    Test / unmanagedSourceDirectories += (baseDirectory.value / "test-common"),
    inConfig(Test)(BloopDefaults.configSettings)
  )
  .settings(
    IntegrationTest / testOptions ++= Seq(Tests.Argument(TestFrameworks.ScalaTest, "-eT")),
    IntegrationTest / unmanagedSourceDirectories += (baseDirectory.value / "test-common"),
    inConfig(IntegrationTest)(BloopDefaults.configSettings)
  )
  .settings(
    scalacOptions ++= Seq(
    "-Wconf:cat=unused&src=views/.*\\.scala:s",
    "-Wconf:cat=unused&src=.*RoutesPrefix\\.scala:s",
    "-Wconf:cat=unused&src=.*Routes\\.scala:s",
    "-Wconf:cat=unused&src=.*ReverseRoutes\\.scala:s",
    "-Wconf:cat=deprecation&src=.*Routes\\.scala:s"
    )
  )
