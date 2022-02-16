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
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.apiplatform.modules.stride.connectors.mocks.AuthConnectorMockModule
import uk.gov.hmrc.apiplatform.modules.stride.config.StrideAuthConfig
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.views.html.ViewDeclinedSubmissionPage
import uk.gov.hmrc.apiplatform.modules.stride.connectors.mocks.ApplicationActionServiceMockModule
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.config.ErrorHandler
import uk.gov.hmrc.apiplatform.modules.submissions.services.SubmissionServiceMockModule
import play.api.http.Status
import play.api.test.Helpers._

import scala.concurrent.ExecutionContext.Implicits.global
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.ApplicationId
import play.api.test.FakeRequest
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.utils.WithCSRFAddToken
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.Application
import uk.gov.hmrc.apiplatform.modules.submissions.SubmissionsTestData

class ViewDeclinedSubmissionControllerSpec extends AsyncHmrcSpec with GuiceOneAppPerSuite with WithCSRFAddToken with SubmissionsTestData {
  override def fakeApplication() =
    new GuiceApplicationBuilder()
      .configure(
        "metrics.jvm"     -> false,
        "metrics.enabled" -> false
      )
      .build()

  trait Setup extends AuthConnectorMockModule with ApplicationActionServiceMockModule with SubmissionServiceMockModule {
    val strideAuthConfig = app.injector.instanceOf[StrideAuthConfig]
    val forbiddenHandler = app.injector.instanceOf[HandleForbiddenWithView]
    val mcc = app.injector.instanceOf[MessagesControllerComponents]
    val viewDeclinedSubmissionPage = app.injector.instanceOf[ViewDeclinedSubmissionPage]
    val errorHandler = app.injector.instanceOf[ErrorHandler]

    val controller = new ViewDeclinedSubmissionController(
      strideAuthConfig,
      AuthConnectorMock.aMock,
      forbiddenHandler,
      mcc,
      viewDeclinedSubmissionPage,
      errorHandler,
      ApplicationActionServiceMock.aMock,
      SubmissionServiceMock.aMock
    )
  }

  "GET /" should {

    "return 200" in new Setup {
      val appId = ApplicationId.random
      val fakeRequest = FakeRequest("GET", "/").withCSRFToken
      val application = Application(appId, "app name")

      AuthConnectorMock.Authorise.thenReturn()
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturnWith(appId, declinedSubmission)

      val result = controller.page(appId, 0)(fakeRequest)
      status(result) shouldBe Status.OK
    }

    "return 400 when given a submission index that doesn't exist" in new Setup {
      val appId = ApplicationId.random
      val fakeRequest = FakeRequest("GET", "/").withCSRFToken
      val application = Application(appId, "app name")

      AuthConnectorMock.Authorise.thenReturn()
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturnWith(appId, declinedSubmission)

      val result = controller.page(appId, 1)(fakeRequest)
      status(result) shouldBe Status.BAD_REQUEST
    }

    "return 400 when given a submission that isn't declined" in new Setup {
      val appId = ApplicationId.random
      val fakeRequest = FakeRequest("GET", "/").withCSRFToken
      val application = Application(appId, "app name")

      AuthConnectorMock.Authorise.thenReturn()
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturnWith(appId, submittedSubmission)

      val result = controller.page(appId, 0)(fakeRequest)
      status(result) shouldBe Status.BAD_REQUEST
    }
  }
}
