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

import uk.gov.hmrc.apigatekeeperapprovalsfrontend.utils.AsyncHmrcSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.utils.WithCSRFAddToken

import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import uk.gov.hmrc.apiplatform.modules.stride.config.StrideAuthConfig
import uk.gov.hmrc.apiplatform.modules.stride.connectors.mocks.AuthConnectorMockModule

import scala.concurrent.ExecutionContext.Implicits.global
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.config.ErrorHandler
import uk.gov.hmrc.apiplatform.modules.stride.connectors.mocks.ApplicationActionServiceMockModule
import uk.gov.hmrc.apiplatform.modules.submissions.services.SubmissionServiceMockModule
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.ApplicationId
import play.api.test.FakeRequest
import play.api.http.Status
import play.api.test.Helpers._
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.Application
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.utils.WithCSRFAddToken

import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.views.html.EmailResponsibleIndividualPage
import uk.gov.hmrc.apiplatform.modules.submissions.services.SubmissionReviewServiceMockModule
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.SubmissionReview
import uk.gov.hmrc.apiplatform.modules.submissions.domain.models.MarkedSubmission

class EmailResponsibleIndividualControllerSpec extends AsyncHmrcSpec with GuiceOneAppPerSuite with WithCSRFAddToken {
  val strideAuthConfig = app.injector.instanceOf[StrideAuthConfig]
  val forbiddenHandler = app.injector.instanceOf[HandleForbiddenWithView]
  val mcc = app.injector.instanceOf[MessagesControllerComponents]
  val page = app.injector.instanceOf[EmailResponsibleIndividualPage]
  val errorHandler = app.injector.instanceOf[ErrorHandler]

  override def fakeApplication() =
    new GuiceApplicationBuilder()
      .configure(
        "metrics.jvm"     -> false,
        "metrics.enabled" -> false
      )
      .build()
  
  trait Setup extends AuthConnectorMockModule with ApplicationActionServiceMockModule with SubmissionServiceMockModule with SubmissionReviewServiceMockModule {
    val controller = new EmailResponsibleIndividualController(
      strideAuthConfig,
      AuthConnectorMock.aMock,
      forbiddenHandler,
      mcc,
      page,
      errorHandler,
      ApplicationActionServiceMock.aMock,
      SubmissionServiceMock.aMock,
      SubmissionReviewServiceMock.aMock
    )

    val appId = ApplicationId.random
    val application = Application(appId, "app name")
  }

  "emailResponsibleIndividualPage" should {
    "return 200" in new Setup {
      val fakeRequest = FakeRequest().withCSRFToken

      val mySubmission = MarkedSubmission(submittableSubmission.submit(), completedProgress, markedAnswers)
      
      AuthConnectorMock.Authorise.thenReturn()
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(mySubmission)

      val result = controller.page(appId)(fakeRequest)
      
      status(result) shouldBe Status.OK
    }

    "return 404 if the submission is not submitted" in new Setup {
      val fakeRequest = FakeRequest().withCSRFToken

      val mySubmission = MarkedSubmission(submittableSubmission, completedProgress, markedAnswers)

      AuthConnectorMock.Authorise.thenReturn()
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(mySubmission)

      val result = controller.page(appId)(fakeRequest)
      
      status(result) shouldBe Status.BAD_REQUEST
    }

    "return 404 if not found" in new Setup {
      val fakeRequest = FakeRequest().withCSRFToken

      AuthConnectorMock.Authorise.thenReturn()
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenNotFound()

      val result = controller.page(appId)(fakeRequest)
      
      status(result) shouldBe Status.NOT_FOUND
    }
  }

  "emailResponsibleIndividualAction" should {
    "redirect to correct page when marking URLs as checked" in new Setup {
      val fakeRequest = FakeRequest()
                          .withCSRFToken
                          .withFormUrlEncodedBody("submit-action" -> "checked")

      AuthConnectorMock.Authorise.thenReturn()
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(appId)
      SubmissionReviewServiceMock.UpdateEmailedResponsibleIndividualStatus.thenReturn(SubmissionReview(submissionId, 0))

      val result = controller.action(appId)(fakeRequest)

      status(result) shouldBe SEE_OTHER
    }

    "redirect to correct page when marking URLs as come-back-later" in new Setup {
      val fakeRequest = FakeRequest()
                          .withCSRFToken
                          .withFormUrlEncodedBody("submit-action" -> "come-back-later")

      AuthConnectorMock.Authorise.thenReturn()
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(appId)
      SubmissionReviewServiceMock.UpdateEmailedResponsibleIndividualStatus.thenReturn(SubmissionReview(submissionId, 0))

      val result = controller.action(appId)(fakeRequest)

      status(result) shouldBe SEE_OTHER
    }

    "return bad request when sending an empty submit-action" in new Setup {
      val fakeRequest = FakeRequest()
                          .withCSRFToken

      AuthConnectorMock.Authorise.thenReturn()
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(appId)

      val result = controller.action(appId)(fakeRequest)

      status(result) shouldBe BAD_REQUEST
    }
  }
}
