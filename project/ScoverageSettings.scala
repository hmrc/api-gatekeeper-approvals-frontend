import scoverage.ScoverageKeys._

object ScoverageSettings {
  def apply() = Seq(
    coverageMinimumStmtTotal := 75,
    coverageFailOnMinimum := true,
    coverageHighlighting := true,
    coverageExcludedPackages :=  Seq(
      "<empty>",
      "prod.*",
      "testOnlyDoNotUseInAppConf.*",
      "app.*",
      ".*Reverse.*",
      ".*Routes.*",
      ".*definition.*",
      ".*BuildInfo.*",
      ".*javascript",
      """uk\.gov\.hmrc\.apiplatform\.modules\.common\..*""",
    ).mkString(";")
  )
}
