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

import play.api.libs.json.Json
import uk.gov.hmrc.apiplatform.modules.submissions.domain.models._
import uk.gov.hmrc.apiplatform.modules.submissions.domain.services._
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, UpstreamErrorResponse}
import uk.gov.hmrc.play.http.metrics.common.API

import uk.gov.hmrc.apigatekeeperapprovalsfrontend.connectors.ConnectorMetrics
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models._

object SubmissionsConnector {

  case class Config(serviceBaseUrl: String, apiKey: String)

  case class GrantedRequest(gatekeeperUserName: String, warnings: Option[String] = None, escalatedTo: Option[String] = None)
  implicit val writesApprovedRequest = Json.writes[GrantedRequest]

  case class DeclinedRequest(gatekeeperUserName: String, reasons: String)
  implicit val writesDeclinedRequest = Json.writes[DeclinedRequest]

  case class TouUpliftRequest(gatekeeperUserName: String, reasons: String)
  implicit val writesTouUpliftRequest = Json.writes[TouUpliftRequest]

  type ErrorOrUnit = Either[UpstreamErrorResponse, Unit]
}

@Singleton
class SubmissionsConnector @Inject() (
    val http: HttpClient,
    val config: SubmissionsConnector.Config,
    val metrics: ConnectorMetrics
  )(implicit val ec: ExecutionContext
  ) extends SubmissionsJsonFormatters {

  import SubmissionsConnector._
  import config._

  val api = API("third-party-application-submissions")

  def fetchLatestSubmission(applicationId: ApplicationId)(implicit hc: HeaderCarrier): Future[Option[Submission]] = {
    metrics.record(api) {
      http.GET[Option[Submission]](s"$serviceBaseUrl/submissions/application/${applicationId.value}")
    }
  }

  def fetchLatestExtenedSubmission(applicationId: ApplicationId)(implicit hc: HeaderCarrier): Future[Option[ExtendedSubmission]] = {
    metrics.record(api) {
      http.GET[Option[ExtendedSubmission]](s"$serviceBaseUrl/submissions/application/${applicationId.value}/extended")
    }
  }

  def fetchLatestMarkedSubmission(id: ApplicationId)(implicit hc: HeaderCarrier): Future[Option[MarkedSubmission]] = {
    import uk.gov.hmrc.http.HttpReads.Implicits._
    val url = s"$serviceBaseUrl/submissions/marked/application/${id.value}"

    metrics.record(api) {
      http.GET[Option[MarkedSubmission]](url)
    }
  }

  def grant(applicationId: ApplicationId, requestedBy: String)(implicit hc: HeaderCarrier): Future[Either[String, Application]] = {
    import cats.implicits._
    val failed = (err: UpstreamErrorResponse) => s"Failed to grant application ${applicationId.value}: ${err}"

    metrics.record(api) {
      http.POST[GrantedRequest, Either[UpstreamErrorResponse, Application]](s"$serviceBaseUrl/approvals/application/${applicationId.value}/grant", GrantedRequest(requestedBy, None))
        .map(_.leftMap(failed(_)))
    }
  }

  def grantWithWarnings(
      applicationId: ApplicationId,
      requestedBy: String,
      warnings: String,
      escalatedTo: Option[String]
    )(implicit hc: HeaderCarrier
    ): Future[Either[String, Application]] = {
    import cats.implicits._
    val failed = (err: UpstreamErrorResponse) => s"Failed to grant application ${applicationId.value}: ${err}"

    metrics.record(api) {
      http.POST[GrantedRequest, Either[UpstreamErrorResponse, Application]](
        s"$serviceBaseUrl/approvals/application/${applicationId.value}/grant",
        GrantedRequest(requestedBy, warnings.some, escalatedTo)
      )
        .map(_.leftMap(failed(_)))
    }
  }

  def termsOfUseInvite(applicationId: ApplicationId)(implicit hc: HeaderCarrier): Future[Either[String, TermsOfUseInvitationSuccessful]] = {
    val failed = (err: UpstreamErrorResponse) => s"Failed to invite for terms of use for application with id ${applicationId.value}: ${err}"

    metrics.record(api) {
      http.POSTEmpty[ErrorOrUnit](s"$serviceBaseUrl/terms-of-use/application/${applicationId.value}")
        .map {
          case Right(_)  => Right(TermsOfUseInvitationSuccessful)
          case Left(err) => Left(failed(err))
        }
    }
  }

  def fetchTermsOfUseInvitation(applicationId: ApplicationId)(implicit hc: HeaderCarrier): Future[Option[TermsOfUseInvitation]] = {
    metrics.record(api) {
      http.GET[Option[TermsOfUseInvitation]](s"$serviceBaseUrl/terms-of-use/application/${applicationId.value}")
    }
  }

  def fetchTermsOfUseInvitations()(implicit hc: HeaderCarrier) = {
    metrics.record(api) {
      http.GET[List[TermsOfUseInvitation]](s"$serviceBaseUrl/terms-of-use")
    }
  }

  def grantWithWarningsForTouUplift(
      applicationId: ApplicationId,
      requestedBy: String,
      warnings: String
    )(implicit hc: HeaderCarrier
    ): Future[Either[String, Application]] = {
    import cats.implicits._
    val failed = (err: UpstreamErrorResponse) => s"Failed to grant with warnings application ${applicationId.value}: ${err}"

    metrics.record(api) {
      http.POST[TouUpliftRequest, Either[UpstreamErrorResponse, Application]](
        s"$serviceBaseUrl/approvals/application/${applicationId.value}/grant-with-warn-tou",
        TouUpliftRequest(requestedBy, warnings)
      )
        .map(_.leftMap(failed(_)))
    }
  }

  def grantForTouUplift(
      applicationId: ApplicationId,
      requestedBy: String,
      reasons: String
    )(implicit hc: HeaderCarrier
    ): Future[Either[String, Application]] = {
    import cats.implicits._
    val failed = (err: UpstreamErrorResponse) => s"Failed to grant application ${applicationId.value}: ${err}"

    metrics.record(api) {
      http.POST[TouUpliftRequest, Either[UpstreamErrorResponse, Application]](
        s"$serviceBaseUrl/approvals/application/${applicationId.value}/grant-tou",
        TouUpliftRequest(requestedBy, reasons)
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
    val failed = (err: UpstreamErrorResponse) => s"Failed to decline application ${applicationId.value}: ${err}"

    metrics.record(api) {
      http.POST[TouUpliftRequest, Either[UpstreamErrorResponse, Application]](
        s"$serviceBaseUrl/approvals/application/${applicationId.value}/decline-tou",
        TouUpliftRequest(requestedBy, reasons)
      )
        .map(_.leftMap(failed(_)))
    }
  }
}
