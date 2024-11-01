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

import uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.TermsOfUseDeleteController
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.views.html.{TermsOfUseDeleteConfirmationPage, TermsOfUseDeletePage}

class TermsOfUseDeleteControllerSpec extends AbstractControllerSpec {

  trait Setup extends AbstractSetup with StrideAuthorisationServiceMockModule {
    val termsOfUseDeletePage             = app.injector.instanceOf[TermsOfUseDeletePage]
    val termsOfUseDeleteConfirmationPage = app.injector.instanceOf[TermsOfUseDeleteConfirmationPage]

    val controller = new TermsOfUseDeleteController(
      StrideAuthorisationServiceMock.aMock,
      mcc,
      errorHandler,
      SubmissionReviewServiceMock.aMock,
      termsOfUseDeletePage,
      termsOfUseDeleteConfirmationPage,
      ApplicationActionServiceMock.aMock,
      SubmissionServiceMock.aMock
    )
  }

  "page" should {
    "return 200" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)
      ApplicationActionServiceMock.Process.thenReturn(application)

      val result = controller.page(applicationId)(fakeRequest)

      status(result) shouldBe Status.OK
    }
  }

  "action" should {
    "return 303" in new Setup {
      val fakeYesNoRequest = fakeRequest.withFormUrlEncodedBody("tou-delete" -> "yes")

      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.DeleteTouUplift.thenReturn(applicationId, application)

      val result = controller.action(applicationId)(fakeYesNoRequest)

      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result).value shouldBe uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.routes.TermsOfUseDeleteController.confirmationPage(applicationId).url
    }

    "redirect back to main ToU page if select no" in new Setup {
      val fakeYesNoRequest = fakeRequest.withFormUrlEncodedBody("tou-delete" -> "no")

      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      ApplicationActionServiceMock.Process.thenReturn(application)

      val result = controller.action(applicationId)(fakeYesNoRequest)

      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result).value shouldBe uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.routes.TermsOfUseInvitationController.page.url
    }

    "redirect back to main ToU page if select nothing" in new Setup {
      val fakeYesNoRequest = fakeRequest.withFormUrlEncodedBody("tou-delete" -> "")

      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      ApplicationActionServiceMock.Process.thenReturn(application)

      val result = controller.action(applicationId)(fakeYesNoRequest)

      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result).value shouldBe uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.routes.TermsOfUseInvitationController.page.url
    }

    "return 400 if failed to delete" in new Setup {
      val fakeYesNoRequest = fakeRequest.withFormUrlEncodedBody("tou-delete" -> "yes")

      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.DeleteTouUplift.thenReturnError(applicationId)

      val result = controller.action(applicationId)(fakeYesNoRequest)

      status(result) shouldBe Status.BAD_REQUEST
    }
  }
}
