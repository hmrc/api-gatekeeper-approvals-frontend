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
import uk.gov.hmrc.modules.stride.config.StrideAuthConfig
import uk.gov.hmrc.modules.stride.connectors.mocks.AuthConnectorMockModule

import scala.concurrent.ExecutionContext.Implicits.global
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.views.html.CheckAnswersThatFailedPage
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.config.ErrorHandler
import uk.gov.hmrc.modules.stride.connectors.mocks.ApplicationActionServiceMockModule
import uk.gov.hmrc.apiplatform.modules.submissions.services.SubmissionServiceMockModule
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.ApplicationId
import play.api.test.FakeRequest
import play.api.http.Status
import play.api.test.Helpers._
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.Application
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.utils.WithCSRFAddToken

import play.api.inject.guice.GuiceApplicationBuilder

class CheckAnswersThatFailedControllerSpec extends AsyncHmrcSpec with GuiceOneAppPerSuite with WithCSRFAddToken {
  val strideAuthConfig = app.injector.instanceOf[StrideAuthConfig]
  val forbiddenHandler = app.injector.instanceOf[HandleForbiddenWithView]
  val mcc = app.injector.instanceOf[MessagesControllerComponents]
  val page = app.injector.instanceOf[CheckAnswersThatFailedPage]
  val errorHandler = app.injector.instanceOf[ErrorHandler]

  override def fakeApplication() =
    new GuiceApplicationBuilder()
      .configure(
        "metrics.jvm"     -> false,
        "metrics.enabled" -> false
      )
      .build()

  trait Setup extends AuthConnectorMockModule with ApplicationActionServiceMockModule with SubmissionServiceMockModule {
    val controller = new CheckAnswersThatFailedController(
      strideAuthConfig,
      AuthConnectorMock.aMock,
      forbiddenHandler,
      mcc,
      page,
      errorHandler,
      ApplicationActionServiceMock.aMock,
      SubmissionServiceMock.aMock
    )

    val appId = ApplicationId.random
    val application = Application(appId, "app name")
  }

  "checkAnswersThatFailedPage" should {
    "return 200" in new Setup {
      val fakeRequest = FakeRequest().withCSRFToken

      AuthConnectorMock.Authorise.thenReturn()
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(appId)

      val result = controller.checkAnswersThatFailedPage(appId)(fakeRequest)
      
      status(result) shouldBe Status.OK
    }

    "return 200 if unknown questions exist" in new Setup {
      val fakeRequest = FakeRequest().withCSRFToken

      AuthConnectorMock.Authorise.thenReturn()
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturnIncludingAnUnknownQuestion(appId)

      val result = controller.checkAnswersThatFailedPage(appId)(fakeRequest)
      
      status(result) shouldBe Status.OK
    }

    "return 404" in new Setup {
      val fakeRequest = FakeRequest().withCSRFToken

      AuthConnectorMock.Authorise.thenReturn()
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenNotFound()

      val result = controller.checkAnswersThatFailedPage(appId)(fakeRequest)
      
      status(result) shouldBe Status.NOT_FOUND
    }
  }

  "checkAnswersThatFailedAction" should {
    "redirect to correct page when marking answers as checked" in new Setup {
      val fakeRequest = FakeRequest()
                          .withCSRFToken
                          .withFormUrlEncodedBody("submit-action" -> "checked")

      AuthConnectorMock.Authorise.thenReturn()
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(appId)

      val result = controller.checkAnswersThatFailedAction(appId)(fakeRequest)

      status(result) shouldBe SEE_OTHER
    }

    "redirect to correct page when marking answers as come-back-later" in new Setup {
      val fakeRequest = FakeRequest()
                          .withCSRFToken
                          .withFormUrlEncodedBody("submit-action" -> "come-back-later")

      AuthConnectorMock.Authorise.thenReturn()
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(appId)

      val result = controller.checkAnswersThatFailedAction(appId)(fakeRequest)

      status(result) shouldBe SEE_OTHER
    }

    "return bad request when marking answers as anything that we don't understand" in new Setup {
      val fakeRequest = FakeRequest()
                          .withCSRFToken
                          .withFormUrlEncodedBody("submit-action" -> "bobbins")

      AuthConnectorMock.Authorise.thenReturn()
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(appId)

      val result = controller.checkAnswersThatFailedAction(appId)(fakeRequest)

      status(result) shouldBe BAD_REQUEST
    }

    "return bad request when sending an empty submit-action" in new Setup {
      val fakeRequest = FakeRequest()
                          .withCSRFToken

      AuthConnectorMock.Authorise.thenReturn()
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(appId)

      val result = controller.checkAnswersThatFailedAction(appId)(fakeRequest)

      status(result) shouldBe BAD_REQUEST
    }
  }
}