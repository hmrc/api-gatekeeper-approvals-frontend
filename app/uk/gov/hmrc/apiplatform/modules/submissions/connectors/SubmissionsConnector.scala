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

package uk.gov.hmrc.apiplatform.modules.submissions.connectors

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

import play.api.libs.json.{Json, Writes}
import uk.gov.hmrc.apiplatform.modules.common.domain.models.ApplicationId
import uk.gov.hmrc.apiplatform.modules.submissions.domain.models._
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, UpstreamErrorResponse}
import uk.gov.hmrc.play.http.metrics.common.API

import uk.gov.hmrc.apigatekeeperapprovalsfrontend.connectors.ConnectorMetrics
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models._

object SubmissionsConnector {

  case class Config(serviceBaseUrl: String, apiKey: String)

  case class GrantedRequest(gatekeeperUserName: String, warnings: Option[String] = None, escalatedTo: Option[String] = None)
  implicit val writesApprovedRequest: Writes[GrantedRequest] = Json.writes[GrantedRequest]

  case class DeclinedRequest(gatekeeperUserName: String, reasons: String)
  implicit val writesDeclinedRequest: Writes[DeclinedRequest] = Json.writes[DeclinedRequest]

  case class TouUpliftRequest(gatekeeperUserName: String, reasons: String)
  implicit val writesTouUpliftRequest: Writes[TouUpliftRequest] = Json.writes[TouUpliftRequest]

  case class TouGrantedRequest(gatekeeperUserName: String, reasons: String, escalatedTo: Option[String])
  implicit val writesTouGrantedRequest: Writes[TouGrantedRequest] = Json.writes[TouGrantedRequest]

  type ErrorOrUnit = Either[UpstreamErrorResponse, Unit]
}

@Singleton
class SubmissionsConnector @Inject() (
    val http: HttpClient,
    val config: SubmissionsConnector.Config,
    val metrics: ConnectorMetrics
  )(implicit val ec: ExecutionContext
  ) {

  import SubmissionsConnector._
  import config._
  import Submission._

  val api = API("third-party-application-submissions")

  def fetchLatestSubmission(applicationId: ApplicationId)(implicit hc: HeaderCarrier): Future[Option[Submission]] = {
    metrics.record(api) {
      http.GET[Option[Submission]](s"$serviceBaseUrl/submissions/application/${applicationId}")
    }
  }

  def fetchLatestMarkedSubmission(id: ApplicationId)(implicit hc: HeaderCarrier): Future[Option[MarkedSubmission]] = {
    import uk.gov.hmrc.http.HttpReads.Implicits._
    val url = s"$serviceBaseUrl/submissions/marked/application/${id}"

    metrics.record(api) {
      http.GET[Option[MarkedSubmission]](url)
    }
  }

  def termsOfUseInvite(applicationId: ApplicationId)(implicit hc: HeaderCarrier): Future[Either[String, TermsOfUseInvitationSuccessful]] = {
    val failed = (err: UpstreamErrorResponse) => s"Failed to invite for terms of use for application with id ${applicationId}: ${err}"

    metrics.record(api) {
      http.POSTEmpty[ErrorOrUnit](s"$serviceBaseUrl/terms-of-use/application/${applicationId}")
        .map {
          case Right(_)  => Right(TermsOfUseInvitationSuccessful)
          case Left(err) => Left(failed(err))
        }
    }
  }

  def fetchTermsOfUseInvitation(applicationId: ApplicationId)(implicit hc: HeaderCarrier): Future[Option[TermsOfUseInvitation]] = {
    metrics.record(api) {
      http.GET[Option[TermsOfUseInvitation]](s"$serviceBaseUrl/terms-of-use/application/${applicationId}")
    }
  }

  def fetchTermsOfUseInvitations()(implicit hc: HeaderCarrier): Future[List[TermsOfUseInvitation]] = {
    metrics.record(api) {
      http.GET[List[TermsOfUseInvitation]](s"$serviceBaseUrl/terms-of-use")
    }
  }

  def searchTermsOfUseInvitations(params: Seq[(String, String)])(implicit hc: HeaderCarrier): Future[List[TermsOfUseInvitationWithApplication]] = {
    metrics.record(api) {
      http.GET[List[TermsOfUseInvitationWithApplication]](s"$serviceBaseUrl/terms-of-use/search", params)
    }
  }

  def grantWithWarningsForTouUplift(
      applicationId: ApplicationId,
      requestedBy: String,
      warnings: String
    )(implicit hc: HeaderCarrier
    ): Future[Either[String, Application]] = {
    import cats.implicits._
    val failed = (err: UpstreamErrorResponse) => s"Failed to grant with warnings application ${applicationId}: ${err}"

    metrics.record(api) {
      http.POST[TouUpliftRequest, Either[UpstreamErrorResponse, Application]](
        s"$serviceBaseUrl/approvals/application/${applicationId}/grant-with-warn-tou",
        TouUpliftRequest(requestedBy, warnings)
      )
        .map(_.leftMap(failed(_)))
    }
  }

  def declineForTouUplift(
      applicationId: ApplicationId,
      requestedBy: String,
      reasons: String
    )(implicit hc: HeaderCarrier
    ): Future[Either[String, Application]] = {
    import cats.implicits._
    val failed = (err: UpstreamErrorResponse) => s"Failed to decline application ${applicationId}: ${err}"

    metrics.record(api) {
      http.POST[TouUpliftRequest, Either[UpstreamErrorResponse, Application]](
        s"$serviceBaseUrl/approvals/application/${applicationId}/decline-tou",
        TouUpliftRequest(requestedBy, reasons)
      )
        .map(_.leftMap(failed(_)))
    }
  }

  def resetForTouUplift(
      applicationId: ApplicationId,
      requestedBy: String,
      reasons: String
    )(implicit hc: HeaderCarrier
    ): Future[Either[String, Application]] = {
    import cats.implicits._
    val failed = (err: UpstreamErrorResponse) => s"Failed to reset application ${applicationId}: ${err}"

    metrics.record(api) {
      http.POST[TouUpliftRequest, Either[UpstreamErrorResponse, Application]](
        s"$serviceBaseUrl/approvals/application/${applicationId}/reset-tou",
        TouUpliftRequest(requestedBy, reasons)
      )
        .map(_.leftMap(failed(_)))
    }
  }
}
