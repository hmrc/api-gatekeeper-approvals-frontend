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

import uk.gov.hmrc.apigatekeeperapprovalsfrontend.views.html.TermsOfUseHistoryPage

class TermsOfUseHistoryControllerSpec
    extends AbstractControllerSpec {

  trait Setup extends AbstractSetup
      with StrideAuthorisationServiceMockModule
      with LdapAuthorisationServiceMockModule
      with ApplicationServiceMockModule {
    val termsOfUsePage = app.injector.instanceOf[TermsOfUseHistoryPage]

    val controller = new TermsOfUseHistoryController(
      config,
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
    "return Ok (200) for Stride users with an application and a submitted submission" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchTermsOfUseInvitation.thenReturn(applicationId)
      SubmissionServiceMock.FetchLatestSubmission.thenReturnHasBeenSubmitted(applicationId)

      val result = controller.page(applicationId)(fakeRequest)

      status(result) shouldBe OK
    }

    "return Ok (200) for Stride users with an application and a created submission" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchTermsOfUseInvitation.thenReturn(applicationId)
      SubmissionServiceMock.FetchLatestSubmission.thenReturn(applicationId)

      val result = controller.page(applicationId)(fakeRequest)

      status(result) shouldBe OK
    }

    "return Ok (200) for Stride users with an application and a granted submission" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchTermsOfUseInvitation.thenReturn(applicationId)
      SubmissionServiceMock.FetchLatestSubmission.thenReturnHasBeenGranted(applicationId)

      val result = controller.page(applicationId)(fakeRequest)

      status(result) shouldBe OK
      contentAsString(result) should not include ("This request is from an in-house developer")
    }

    "return Ok (200) for Stride users with an application and a granted submission with in-house developer context" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchTermsOfUseInvitation.thenReturn(applicationId)
      SubmissionServiceMock.FetchLatestSubmission.thenReturnHasBeenGrantedWithInHouseDeveloper(applicationId)

      val result = controller.page(applicationId)(fakeRequest)

      status(result) shouldBe OK
      contentAsString(result) should include("This request is from an in-house developer")
    }

    "return Ok (200) for Stride users with an application but no submission" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchTermsOfUseInvitation.thenReturn(applicationId)
      SubmissionServiceMock.FetchLatestSubmission.thenNotFound()

      val result = controller.page(applicationId)(fakeRequest)

      status(result) shouldBe OK
    }

    "return Ok (200) for LDAP users with an application and a submitted submission" in new Setup {
      StrideAuthorisationServiceMock.Auth.invalidBearerToken()
      LdapAuthorisationServiceMock.Auth.succeeds
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchTermsOfUseInvitation.thenReturn(applicationId)
      SubmissionServiceMock.FetchLatestSubmission.thenReturnHasBeenSubmitted(applicationId)

      val result = controller.page(applicationId)(fakeRequest)

      status(result) shouldBe OK
    }

    "return Not Found (404) when no application found for application id in invitations" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      ApplicationActionServiceMock.Process.thenNotFound()
      val result = controller.page(applicationId)(fakeRequest)

      status(result) shouldBe NOT_FOUND
    }
  }
}
