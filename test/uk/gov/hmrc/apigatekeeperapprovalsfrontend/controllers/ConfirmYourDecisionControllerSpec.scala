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

import uk.gov.hmrc.apigatekeeperapprovalsfrontend.views.html.{ApplicationApprovedPage, ApplicationDeclinedPage, ConfirmYourDecisionPage}

class ConfirmYourDecisionControllerSpec extends AbstractControllerSpec {
  
  trait Setup extends AbstractSetup {
    val confirmYourDecisionPage = app.injector.instanceOf[ConfirmYourDecisionPage]
    val applicationApprovedPage = app.injector.instanceOf[ApplicationApprovedPage]
    val applicationDeclinedPage = app.injector.instanceOf[ApplicationDeclinedPage]
  
    val controller = new ConfirmYourDecisionController(
        strideAuthConfig,
        AuthConnectorMock.aMock,
        forbiddenHandler,
        mcc,
        errorHandler,
        ApplicationActionServiceMock.aMock,
        SubmissionServiceMock.aMock,
        confirmYourDecisionPage,
        applicationApprovedPage,
        applicationDeclinedPage
      )
  }

  "GET /" should {
    "return 200" in new Setup {
      AuthConnectorMock.Authorise.thenReturn()
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(appId)
    
      val result = controller.page(appId)(fakeRequest)

      status(result) shouldBe Status.OK
    }
  }
}
