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

import org.joda.time.DateTime
import play.api.http.Status.INTERNAL_SERVER_ERROR
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.SubmissionReview.VerifiedByDetails

import scala.concurrent.ExecutionContext.Implicits.global
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.{ApplicationId, ImportantSubmissionData, PrivacyPolicyLocation, ResponsibleIndividual, Standard, SubmissionReview, TermsAndConditionsLocation, TermsOfUseAcceptance}
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.utils.{ApplicationTestData, AsyncHmrcSpec}
import uk.gov.hmrc.apiplatform.modules.stride.connectors.mocks.{ApmConnectorMockModule, ThirdPartyApplicationConnectorMockModule}
import uk.gov.hmrc.apiplatform.modules.submissions.domain.models.Submission
import uk.gov.hmrc.http.HeaderCarrier

class ApplicationServiceSpec extends AsyncHmrcSpec {

  trait Setup extends ThirdPartyApplicationConnectorMockModule with ApmConnectorMockModule with ApplicationTestData {
    implicit val hc: HeaderCarrier = HeaderCarrier()
    val applicationId = ApplicationId.random
    val service = new ApplicationService(ThirdPartyApplicationConnectorMock.aMock, ApmConnectorMock.aMock)

    val responsibleIndividual = ResponsibleIndividual("bob", "bob@example.com")
    val termsOfUseAcceptances = List(TermsOfUseAcceptance(responsibleIndividual, DateTime.now, Submission.Id.random))
    val importantSubmissionData = ImportantSubmissionData(
      Some("http://example.com"),
      responsibleIndividual,
      serverLocations = Set.empty,
      TermsAndConditionsLocation.InDesktopSoftware,
      PrivacyPolicyLocation.InDesktopSoftware,
      termsOfUseAcceptances
    )
    val standardAccess = Standard(importantSubmissionData = Some(importantSubmissionData))
    val application = anApplication().copy(access = standardAccess)

    val verifiedByDetails = VerifiedByDetails(true, Some(DateTime.now))
    val submissionReview = SubmissionReview.updateVerifiedByDetails(verifiedByDetails)(SubmissionReview(Submission.Id.random, 0, true, false, false, false))

    val importantSubmissionDataWithoutTOUAgreement = importantSubmissionData.copy(termsOfUseAcceptances = List.empty)
    val standardAccessWithoutTOUAgreement = Standard(importantSubmissionData = Some(importantSubmissionDataWithoutTOUAgreement))
    val applicationWithoutTOUAgreement = anApplication().copy(access = standardAccessWithoutTOUAgreement)
    val submissionReviewWithoutTOUAgreement = SubmissionReview(Submission.Id.random, 0, true, false, false, false)
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

  "addTermsOfUseAcceptance" should {
    "returns nothing if successful" in new Setup {
      ThirdPartyApplicationConnectorMock.AddTermsOfUseAcceptance.succeeds()

      val result = await(service.addTermsOfUseAcceptance(application, submissionReview))

      result shouldBe Right()
    }

    "returns nothing if ToU not agreed" in new Setup {
      ThirdPartyApplicationConnectorMock.AddTermsOfUseAcceptance.succeeds()

      val result = await(service.addTermsOfUseAcceptance(applicationWithoutTOUAgreement, submissionReviewWithoutTOUAgreement))

      result shouldBe Right()
    }

    "returns error message if TPA call fails" in new Setup {
      val errorMsg = "fail"
      ThirdPartyApplicationConnectorMock.AddTermsOfUseAcceptance.failsWith(INTERNAL_SERVER_ERROR, errorMsg)

      val result = await(service.addTermsOfUseAcceptance(application, submissionReview))

      result shouldBe Left(errorMsg)
    }
  }
}
