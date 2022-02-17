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

import scala.concurrent.ExecutionContext.Implicits.global

import play.api.http.Status
import play.api.test.Helpers._

import uk.gov.hmrc.apigatekeeperapprovalsfrontend.views.html.{ApplicationDeclinedPage, ProvideReasonsForDecliningPage}

class DeclinedJourneyControllerSpec extends AbstractControllerSpec {
  trait Setup extends AbstractSetup {
    val applicationDeclinedPage = app.injector.instanceOf[ApplicationDeclinedPage]
    val provideReasonsForDecliningPage = app.injector.instanceOf[ProvideReasonsForDecliningPage]
  
    val controller = new DeclinedJourneyController(
        strideAuthConfig,
        AuthConnectorMock.aMock,
        forbiddenHandler,
        mcc,
        errorHandler,
        ApplicationActionServiceMock.aMock,
        SubmissionServiceMock.aMock,
        applicationDeclinedPage,
        provideReasonsForDecliningPage
      )
  }

  "application declined provide reasons page" should {
    "return 200" in new Setup {
      AuthConnectorMock.Authorise.thenReturn()
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(applicationId)
    
      val result = controller.provideReasonsPage(applicationId)(fakeRequest)

      status(result) shouldBe Status.OK
    }

    "return 404 when no marked submission is found" in new Setup {
      AuthConnectorMock.Authorise.thenReturn()
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenNotFound()

      val result = controller.provideReasonsPage(applicationId)(fakeRequest)
      
      status(result) shouldBe Status.NOT_FOUND
    }
  }

  "application declined provide reasons action" should {
    "go to next page when valid form and decline works" in new Setup {
      val fakeDeclineRequest = fakeRequest.withFormUrlEncodedBody("reasons" -> "submission looks bad")

      AuthConnectorMock.Authorise.thenReturn()
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(applicationId)
      SubmissionServiceMock.Decline.thenReturn(applicationId, application)
    
      val result = controller.provideReasonsAction(applicationId)(fakeDeclineRequest)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result).value shouldBe uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.routes.DeclinedJourneyController.declinedPage(applicationId).url
    }

    "return bad request when valid form and decline fails" in new Setup {
      val fakeDeclineRequest = fakeRequest.withFormUrlEncodedBody("reasons" -> "submission looks bad")

      AuthConnectorMock.Authorise.thenReturn()
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(applicationId)
      SubmissionServiceMock.Decline.thenReturnError(applicationId)
    
      val result = controller.provideReasonsAction(applicationId)(fakeDeclineRequest)

      status(result) shouldBe BAD_REQUEST
    }

    "return bad request and go back to page when not a valid form" in new Setup {
      val brokenFormRequest = fakeRequest.withFormUrlEncodedBody()

      AuthConnectorMock.Authorise.thenReturn()
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(applicationId)

      val result = controller.provideReasonsAction(applicationId)(brokenFormRequest)

      status(result) shouldBe BAD_REQUEST
    }
  }
}
