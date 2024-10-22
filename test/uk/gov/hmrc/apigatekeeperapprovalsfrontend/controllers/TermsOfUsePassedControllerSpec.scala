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
import uk.gov.hmrc.apiplatform.modules.submissions.services.SubmissionReviewServiceMockModule

import uk.gov.hmrc.apigatekeeperapprovalsfrontend.views.html.TermsOfUsePassedPage

class TermsOfUsePassedControllerSpec extends AbstractControllerSpec {

  trait Setup extends AbstractSetup with SubmissionReviewServiceMockModule
      with StrideAuthorisationServiceMockModule {
    val page = app.injector.instanceOf[TermsOfUsePassedPage]

    val controller = new TermsOfUsePassedController(
      StrideAuthorisationServiceMock.aMock,
      mcc,
      errorHandler,
      SubmissionReviewServiceMock.aMock,
      page,
      ApplicationActionServiceMock.aMock,
      SubmissionServiceMock.aMock
    )
  }

  "answersThatPassedPage" should {
    "return 200" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(applicationId)

      val result = controller.answersThatPassedPage(applicationId)(fakeRequest)

      status(result) shouldBe Status.OK
      contentAsString(result) should not include ("This application has been deleted")
    }

    "return 200 if unknown questions exist" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturnIncludingAnUnknownQuestion(applicationId)

      val result = controller.answersThatPassedPage(applicationId)(fakeRequest)

      status(result) shouldBe Status.OK
    }

    "return 200 with a deleted application" in new Setup {
      val deletedApp = application.modifyState(_.toDeleted(instant).copy(requestedByName = Some("delete-user@example.com")))
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      ApplicationActionServiceMock.Process.thenReturn(deletedApp)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(applicationId)

      val result = controller.answersThatPassedPage(applicationId)(fakeRequest)

      status(result) shouldBe Status.OK
      contentAsString(result) should include("This application has been deleted")
    }

    "return 404" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenNotFound()

      val result = controller.answersThatPassedPage(applicationId)(fakeRequest)

      status(result) shouldBe Status.NOT_FOUND
    }
  }
}
