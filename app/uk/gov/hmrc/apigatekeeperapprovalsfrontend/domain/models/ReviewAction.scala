/*
 * Copyright 2026 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models

enum ReviewAction {
  case CheckFailsAndWarnings
  case CheckApplicationName
  case CheckCompanyRegistration
  case CheckUrls
  case CheckSandboxTesting
  case CheckFraudPreventionData
  case ArrangedDemo
  case CheckPassedAnswers
}

object ReviewAction {

  def fromText(text: String): Option[ReviewAction] = {
    import cats.implicits._
    text match {
      case "CheckFailsAndWarnings"    => CheckFailsAndWarnings.some
      case "CheckApplicationName"     => CheckApplicationName.some
      case "CheckCompanyRegistration" => CheckCompanyRegistration.some
      case "CheckUrls"                => CheckUrls.some
      case "CheckSandboxTesting"      => CheckSandboxTesting.some
      case "CheckFraudPreventionData" => CheckFraudPreventionData.some
      case "ArrangedDemo"             => ArrangedDemo.some
      case "CheckPassedAnswers"       => CheckPassedAnswers.some
      case _                          => None
    }
  }

  def toText(action: ReviewAction) = action match {
    case CheckFailsAndWarnings    => "CheckFailsAndWarnings"
    case CheckApplicationName     => "CheckApplicationName"
    case CheckCompanyRegistration => "CheckCompanyRegistration"
    case CheckUrls                => "CheckUrls"
    case CheckSandboxTesting      => "CheckSandboxTesting"
    case CheckFraudPreventionData => "CheckFraudPreventionData"
    case ArrangedDemo             => "ArrangedDemo"
    case CheckPassedAnswers       => "CheckPassedAnswers"
  }

  import play.api.libs.json._
  given KeyReads[ReviewAction]  = key => ReviewAction.fromText(key).fold[JsResult[ReviewAction]](JsError(s"Bad action key $key"))(a => JsSuccess(a))
  given KeyWrites[ReviewAction] = action => ReviewAction.toText(action)
}
