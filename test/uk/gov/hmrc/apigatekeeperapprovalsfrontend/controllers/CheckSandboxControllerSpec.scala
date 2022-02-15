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

import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.MessagesControllerComponents
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.config.{AppConfig, ErrorHandler}
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.{ApplicationId, SubmissionReview}
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.services.SubscriptionServiceMockModule
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.utils.{ApplicationTestData, AsyncHmrcSpec, WithCSRFAddToken}
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.views.html.CheckSandboxPage
import uk.gov.hmrc.apiplatform.modules.stride.config.StrideAuthConfig
import uk.gov.hmrc.apiplatform.modules.stride.connectors.mocks.{ApplicationActionServiceMockModule, ApplicationServiceMockModule, AuthConnectorMockModule}
import uk.gov.hmrc.apiplatform.modules.submissions.services.{SubmissionReviewServiceMockModule, SubmissionServiceMockModule}

import scala.concurrent.ExecutionContext.Implicits.global


class CheckSandboxControllerSpec extends AsyncHmrcSpec with GuiceOneAppPerSuite with WithCSRFAddToken {
  override def fakeApplication() =
    new GuiceApplicationBuilder()
      .configure(
        "metrics.jvm"     -> false,
        "metrics.enabled" -> false
      )
      .build()

  trait Setup
      extends AuthConnectorMockModule
      with ApplicationActionServiceMockModule
      with SubmissionServiceMockModule
      with SubmissionReviewServiceMockModule
      with ApplicationTestData
      with ApplicationServiceMockModule
      with SubscriptionServiceMockModule {
        
    implicit val appConfig = app.injector.instanceOf[AppConfig]

    val strideAuthConfig = app.injector.instanceOf[StrideAuthConfig]
    val forbiddenHandler = app.injector.instanceOf[HandleForbiddenWithView]
    val mcc = app.injector.instanceOf[MessagesControllerComponents]
    val checkSandboxPage = app.injector.instanceOf[CheckSandboxPage]
    val errorHandler = app.injector.instanceOf[ErrorHandler]

    val appId = ApplicationId.random
    val application = anApplication(id = appId)
    val submissionReview = SubmissionReview(markedSubmission.submission.id, 0)

    val controller = new CheckSandboxController(
      strideAuthConfig,
      AuthConnectorMock.aMock,
      forbiddenHandler,
      mcc,
      checkSandboxPage,
      errorHandler,
      ApplicationActionServiceMock.aMock,
      SubmissionServiceMock.aMock,
      SubmissionReviewServiceMock.aMock,
      ApplicationServiceMock.aMock,
      SubscriptionServiceMock.aMock
    )
  }

  "GET /" should {
    "return 200" in new Setup {
      val fakeRequest = FakeRequest().withCSRFToken
      val subordinateApplicationId = ApplicationId.random

      AuthConnectorMock.Authorise.thenReturn()
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(appId)
      SubmissionReviewServiceMock.FindOrCreateReview.thenReturn(submissionReview)
      ApplicationServiceMock.FetchLinkedSubordinateApplicationByApplicationId.thenReturn(subordinateApplicationId)
      SubscriptionServiceMock.FetchSubscriptionsByApplicationId.thenReturn(("serviceName1", "name1"), ("serviceName2", "name2"))

      val result = controller.checkSandboxPage(appId)(fakeRequest)
      status(result) shouldBe Status.OK
    }

    "return 404 if no application is found" in new Setup {
      val fakeRequest = FakeRequest().withCSRFToken
      val subordinateApplicationId = ApplicationId.random

      AuthConnectorMock.Authorise.thenReturn()
      ApplicationActionServiceMock.Process.thenNotFound

      val result = controller.checkSandboxPage(appId)(fakeRequest)
      status(result) shouldBe Status.NOT_FOUND
    }
  }

  "POST /" should {
    "update checked status and redirect to checklist page when Checked button is clicked" in new Setup {
      val fakeRequest = FakeRequest().withCSRFToken
        .withFormUrlEncodedBody("submit-action" -> "checked")

      AuthConnectorMock.Authorise.thenReturn()
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(appId)
      SubmissionReviewServiceMock.UpdateCheckedForSandboxTestingStatus.thenReturn(submissionReview)

      val result = controller.checkSandboxAction(appId)(fakeRequest)
      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some(s"/api-gatekeeper-approvals/applications/${appId.value}/checklist")
    }

    "update checked status and redirect to checklist page when Come Back Later button is clicked" in new Setup {
      val fakeRequest = FakeRequest().withCSRFToken
        .withFormUrlEncodedBody("submit-action" -> "come-back-later")

      AuthConnectorMock.Authorise.thenReturn()
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(appId)
      SubmissionReviewServiceMock.UpdateCheckedForSandboxTestingStatus.thenReturn(submissionReview)

      val result = controller.checkSandboxAction(appId)(fakeRequest)
      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some(s"/api-gatekeeper-approvals/applications/${appId.value}/checklist")
    }
  }

}
