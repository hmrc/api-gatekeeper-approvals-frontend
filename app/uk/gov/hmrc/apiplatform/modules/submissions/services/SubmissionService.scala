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

package uk.gov.hmrc.apiplatform.modules.submissions.services

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

import uk.gov.hmrc.apiplatform.modules.submissions.connectors.SubmissionsConnector
import uk.gov.hmrc.apiplatform.modules.submissions.domain.models._
import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.{Application, ApplicationId}

@Singleton
class SubmissionService @Inject() (
    submissionConnector: SubmissionsConnector
  )(implicit val ec: ExecutionContext
  ) {

  def fetchLatestSubmission(appId: ApplicationId)(implicit hc: HeaderCarrier): Future[Option[Submission]] = submissionConnector.fetchLatestSubmission(appId)

  def fetchLatestMarkedSubmission(applicationId: ApplicationId)(implicit hc: HeaderCarrier): Future[Option[MarkedSubmission]] = {
    submissionConnector.fetchLatestMarkedSubmission(applicationId)
  }

  def grant(applicationId: ApplicationId, requestedBy: String)(implicit hc: HeaderCarrier): Future[Either[String, Application]] = {
    for {
      app <- submissionConnector.grant(applicationId, requestedBy)
    } yield app
  }

  def grantWithWarnings(
      applicationId: ApplicationId,
      requestedBy: String,
      warnings: String,
      escalatedTo: Option[String]
    )(implicit hc: HeaderCarrier
    ): Future[Either[String, Application]] = {
    for {
      app <- submissionConnector.grantWithWarnings(applicationId, requestedBy, warnings, escalatedTo)
    } yield app
  }

  def termsOfUseInvite(applicationId: ApplicationId)(implicit hc: HeaderCarrier) = {
    submissionConnector.termsOfUseInvite(applicationId)
  }

  def fetchTermsOfUseInvitation(applicationId: ApplicationId)(implicit hc: HeaderCarrier) = {
    submissionConnector.fetchTermsOfUseInvitation(applicationId)
  }

  def fetchTermsOfUseInvitations()(implicit hc: HeaderCarrier) = {
    submissionConnector.fetchTermsOfUseInvitations()
  }

  def grantOrDeclineForTouUplift(
      applicationId: ApplicationId,
      submission: Submission,
      requestedBy: String,
      reasons: String
    )(implicit hc: HeaderCarrier
    ): Future[Either[String, Application]] = {
    submission.latestInstance.status match {
      // if current submission state is Warnings, then grant with warnings
      case _: Submission.Status.Warnings => submissionConnector.grantWithWarningsForTouUplift(applicationId, requestedBy, reasons)
      // if current submission state is Failed, then decline
      case _: Submission.Status.Failed   => submissionConnector.declineForTouUplift(applicationId, requestedBy, reasons)
      case _                             => Future.successful(Left("Error - invalid submission status"))
    }
  }
}
