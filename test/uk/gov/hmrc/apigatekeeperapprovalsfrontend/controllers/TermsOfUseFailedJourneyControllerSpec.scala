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

import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.ApplicationState
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.views.html.TermsOfUseFailedListPage

class TermsOfUseFailedJourneyControllerSpec extends AbstractControllerSpec {

  trait Setup extends AbstractSetup with SubmissionReviewServiceMockModule
      with StrideAuthorisationServiceMockModule {
    val page = app.injector.instanceOf[TermsOfUseFailedListPage]

    val controller = new TermsOfUseFailedJourneyController(
      StrideAuthorisationServiceMock.aMock,
      mcc,
      errorHandler,
      SubmissionReviewServiceMock.aMock,
      page,
      ApplicationActionServiceMock.aMock,
      SubmissionServiceMock.aMock
    )
  }

  "termsOfUseFailedListPage" should {
    "return 200" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(applicationId)

      val result = controller.listPage(applicationId)(fakeRequest)

      status(result) shouldBe Status.OK
    }

    "return 200 if unknown questions exist" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturnIncludingAnUnknownQuestion(applicationId)

      val result = controller.listPage(applicationId)(fakeRequest)

      status(result) shouldBe Status.OK
    }

    "return 404" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenNotFound()

      val result = controller.listPage(applicationId)(fakeRequest)

      status(result) shouldBe Status.NOT_FOUND
    }
  }

  // "checkAnswersThatFailedAction" should {
  //   "redirect to correct page when marking answers as checked" in new Setup {
  //     StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
  //     ApplicationActionServiceMock.Process.thenReturn(application)
  //     SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(applicationId)
  //     SubmissionReviewServiceMock.UpdateActionStatus.thenReturn(submissionReview)

  //     val result = controller.listPage(applicationId)(fakeSubmitCheckedRequest)

  //     status(result) shouldBe SEE_OTHER
  //   }

  //   "redirect to correct page when marking answers as come-back-later" in new Setup {
  //     StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
  //     ApplicationActionServiceMock.Process.thenReturn(application)
  //     SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(applicationId)
  //     SubmissionReviewServiceMock.UpdateActionStatus.thenReturn(submissionReview)

  //     val result = controller.action(applicationId)(fakeSubmitComebackLaterRequest)

  //     status(result) shouldBe SEE_OTHER
  //   }

  //   "return bad request when marking answers as anything that we don't understand" in new Setup {
  //     StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
  //     ApplicationActionServiceMock.Process.thenReturn(application)
  //     SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(applicationId)

  //     val result = controller.action(applicationId)(brokenRequest)

  //     status(result) shouldBe BAD_REQUEST
  //   }

  //   "return bad request when sending an empty submit-action" in new Setup {
  //     StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
  //     ApplicationActionServiceMock.Process.thenReturn(application)
  //     SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(applicationId)

  //     val result = controller.action(applicationId)(fakeRequest)

  //     status(result) shouldBe BAD_REQUEST
  //   }
  // }
}