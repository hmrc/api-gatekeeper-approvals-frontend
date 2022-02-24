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

import uk.gov.hmrc.apigatekeeperapprovalsfrontend.views.html.{ApplicationApprovedPage, ProvideWarningsForGrantingPage}

class GrantedJourneyControllerSpec extends AbstractControllerSpec {
  
  trait Setup extends AbstractSetup {
    val applicationApprovedPage = app.injector.instanceOf[ApplicationApprovedPage]
    val provideWarningsForGrantingPage = app.injector.instanceOf[ProvideWarningsForGrantingPage]

    val controller = new GrantedJourneyController(
        strideAuthConfig,
        AuthConnectorMock.aMock,
        forbiddenHandler,
        mcc,
        errorHandler,
        ApplicationActionServiceMock.aMock,
        SubmissionServiceMock.aMock,
        SubmissionReviewServiceMock.aMock,
        provideWarningsForGrantingPage,
        applicationApprovedPage
      )
  }

  "grantedPage page" should {
    "return 200 when marked submission is found" in new Setup {
      AuthConnectorMock.Authorise.thenReturn()
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturnWith(applicationId, passMarkedSubmission)
    
      val result = controller.grantedPage(applicationId)(fakeRequest)

      status(result) shouldBe Status.OK
    }

    "return 404 when no marked submission is found" in new Setup {
      AuthConnectorMock.Authorise.thenReturn()
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenNotFound()

      val result = controller.grantedPage(applicationId)(fakeRequest)
      
      status(result) shouldBe Status.NOT_FOUND
    }
  }
}
