/*
 * Copyright 2022 HM Revenue & Customs
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

import enumeratum.{EnumEntry, PlayEnum}
import org.joda.time.DateTime
import uk.gov.hmrc.time.DateTimeUtils

case class ApplicationState(
                             name: State,
                             requestedByEmailAddress: Option[String] = None,
                             verificationCode: Option[String] = None,
                             updatedOn: DateTime = DateTimeUtils.now
                           )

object ApplicationState {
  import play.api.libs.json.Json
  import play.api.libs.json.JodaReads._
  import play.api.libs.json.JodaWrites._

  implicit val format = Json.format[ApplicationState]

  val testing = ApplicationState(State.TESTING, None)

  def pendingGatekeeperApproval(requestedBy: String) =
    ApplicationState(State.PENDING_GATEKEEPER_APPROVAL, Some(requestedBy))

  def pendingRequesterVerification(requestedBy: String, verificationCode: String) =
    ApplicationState(State.PENDING_REQUESTER_VERIFICATION, Some(requestedBy), Some(verificationCode))

  def production(requestedBy: String, verificationCode: String) =
    ApplicationState(State.PRODUCTION, Some(requestedBy), Some(verificationCode))
}

sealed trait State extends EnumEntry {
  def isApproved: Boolean = this == State.PRODUCTION

  def isPendingApproval: Boolean = (this == State.PENDING_REQUESTER_VERIFICATION
                          || this == State.PENDING_GATEKEEPER_APPROVAL)

  def isInTesting: Boolean = this == State.TESTING
}

object State extends PlayEnum[State] {
  val values = findValues

  final case object TESTING                         extends State
  final case object PENDING_GATEKEEPER_APPROVAL     extends State
  final case object PENDING_REQUESTER_VERIFICATION  extends State
  final case object PRODUCTION                      extends State
}

case class Application(
  id: ApplicationId,
  clientId: ClientId,
  // gatewayId: String,
  name: String,
  // deployedTo: String,
  // description: Option[String] = None,
  collaborators: Set[Collaborator],
  // createdOn: DateTime,
  // lastAccess: Option[DateTime],
  // grantLength: Int,
  // lastAccessTokenUsage: Option[DateTime] = None,  
  // redirectUris: List[String] = List.empty,
  // termsAndConditionsUrl: Option[String] = None,
  // privacyPolicyUrl: Option[String] = None,
  access: Access = Standard(List.empty, None, None),
  state: ApplicationState = ApplicationState(name = State.TESTING)
  // rateLimitTier: RateLimitTier = BRONZE,
  // checkInformation: Option[CheckInformation] = None,
  // blocked: Boolean = false,
  // trusted: Boolean = false,
  // ipAllowlist: IpAllowlist = IpAllowlist()
)

object Application {
  import play.api.libs.json.Json

  implicit val applicationReads = Json.reads[Application]
  implicit val applicationWrites = Json.writes[Application]
}