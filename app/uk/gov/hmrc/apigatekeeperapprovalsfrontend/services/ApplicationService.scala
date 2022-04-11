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

package uk.gov.hmrc.apigatekeeperapprovalsfrontend.services

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.connectors.{ApmConnector, ThirdPartyApplicationConnector}
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.connectors.AddTermsOfUseAcceptanceRequest
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.{Application, ApplicationId, Standard, SubmissionReview}

import scala.concurrent.Future.successful

@Singleton
class ApplicationService @Inject()(
  thirdPartyApplicationConnector: ThirdPartyApplicationConnector,
  apmConnector: ApmConnector
)(implicit val ec: ExecutionContext) {
  
  def fetchByApplicationId(applicationId: ApplicationId)(implicit hc: HeaderCarrier): Future[Option[Application]] = {
    thirdPartyApplicationConnector.fetchApplicationById(applicationId)
  }

  def fetchLinkedSubordinateApplicationByApplicationId(applicationId: ApplicationId)(implicit hc: HeaderCarrier): Future[Option[Application]] = {
    apmConnector.fetchLinkedSubordinateApplicationById(applicationId)
  }

  def addTermsOfUseAcceptance(application: Application, submissionReview: SubmissionReview)(implicit hc: HeaderCarrier): Future[Either[String, Unit]] = {
    def maybeStandardAccess(application: Application) = application.access match {
      case stdAccess: Standard => Some(stdAccess)
      case _ => None
    }

    val maybeAddTermsOfUseAcceptanceRequest = for {
      standardAccess <- maybeStandardAccess(application)
      importantSubmissionData <- standardAccess.importantSubmissionData
      verifiedByDetails <- submissionReview.verifiedByDetails
      acceptanceDate <- verifiedByDetails.timestamp
    } yield AddTermsOfUseAcceptanceRequest(
      importantSubmissionData.responsibleIndividual.fullName,
      importantSubmissionData.responsibleIndividual.emailAddress,
      acceptanceDate,
      submissionReview.submissionId
    )

    maybeAddTermsOfUseAcceptanceRequest match {
      case Some(request) => {
        thirdPartyApplicationConnector.addTermsOfUseAcceptance(application.id, request).map(_ match {
          case Left(upstreamErrorResponse: UpstreamErrorResponse) => Left(upstreamErrorResponse.message)
          case Right(_) => Right(())
        })
      }
      case None => successful(Right(())) // ToU agreement not mandatory before granting prod creds
    }
  }
}
