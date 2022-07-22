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

import play.api.http.Status
import play.api.test.Helpers._

import scala.concurrent.ExecutionContext.Implicits.global
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.views.html.SubmittedAnswersPage
import uk.gov.hmrc.apiplatform.modules.gkauth.services.StrideAuthorisationServiceMockModule
import uk.gov.hmrc.apiplatform.modules.gkauth.domain.models.GatekeeperRoles
import uk.gov.hmrc.apiplatform.modules.gkauth.services.LdapAuthorisationServiceMockModule

class SubmittedAnswersControllerSpec extends AbstractControllerSpec {

  trait Setup
      extends AbstractSetup
      with LdapAuthorisationServiceMockModule
      with StrideAuthorisationServiceMockModule {
    val submittedAnswersPage = app.injector.instanceOf[SubmittedAnswersPage]

    val controller = new SubmittedAnswersController(
      LdapAuthorisationServiceMock.aMock,
      StrideAuthorisationServiceMock.aMock,
      mcc,
      errorHandler,
      submittedAnswersPage,
      ApplicationActionServiceMock.aMock,
      SubmissionServiceMock.aMock
    )
  }

  "GET /" should {
    "return 200" in new Setup {
      LdapAuthorisationServiceMock.Auth.notAuthorised
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(applicationId)

      val result = controller.page(applicationId, 0)(fakeRequest)
      status(result) shouldBe Status.OK
    }

    "return 200 for LDAP user" in new Setup {
      LdapAuthorisationServiceMock.Auth.succeeds
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(applicationId)

      val result = controller.page(applicationId, 0)(fakeRequest)
      status(result) shouldBe Status.OK
    }

    "return 400 when given a submission index that doesn't exist" in new Setup {
      LdapAuthorisationServiceMock.Auth.notAuthorised
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(applicationId)

      val result = controller.page(applicationId, 1)(fakeRequest)
      status(result) shouldBe Status.BAD_REQUEST
    }

    "return 404 when given an application that doesn't exist" in new Setup {
      LdapAuthorisationServiceMock.Auth.notAuthorised
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      ApplicationActionServiceMock.Process.thenNotFound()

      val result = controller.page(applicationId, 0)(fakeRequest)
      status(result) shouldBe Status.NOT_FOUND
    }
  }
}
