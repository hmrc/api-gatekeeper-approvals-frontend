/*
 * Copyright 2023 HM Revenue & Customs
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

import uk.gov.hmrc.apigatekeeperapprovalsfrontend.utils.ApplicationTestData
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.views.html.ViewDeclinedSubmissionPage
import uk.gov.hmrc.apiplatform.modules.gkauth.services.StrideAuthorisationServiceMockModule
import uk.gov.hmrc.apiplatform.modules.gkauth.domain.models.GatekeeperRoles

class ViewDeclinedSubmissionControllerSpec extends AbstractControllerSpec {

  trait Setup
      extends AbstractSetup
      with ApplicationTestData
      with StrideAuthorisationServiceMockModule {

    val viewDeclinedSubmissionPage = app.injector.instanceOf[ViewDeclinedSubmissionPage]

    val controller = new ViewDeclinedSubmissionController(
      StrideAuthorisationServiceMock.aMock,
      mcc,
      viewDeclinedSubmissionPage,
      errorHandler,
      SubmissionReviewServiceMock.aMock,
      ApplicationActionServiceMock.aMock,
      SubmissionServiceMock.aMock
    )
  }

  "GET /" should {
    "return 200" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturnWith(applicationId, declinedSubmission)

      val result = controller.page(applicationId, 0)(fakeRequest)
      status(result) shouldBe Status.OK
    }

    "return 400 when given a submission index that doesn't exist" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturnWith(applicationId, declinedSubmission)

      val result = controller.page(applicationId, 1)(fakeRequest)
      status(result) shouldBe Status.BAD_REQUEST
    }

    "return 400 when given a submission that isn't declined" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturnWith(applicationId, submittedSubmission)

      val result = controller.page(applicationId, 0)(fakeRequest)
      status(result) shouldBe Status.BAD_REQUEST
    }
  }
}
