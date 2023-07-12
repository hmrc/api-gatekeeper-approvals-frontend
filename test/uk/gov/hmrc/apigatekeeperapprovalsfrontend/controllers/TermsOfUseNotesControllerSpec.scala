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
import uk.gov.hmrc.apiplatform.modules.gkauth.domain.models.GatekeeperRoles
import uk.gov.hmrc.apiplatform.modules.gkauth.services.StrideAuthorisationServiceMockModule

import uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.TermsOfUseNotesController
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.views.html.{TermsOfUseGrantedConfirmationPage, TermsOfUseNotesPage}

class TermsOfUseNotesControllerSpec extends AbstractControllerSpec {

  trait Setup extends AbstractSetup with StrideAuthorisationServiceMockModule {
    val notesPage   = app.injector.instanceOf[TermsOfUseNotesPage]
    val confirmPage = app.injector.instanceOf[TermsOfUseGrantedConfirmationPage]

    val controller = new TermsOfUseNotesController(
      StrideAuthorisationServiceMock.aMock,
      mcc,
      errorHandler,
      ApplicationActionServiceMock.aMock,
      SubmissionServiceMock.aMock,
      notesPage,
      confirmPage
    )
  }

  "page" should {
    "return 200" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(applicationId)

      val result = controller.page(applicationId)(fakeRequest)

      status(result) shouldBe Status.OK
    }

    "return 404" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenNotFound()

      val result = controller.page(applicationId)(fakeRequest)

      status(result) shouldBe Status.NOT_FOUND
    }
  }

  "action" should {
    "return 200" in new Setup {
      val fakeReasonsRequest = fakeRequest.withFormUrlEncodedBody("notes" -> "submission looks bad")

      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(applicationId)
      SubmissionServiceMock.GrantForTouUplift.thenReturn(applicationId, application)

      val result = controller.action(applicationId)(fakeReasonsRequest)

      status(result) shouldBe Status.OK
    }

    "return 400 if no reasons supplied" in new Setup {
      val fakeReasonsRequest = fakeRequest.withFormUrlEncodedBody("notes" -> "")

      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(applicationId)

      val result = controller.action(applicationId)(fakeReasonsRequest)

      status(result) shouldBe Status.BAD_REQUEST
    }

    "return 400 if failed to save" in new Setup {
      val fakeReasonsRequest = fakeRequest.withFormUrlEncodedBody("reasons" -> "submission looks bad")

      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(applicationId)
      SubmissionServiceMock.GrantForTouUplift.thenReturnError(applicationId)

      val result = controller.action(applicationId)(fakeReasonsRequest)

      status(result) shouldBe Status.BAD_REQUEST
    }
  }
}
