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

import play.api.http.Status.OK
import play.api.test.Helpers._
import uk.gov.hmrc.apiplatform.modules.gkauth.domain.models.GatekeeperRoles
import uk.gov.hmrc.apiplatform.modules.gkauth.services.{ApplicationServiceMockModule, LdapAuthorisationServiceMockModule, StrideAuthorisationServiceMockModule}

import uk.gov.hmrc.apigatekeeperapprovalsfrontend.views.html.TermsOfUsePage

class TermsOfUseInvitationControllerSpec
    extends AbstractControllerSpec {

  trait Setup extends AbstractSetup
      with StrideAuthorisationServiceMockModule
      with LdapAuthorisationServiceMockModule
      with ApplicationServiceMockModule {
    val termsOfUsePage = app.injector.instanceOf[TermsOfUsePage]

    val controller = new TermsOfUseInvitationController(
      StrideAuthorisationServiceMock.aMock,
      mcc,
      errorHandler,
      ApplicationActionServiceMock.aMock,
      SubmissionServiceMock.aMock,
      LdapAuthorisationServiceMock.aMock,
      termsOfUsePage,
      ApplicationServiceMock.aMock
    )
  }

  "GET /" should {
    "return Ok (200) for Stride users" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      SubmissionServiceMock.SearchTermsOfUseInvitations.thenReturn()

      val result = controller.page()(fakeRequest)

      status(result) shouldBe OK
    }

    "return Ok (200) for Stride users with passed in status filter" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      SubmissionServiceMock.SearchTermsOfUseInvitations.thenReturn()

      val request = fakeRequest.withCSRFToken.withFormUrlEncodedBody("emailSentStatus" -> "true", "overdueStatus" -> "true")

      val result = controller.page()(request)

      status(result) shouldBe OK
      SubmissionServiceMock.SearchTermsOfUseInvitations.verifyCalled(Seq("status" -> "EMAIL_SENT", "status" -> "OVERDUE"))
    }

    "return Unauthorised (401) when user not logged in" in new Setup {
      StrideAuthorisationServiceMock.Auth.invalidBearerToken()
      LdapAuthorisationServiceMock.Auth.notAuthorised

      val result = controller.page()(fakeRequest)

      status(result) shouldBe UNAUTHORIZED
    }
  }
}
