import bloop.integrations.sbt.BloopDefaults
import uk.gov.hmrc.DefaultBuildSettings.integrationTestSettings
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin.publishingSettings

val appName = "api-gatekeeper-approvals-frontend"

lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
  .settings(
    majorVersion                     := 0,
    scalaVersion                     := "2.12.12",
    libraryDependencies              ++= AppDependencies.compile ++ AppDependencies.test,
    pipelineStages in Assets := Seq(gzip)
  )
  .settings(
    TwirlKeys.templateImports ++= Seq(
      "views.html.helper.CSPNonce",
      "uk.gov.hmrc.govukfrontend.views.html.components._",
      "uk.gov.hmrc.hmrcfrontend.views.html.components._",
      "uk.gov.hmrc.hmrcfrontend.views.html.helpers._"
    )
  )
  .settings(
    ThisBuild / scalafixDependencies += "com.github.liancheng" %% "organize-imports" % "0.6.0",
    ThisBuild / semanticdbEnabled := true,
    ThisBuild / semanticdbVersion := scalafixSemanticdb.revision
  )
  .settings(publishingSettings: _*)
  .settings(ScoverageSettings(): _*)
  .settings(SilencerSettings())
  .configs(IntegrationTest)
  .settings(integrationTestSettings(): _*)
  .settings(resolvers += Resolver.jcenterRepo)
  .settings(
    routesImport ++= Seq(
      "uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models._"
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

    
