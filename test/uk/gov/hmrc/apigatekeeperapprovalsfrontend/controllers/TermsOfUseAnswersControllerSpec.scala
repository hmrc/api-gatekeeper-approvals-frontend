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
import uk.gov.hmrc.apiplatform.modules.gkauth.services.{LdapAuthorisationServiceMockModule, StrideAuthorisationServiceMockModule}

import uk.gov.hmrc.apigatekeeperapprovalsfrontend.views.html.TermsOfUseAnswersPage

class TermsOfUseAnswersControllerSpec extends AbstractControllerSpec {

  trait Setup
      extends AbstractSetup
      with LdapAuthorisationServiceMockModule
      with StrideAuthorisationServiceMockModule {
    val termsOfUseAnswersPage = app.injector.instanceOf[TermsOfUseAnswersPage]

    val controller = new TermsOfUseAnswersController(
      LdapAuthorisationServiceMock.aMock,
      StrideAuthorisationServiceMock.aMock,
      mcc,
      errorHandler,
      termsOfUseAnswersPage,
      ApplicationActionServiceMock.aMock,
      SubmissionServiceMock.aMock
    )
  }

  "GET /" should {
    "return 200" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(applicationId)

      val result = controller.page(applicationId)(fakeRequest)
      status(result) shouldBe Status.OK
    }

    "return 200 for LDAP user" in new Setup {
      StrideAuthorisationServiceMock.Auth.invalidBearerToken()
      LdapAuthorisationServiceMock.Auth.succeeds
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(applicationId)

      val result = controller.page(applicationId)(fakeRequest)
      status(result) shouldBe Status.OK
    }

    "return 404 when given an application that doesn't exist" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      ApplicationActionServiceMock.Process.thenNotFound()

      val result = controller.page(applicationId)(fakeRequest)
      status(result) shouldBe Status.NOT_FOUND
    }
  }
}
