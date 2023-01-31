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
import uk.gov.hmrc.apiplatform.modules.gkauth.services.{ApplicationServiceMockModule, StrideAuthorisationServiceMockModule}

import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.SubmissionReview
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.views.html.{ApplicationApprovedPage, ApplicationDeclinedPage, ConfirmYourDecisionPage}

class ConfirmYourDecisionControllerSpec extends AbstractControllerSpec {

  trait Setup extends AbstractSetup with ApplicationServiceMockModule
      with StrideAuthorisationServiceMockModule {
    val confirmYourDecisionPage = app.injector.instanceOf[ConfirmYourDecisionPage]
    val applicationApprovedPage = app.injector.instanceOf[ApplicationApprovedPage]
    val applicationDeclinedPage = app.injector.instanceOf[ApplicationDeclinedPage]

    val controller = new ConfirmYourDecisionController(
      StrideAuthorisationServiceMock.aMock,
      mcc,
      errorHandler,
      ApplicationActionServiceMock.aMock,
      SubmissionServiceMock.aMock,
      confirmYourDecisionPage,
      SubmissionReviewServiceMock.aMock
    )
  }

  "confirmYourDecision page" should {
    "return 200 when marked submission is found" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturnWith(applicationId, passMarkedSubmission)

      val result = controller.page(applicationId)(fakeRequest)

      status(result) shouldBe Status.OK
      contentAsString(result) should include("Grant production access")
    }

    "return 200 with no grant when marked submission has fails" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(applicationId)

      val result = controller.page(applicationId)(fakeRequest)

      status(result) shouldBe Status.OK
      contentAsString(result) should include("Grant production access and email with warnings")
    }

    "return 404 when no marked submission is found" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenNotFound()

      val result = controller.page(applicationId)(fakeRequest)

      status(result) shouldBe Status.NOT_FOUND
    }
  }

  "confirmYourDecision action" should {
    "redirect to correct page when grant decision is decline" in new Setup {
      val fakeDeclineRequest = fakeRequest.withFormUrlEncodedBody("grant-decision" -> "decline")

      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(applicationId)

      val result = controller.action(applicationId)(fakeDeclineRequest)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result).value shouldBe uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.routes.DeclinedJourneyController.provideReasonsPage(applicationId).url
    }

    "redirect to correct page when grant decision is grant without warnings" in new Setup {
      val fakeDeclineRequest = fakeRequest.withFormUrlEncodedBody("grant-decision" -> "grant")

      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(applicationId)
      SubmissionServiceMock.Grant.thenReturn(applicationId, application)
      SubmissionReviewServiceMock.FindReview.thenReturn(SubmissionReview(submissionId, 0, true, false, false, false))

      val result = controller.action(applicationId)(fakeDeclineRequest)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result).value shouldBe uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.routes.GrantedJourneyController.grantedPage(applicationId).url
    }

    "return redirect back to page when grant decision is something we don't understand" in new Setup {
      val brokenDeclineRequest = fakeRequest.withFormUrlEncodedBody("grant-decision" -> "bobbins")

      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(applicationId)

      val result = controller.action(applicationId)(brokenDeclineRequest)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result).value shouldBe uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.routes.ConfirmYourDecisionController.page(applicationId).url
    }
  }
}
