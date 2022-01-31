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
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.inject.guice.GuiceApplicationBuilder

import scala.concurrent.ExecutionContext.Implicits.global
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.config.AppConfig
import uk.gov.hmrc.apiplatform.modules.stride.config.StrideAuthConfig
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.apiplatform.modules.stride.connectors.mocks.ApplicationActionServiceMockModule
import uk.gov.hmrc.apiplatform.modules.stride.connectors.mocks.AuthConnectorMockModule
import uk.gov.hmrc.apiplatform.modules.submissions.services.SubmissionServiceMockModule
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.config.ErrorHandler
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.{ApplicationId,Application}
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.utils.AsyncHmrcSpec
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.views.html.ApplicationChecklistPage
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.utils.WithCSRFAddToken
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.views.html.ChecksCompletedPage
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.views.html.ApplicationApprovedPage
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.views.html.ApplicationDeclinedPage


class ChecksCompletedControllerSpec extends AsyncHmrcSpec with GuiceOneAppPerSuite with WithCSRFAddToken {
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
      with SubmissionServiceMockModule {
        
    implicit val appConfig = app.injector.instanceOf[AppConfig]

    val strideAuthConfig = app.injector.instanceOf[StrideAuthConfig]
    val forbiddenHandler = app.injector.instanceOf[HandleForbiddenWithView]
    val mcc = app.injector.instanceOf[MessagesControllerComponents]
    val appChecklistPage = app.injector.instanceOf[ApplicationChecklistPage]
    val errorHandler = app.injector.instanceOf[ErrorHandler]
    val page = app.injector.instanceOf[ChecksCompletedPage]
    val approvedPage = app.injector.instanceOf[ApplicationApprovedPage]
    val declinedPage = app.injector.instanceOf[ApplicationDeclinedPage]

    val controller = new ChecksCompletedController(
      strideAuthConfig,
      AuthConnectorMock.aMock,
      forbiddenHandler,
      mcc,
      errorHandler,
      ApplicationActionServiceMock.aMock,
      SubmissionServiceMock.aMock,
      page,
      approvedPage,
      declinedPage
    )
  }

  "GET /" should {
    "return 200" in new Setup {
      val appId = ApplicationId.random
      val fakeRequest = FakeRequest("GET", "/").withCSRFToken
      val application = Application(appId, "app name")

      AuthConnectorMock.Authorise.thenReturn()
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(appId)

      val result = controller.checksCompletedPage(appId)(fakeRequest)
      status(result) shouldBe Status.OK
    }

    "return 404 if marked submission not found" in new Setup {
      val appId = ApplicationId.random
      val fakeRequest = FakeRequest("GET", "/").withCSRFToken
      val application = Application(appId, "app name")

      AuthConnectorMock.Authorise.thenReturn()
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenNotFound()

      val result = controller.checksCompletedPage(appId)(fakeRequest)
      
      status(result) shouldBe Status.NOT_FOUND
    }
  }

  "POST /" should {
    "return 200 for grant" in new Setup {
      val appId = ApplicationId.random
      val application = Application(appId, "app name")
      val fakeRequest = FakeRequest()
                          .withCSRFToken
                          .withFormUrlEncodedBody("submit-action" -> "passed")

      AuthConnectorMock.Authorise.thenReturn()
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(appId)
      SubmissionServiceMock.Grant.thenReturn(appId, application)

      val result = controller.checksCompletedAction(appId)(fakeRequest)
      status(result) shouldBe Status.OK
    }

    "return 200 for decline" in new Setup {
      val appId = ApplicationId.random
      val application = Application(appId, "app name")
      val fakeRequest = FakeRequest()
                          .withCSRFToken
                          .withFormUrlEncodedBody("submit-action" -> "failed")

      AuthConnectorMock.Authorise.thenReturn()
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(appId)
      SubmissionServiceMock.Decline.thenReturn(appId, application)

      val result = controller.checksCompletedAction(appId)(fakeRequest)
      status(result) shouldBe Status.OK
    }

    "return bad request for unsuccessful approval" in new Setup {
      val appId = ApplicationId.random
      val application = Application(appId, "app name")
      val fakeRequest = FakeRequest()
                          .withCSRFToken
                          .withFormUrlEncodedBody("submit-action" -> "passed")

      AuthConnectorMock.Authorise.thenReturn()
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(appId)
      SubmissionServiceMock.Grant.thenReturnError(appId)

      val result = controller.checksCompletedAction(appId)(fakeRequest)
      status(result) shouldBe Status.BAD_REQUEST
    }

    "return bad request for unsuccessful decline" in new Setup {
      val appId = ApplicationId.random
      val application = Application(appId, "app name")
      val fakeRequest = FakeRequest()
                          .withCSRFToken
                          .withFormUrlEncodedBody("submit-action" -> "failed")

      AuthConnectorMock.Authorise.thenReturn()
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(appId)
      SubmissionServiceMock.Decline.thenReturnError(appId)

      val result = controller.checksCompletedAction(appId)(fakeRequest)
      status(result) shouldBe Status.BAD_REQUEST
    }
  }
}
