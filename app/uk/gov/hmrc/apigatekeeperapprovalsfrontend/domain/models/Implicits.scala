/*
 * Copyright 2023 HM Revenue & Customs
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

import uk.gov.hmrc.apiplatform.modules.applications.access.domain.models._
import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.{ApplicationWithCollaborators, CoreApplication}
import uk.gov.hmrc.apiplatform.modules.applications.submissions.domain.models.ImportantSubmissionData

object Implicits {

  implicit class ApplicationSyntax(app: ApplicationWithCollaborators) {

    def importantSubmissionData: Option[ImportantSubmissionData] = app.details.access match {
      case Access.Standard(_, _, _, _, _, _, Some(submissionData)) => Some(submissionData)
      case _                                                       => None
    }

    def sellResellOrDistribute: Option[SellResellOrDistribute] = app.details.access match {
      case Access.Standard(_, _, _, _, _, sellResellOrDistribute, _) => sellResellOrDistribute
      case _                                                         => None
    }

    def isInHouseSoftware = sellResellOrDistribute.fold(false)(_ == SellResellOrDistribute("No"))
  }

  implicit class CoreApplicationSyntax(app: CoreApplication) {

    def importantSubmissionData: Option[ImportantSubmissionData] = app.access match {
      case Access.Standard(_, _, _, _, _, _, Some(submissionData)) => Some(submissionData)
      case _                                                       => None
    }

    def sellResellOrDistribute: Option[SellResellOrDistribute] = app.access match {
      case Access.Standard(_, _, _, _, _, sellResellOrDistribute, _) => sellResellOrDistribute
      case _                                                         => None
    }

    def isInHouseSoftware = sellResellOrDistribute.fold(false)(_ == SellResellOrDistribute("No"))
  }
}
