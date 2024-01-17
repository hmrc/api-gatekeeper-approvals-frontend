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
import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.{ApplicationState, Collaborator}
import uk.gov.hmrc.apiplatform.modules.applications.submissions.domain.models.ImportantSubmissionData
import uk.gov.hmrc.apiplatform.modules.common.domain.models.{ApplicationId, ClientId}

case class Application(
    id: ApplicationId,
    clientId: ClientId,
    name: String,
    collaborators: Set[Collaborator],
    access: Access = Access.Standard(List.empty, None, None),
    state: ApplicationState
  ) {

  lazy val importantSubmissionData: Option[ImportantSubmissionData] = access match {
    case Access.Standard(_, _, _, _, _, Some(submissionData)) => Some(submissionData)
    case _                                                    => None
  }

  lazy val sellResellOrDistribute: Option[SellResellOrDistribute] = access match {
    case Access.Standard(_, _, _, _, sellResellOrDistribute, _) => sellResellOrDistribute
    case _                                                      => None
  }

  lazy val isInHouseSoftware = sellResellOrDistribute.fold(false)(_ == SellResellOrDistribute("No"))

}

object Application {
  import play.api.libs.json._

  implicit val applicationReads: Reads[Application]   = Json.reads[Application]
  implicit val applicationWrites: Writes[Application] = Json.writes[Application]
}
