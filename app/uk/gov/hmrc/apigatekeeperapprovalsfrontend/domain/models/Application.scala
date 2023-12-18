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

import java.time.Instant

import enumeratum.{EnumEntry, PlayEnum}

import uk.gov.hmrc.apiplatform.modules.applications.access.domain.models._
import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.Collaborator
import uk.gov.hmrc.apiplatform.modules.applications.submissions.domain.models.ImportantSubmissionData
import uk.gov.hmrc.apiplatform.modules.common.domain.models.{ApplicationId, ClientId}

case class ApplicationState(
    name: State,
    requestedByEmailAddress: Option[String] = None,
    verificationCode: Option[String] = None,
    updatedOn: Instant = Instant.now
  )

object ApplicationState {
  import play.api.libs.json.Format
  import play.api.libs.json.Json
  import uk.gov.hmrc.apiplatform.modules.common.domain.services.InstantJsonFormatter.WithTimeZone._

  implicit val format: Format[ApplicationState] = Json.format[ApplicationState]

  val testing = ApplicationState(State.TESTING, None)

  def pendingGatekeeperApproval(requestedBy: String) =
    ApplicationState(State.PENDING_GATEKEEPER_APPROVAL, Some(requestedBy))

  def pendingRequesterVerification(requestedBy: String, verificationCode: String) =
    ApplicationState(State.PENDING_REQUESTER_VERIFICATION, Some(requestedBy), Some(verificationCode))

  def production(requestedBy: String, verificationCode: String) =
    ApplicationState(State.PRODUCTION, Some(requestedBy), Some(verificationCode))

  def deleted(requestedBy: String) =
    ApplicationState(State.DELETED, Some(requestedBy))
}

sealed trait State extends EnumEntry {
  def isApproved: Boolean = this == State.PRODUCTION || this == State.PRE_PRODUCTION

  def isPendingApproval: Boolean = (this == State.PENDING_REQUESTER_VERIFICATION
    || this == State.PENDING_GATEKEEPER_APPROVAL)

  def isInTesting: Boolean = this == State.TESTING
}

object State extends PlayEnum[State] {
  val values = findValues

  final case object TESTING                                     extends State
  final case object PENDING_RESPONSIBLE_INDIVIDUAL_VERIFICATION extends State
  final case object PENDING_GATEKEEPER_APPROVAL                 extends State
  final case object PENDING_REQUESTER_VERIFICATION              extends State
  final case object PRE_PRODUCTION                              extends State
  final case object PRODUCTION                                  extends State
  final case object DELETED                                     extends State
}

case class Application(
    id: ApplicationId,
    clientId: ClientId,
    name: String,
    collaborators: Set[Collaborator],
    access: Access = Access.Standard(List.empty, None, None),
    state: ApplicationState = ApplicationState(name = State.TESTING)
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
