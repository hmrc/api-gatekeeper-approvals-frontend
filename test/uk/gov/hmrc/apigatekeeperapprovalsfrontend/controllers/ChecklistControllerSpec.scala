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
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.views.html.ChecklistPage
import uk.gov.hmrc.apiplatform.modules.submissions.services.SubmissionReviewServiceMockModule
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.SubmissionReview
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.utils.WithCSRFAddToken


class ChecklistControllerSpec extends AsyncHmrcSpec with GuiceOneAppPerSuite with WithCSRFAddToken {
  override def fakeApplication() =
    new GuiceApplicationBuilder()
      .configure(
        "metrics.jvm"     -> false,
        "metrics.enabled" -> false
      )
      .build()

  private val fakeRequest = FakeRequest("GET", "/")

  trait Setup 
      extends AuthConnectorMockModule
      with ApplicationActionServiceMockModule 
      with SubmissionServiceMockModule
      with SubmissionReviewServiceMockModule {
        
    implicit val appConfig = app.injector.instanceOf[AppConfig]

    val strideAuthConfig = app.injector.instanceOf[StrideAuthConfig]
    val forbiddenHandler = app.injector.instanceOf[HandleForbiddenWithView]
    val mcc = app.injector.instanceOf[MessagesControllerComponents]
    val appChecklistPage = app.injector.instanceOf[ChecklistPage]
    val errorHandler = app.injector.instanceOf[ErrorHandler]

    val controller = new ChecklistController(
      strideAuthConfig,
      AuthConnectorMock.aMock,
      forbiddenHandler,
      mcc,
      appChecklistPage,
      errorHandler,
      ApplicationActionServiceMock.aMock,
      SubmissionServiceMock.aMock,
      SubmissionReviewServiceMock.aMock
    )
  }

  "GET /" should {
    "return 200" in new Setup {
      val appId = ApplicationId.random
      val application = Application(appId, "app name")
      val submissionReview = SubmissionReview(markedSubmission.submission.id, 0)
      val fakeRequest = FakeRequest().withCSRFToken

      AuthConnectorMock.Authorise.thenReturn()
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(appId)
      SubmissionReviewServiceMock.FindOrCreateReview.thenReturn(submissionReview)

      val result = controller.checklistPage(appId)(fakeRequest)
      status(result) shouldBe Status.OK
    }

    "return 404 if no marked application is found" in new Setup {
      val appId = ApplicationId.random
      val application = Application(appId, "app name")

      val fakeRequest = FakeRequest().withCSRFToken

      AuthConnectorMock.Authorise.thenReturn()
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenNotFound()

      val result = controller.checklistPage(appId)(fakeRequest)
      status(result) shouldBe Status.NOT_FOUND
    }

    "return 404 if no application is found" in new Setup {
      val appId = ApplicationId.random

      val fakeRequest = FakeRequest().withCSRFToken

      AuthConnectorMock.Authorise.thenReturn()
      ApplicationActionServiceMock.Process.thenNotFound()

      val result = controller.checklistPage(appId)(fakeRequest)
      status(result) shouldBe Status.NOT_FOUND
    }

    "return 403 for InsufficientEnrolments" in new Setup {
      AuthConnectorMock.Authorise.thenReturnInsufficientEnrolments()
      val appId = ApplicationId.random
      val result = controller.checklistPage(appId)(fakeRequest)
      status(result) shouldBe Status.FORBIDDEN
    }
    
    "return 303 for SessionRecordNotFound" in new Setup {
      AuthConnectorMock.Authorise.thenReturnSessionRecordNotFound()
      val appId = ApplicationId.random
      val result = controller.checklistPage(appId)(fakeRequest)
      status(result) shouldBe Status.SEE_OTHER
    }  
  }

  "POST /" should {
    "return 200" in new Setup {
      val appId = ApplicationId.random
      val application = Application(appId, "app name")
      val fakeRequest = FakeRequest().withCSRFToken
            .withFormUrlEncodedBody("submit-action" -> "checked")

      AuthConnectorMock.Authorise.thenReturn()
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(appId)

      val result = controller.checklistAction(appId)(fakeRequest)
      status(result) shouldBe Status.SEE_OTHER
    }
  }
}
