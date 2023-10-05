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

package uk.gov.hmrc.apigatekeeperapprovalsfrontend.services

import scala.concurrent.ExecutionContext.Implicits.global

import org.joda.time.DateTime

import uk.gov.hmrc.apiplatform.modules.applications.domain.models.{PrivacyPolicyLocations, TermsAndConditionsLocations}
import uk.gov.hmrc.apiplatform.modules.commands.applications.domain.models._
import uk.gov.hmrc.apiplatform.modules.common.domain.models.{ApplicationId, LaxEmailAddress}
import uk.gov.hmrc.apiplatform.modules.gkauth.connectors.{ApmConnectorMockModule, ApplicationCommandConnectorMockModule, ThirdPartyApplicationConnectorMockModule}
import uk.gov.hmrc.apiplatform.modules.submissions.domain.models.Submission
import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models._
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.utils.{ApplicationTestData, AsyncHmrcSpec}

class ApplicationServiceSpec extends AsyncHmrcSpec {

  trait Setup extends ThirdPartyApplicationConnectorMockModule with ApmConnectorMockModule with ApplicationCommandConnectorMockModule with ApplicationTestData {
    implicit val hc: HeaderCarrier = HeaderCarrier()
    val applicationId              = ApplicationId.random
    val service                    = new ApplicationService(ThirdPartyApplicationConnectorMock.aMock, ApmConnectorMock.aMock, ApplicationCommandConnectorMock.aMock)

    val responsibleIndividual = ResponsibleIndividual("bob", LaxEmailAddress("bob@example.com"))
    val termsOfUseAcceptances = List(TermsOfUseAcceptance(responsibleIndividual, DateTime.now, Submission.Id.random, 0))

    val importantSubmissionData = ImportantSubmissionData(
      Some("http://example.com"),
      responsibleIndividual,
      serverLocations = Set.empty,
      TermsAndConditionsLocations.InDesktopSoftware,
      PrivacyPolicyLocations.InDesktopSoftware,
      termsOfUseAcceptances
    )
    val standardAccess          = Standard(importantSubmissionData = Some(importantSubmissionData))
    val application             = anApplication().copy(access = standardAccess)

    val submissionReview = SubmissionReview(Submission.Id.random, 0, true, false, false, false)

    val importantSubmissionDataWithoutTOUAgreement = importantSubmissionData.copy(termsOfUseAcceptances = List.empty)
    val standardAccessWithoutTOUAgreement          = Standard(importantSubmissionData = Some(importantSubmissionDataWithoutTOUAgreement))
    val applicationWithoutTOUAgreement             = anApplication().copy(access = standardAccessWithoutTOUAgreement)
    val submissionReviewWithoutTOUAgreement        = SubmissionReview(Submission.Id.random, 0, true, false, false, false)
  }

  "fetchByApplicationId" should {
    "return the correct application" in new Setup {
      ThirdPartyApplicationConnectorMock.FetchApplicationById.thenReturn()
      val result = await(service.fetchByApplicationId(applicationId))
      result.value.id shouldBe applicationId
    }
  }

  "fetchLinkedSubordinateApplicationByApplicationId" should {
    val subordinateApplicationId = ApplicationId.random
    "return the correct application" in new Setup {
      ApmConnectorMock.FetchLinkedSubordinateApplicationById.thenReturn(subordinateApplicationId)
      val result = await(service.fetchLinkedSubordinateApplicationByApplicationId(applicationId))
      result.value.id shouldBe subordinateApplicationId
    }
  }

  "declineApplicationApprovalRequest" should {
    "return the correct application" in new Setup {
      val requestedBy = "requestedBy"
      val reasons     = "reasons"
      val adminEmails = Set(LaxEmailAddress("bob@example.com"))
      ApplicationCommandConnectorMock.Dispatch.thenReturn(application)
      val result      = await(service.declineApplicationApprovalRequest(applicationId, requestedBy, reasons, adminEmails))
      result.isRight shouldBe true
      result shouldBe Right(DispatchSuccessResult(application))
    }
  }
}
