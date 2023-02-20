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

import uk.gov.hmrc.apiplatform.modules.gkauth.services.{LdapAuthorisationServiceMockModule, StrideAuthorisationServiceMockModule}
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.views.html.TermsOfUsePage
import uk.gov.hmrc.apiplatform.modules.gkauth.services.ApplicationServiceMockModule
import uk.gov.hmrc.apiplatform.modules.gkauth.domain.models.GatekeeperRoles

import play.api.http.Status.OK
import play.api.test.Helpers._

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
    "return Ok (200)" in new Setup {
      // StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      // ApplicationServiceMock.FetchByApplicationId.thenReturn(applicationId)
      // SubmissionServiceMock.FetchLatestSubmission.thenReturn(applicationId)

      // val result = controller.page()(fakeRequest)

      // status(result) shouldBe OK
    }
  }
}
