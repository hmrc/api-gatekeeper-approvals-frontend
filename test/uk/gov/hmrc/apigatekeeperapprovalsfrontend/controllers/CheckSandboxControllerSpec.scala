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

package uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers

import play.api.http.Status
import play.api.test.Helpers._
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.ApplicationId
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.services.SubscriptionServiceMockModule
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.views.html.CheckSandboxPage
import uk.gov.hmrc.apiplatform.modules.stride.connectors.mocks.ApplicationServiceMockModule
import uk.gov.hmrc.apiplatform.modules.submissions.services.SubmissionReviewServiceMockModule
import uk.gov.hmrc.apiplatform.modules.submissions.SubmissionsTestData
import scala.concurrent.ExecutionContext.Implicits.global
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.utils.ApplicationTestData

class CheckSandboxControllerSpec extends AbstractControllerSpec with SubmissionsTestData {
  trait Setup
      extends AbstractSetup
      with ApplicationServiceMockModule
      with SubmissionReviewServiceMockModule
      with SubscriptionServiceMockModule
      with ApplicationTestData {
        
    val checkSandboxPage = app.injector.instanceOf[CheckSandboxPage]

    val controller = new CheckSandboxController(
      strideAuthConfig,
      AuthConnectorMock.aMock,
      forbiddenHandler,
      mcc,
      checkSandboxPage,
      errorHandler,
      SubmissionReviewServiceMock.aMock,
      ApplicationActionServiceMock.aMock,
      SubmissionServiceMock.aMock,
      ApplicationServiceMock.aMock,
      SubscriptionServiceMock.aMock
    )
  }

  "GET /" should {
    "return 200" in new Setup {
      val subordinateApplicationId = ApplicationId.random

      AuthConnectorMock.Authorise.thenReturn()
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(applicationId)
      SubmissionReviewServiceMock.FindOrCreateReview.thenReturn(submissionReview)
      ApplicationServiceMock.FetchLinkedSubordinateApplicationByApplicationId.thenReturn(subordinateApplicationId)
      SubscriptionServiceMock.FetchSubscriptionsByApplicationId.thenReturn(
        ("serviceName1", "name1", Array("CATEGORY_1")),
        ("serviceName2", "name2", Array("CATEGORY_2"))
      )

      val result = controller.checkSandboxPage(applicationId)(fakeRequest)
      status(result) shouldBe Status.OK
    }

    "return 404 if no application is found" in new Setup {
      AuthConnectorMock.Authorise.thenReturn()
      ApplicationActionServiceMock.Process.thenNotFound

      val result = controller.checkSandboxPage(applicationId)(fakeRequest)
      status(result) shouldBe Status.NOT_FOUND
    }
  }

  "POST /" should {
    "update checked status and redirect to checklist page when Checked button is clicked" in new Setup {
      AuthConnectorMock.Authorise.thenReturn()
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(applicationId)
      SubmissionReviewServiceMock.UpdateActionStatus.thenReturn(submissionReview)

      val result = controller.checkSandboxAction(applicationId)(fakeSubmitCheckedRequest)
      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some(s"/api-gatekeeper-approvals/applications/${applicationId.value}/checklist")
    }

    "update checked status and redirect to checklist page when Come Back Later button is clicked" in new Setup {
      AuthConnectorMock.Authorise.thenReturn()
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(applicationId)
      SubmissionReviewServiceMock.UpdateActionStatus.thenReturn(submissionReview)

      val result = controller.checkSandboxAction(applicationId)(fakeSubmitComebackLaterRequest)
      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some(s"/api-gatekeeper-approvals/applications/${applicationId.value}/checklist")
    }
  }

}
