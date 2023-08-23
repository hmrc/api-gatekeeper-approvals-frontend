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
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.views.html._

class TermsOfUseFailedJourneyControllerSpec extends AbstractControllerSpec {

  trait Setup extends AbstractSetup with SubmissionReviewServiceMockModule
      with StrideAuthorisationServiceMockModule {
    val listPage         = app.injector.instanceOf[TermsOfUseFailedListPage]
    val failedPage       = app.injector.instanceOf[TermsOfUseFailedPage]
    val adminsPage       = app.injector.instanceOf[TermsOfUseAdminsPage]
    val confirmationPage = app.injector.instanceOf[TermsOfUseConfirmationPage]
    val failOverridePage = app.injector.instanceOf[TermsOfUseFailOverridePage]
    val approverPage     = app.injector.instanceOf[TermsOfUseOverrideApproverPage]
    val notesPage        = app.injector.instanceOf[TermsOfUseOverrideNotesPage]
    val confirmPage      = app.injector.instanceOf[TermsOfUseOverrideConfirmPage]

    val controller = new TermsOfUseFailedJourneyController(
      StrideAuthorisationServiceMock.aMock,
      mcc,
      errorHandler,
      SubmissionReviewServiceMock.aMock,
      listPage,
      failedPage,
      adminsPage,
      confirmationPage,
      failOverridePage,
      approverPage,
      notesPage,
      confirmPage,
      ApplicationActionServiceMock.aMock,
      SubmissionServiceMock.aMock
    )
  }

  "termsOfUseFailedListPage" should {
    "return 200" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(applicationId)
      SubmissionReviewServiceMock.FindOrCreateReview.thenReturn(submissionReview)

      val result = controller.listPage(applicationId)(fakeRequest)

      status(result) shouldBe Status.OK
      contentAsString(result) should not include ("This application has been deleted")
    }

    "return 200 with a deleted application" in new Setup {
      val deletedApp = application.copy(state = ApplicationState.deleted("delete-user@example.com"))
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      ApplicationActionServiceMock.Process.thenReturn(deletedApp)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(applicationId)
      SubmissionReviewServiceMock.FindOrCreateReview.thenReturn(submissionReview)

      val result = controller.listPage(applicationId)(fakeRequest)

      status(result) shouldBe Status.OK
      contentAsString(result) should include("This application has been deleted")
    }

    "return 200 if unknown questions exist" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturnIncludingAnUnknownQuestion(applicationId)
      SubmissionReviewServiceMock.FindOrCreateReview.thenReturn(submissionReview)

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

  "checkAnswersThatFailedAction" should {
    "redirect to correct page when marking answers as continue" in new Setup {
      val fakeSubmitContinueRequest = fakeRequest.withFormUrlEncodedBody("submit-action" -> "continue")
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(applicationId)

      val result = controller.listAction(applicationId)(fakeSubmitContinueRequest)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result).value shouldBe uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.routes.TermsOfUseReasonsController.provideReasonsPage(applicationId).url
    }

    "redirect to terms of use page when marking answers as come back later" in new Setup {
      val fakeSubmitComeBackLaterRequest = fakeRequest.withFormUrlEncodedBody("submit-action" -> "come-back-later")
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(applicationId)

      val result = controller.listAction(applicationId)(fakeSubmitComeBackLaterRequest)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result).value shouldBe uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.routes.TermsOfUseInvitationController.page.url
    }
  }

  "answersWithWarningsOrFails" should {
    "return 200" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(applicationId)

      val result = controller.answersWithWarningsOrFails(applicationId)(fakeRequest)

      status(result) shouldBe Status.OK
    }
  }

  "emailAddressesPage" should {
    "return 200" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(applicationId)

      val result = controller.emailAddressesPage(applicationId)(fakeRequest)

      status(result) shouldBe Status.OK
    }
  }

  "emailAddressesAction" should {
    "return 200" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(applicationId)
      SubmissionReviewServiceMock.FindOrCreateReview.thenReturn(submissionReview)
      SubmissionServiceMock.GrantWithWarningsOrDeclineForTouUplift.thenReturn(applicationId, application)

      val result = controller.emailAddressesAction(applicationId)(fakeRequest)

      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result).value shouldBe uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.routes.TermsOfUseFailedJourneyController.confirmationPage(applicationId).url
    }
  }

  "failOverridePage" should {
    "return 200" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(applicationId)

      val result = controller.failOverridePage(applicationId)(fakeRequest)

      status(result) shouldBe Status.OK
    }
  }

  "failOverrideYesAction" should {
    "return 200" in new Setup {
      val fakeSubmitOverrideYesRequest = fakeRequest.withFormUrlEncodedBody("override" -> "yes")
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(applicationId)

      val result = controller.failOverrideAction(applicationId)(fakeSubmitOverrideYesRequest)

      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result).value shouldBe uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.routes.TermsOfUseFailedJourneyController.overrideApproverPage(
        applicationId
      ).url
    }
  }

  "failOverrideNoAction" should {
    "return 200" in new Setup {
      val fakeSubmitOverrideYesRequest = fakeRequest.withFormUrlEncodedBody("override" -> "no")
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(applicationId)

      val result = controller.failOverrideAction(applicationId)(fakeSubmitOverrideYesRequest)

      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result).value shouldBe uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.routes.TermsOfUseFailedJourneyController.listPage(applicationId).url
    }
  }

  "overrideApproverPage" should {
    "return 200" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(applicationId)

      val result = controller.overrideApproverPage(applicationId)(fakeRequest)

      status(result) shouldBe Status.OK
    }
  }

  "overrideApproverAction" should {
    "return 200" in new Setup {
      val fakeSubmitApproverRequest = fakeRequest.withFormUrlEncodedBody("first-name" -> "Bob", "last-name" -> "Mortimer")
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(applicationId)
      SubmissionReviewServiceMock.UpdateEscalatedTo.thenReturn(submissionReview)

      val result = controller.overrideApproverAction(applicationId)(fakeSubmitApproverRequest)

      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result).value shouldBe uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.routes.TermsOfUseFailedJourneyController.overrideNotesPage(applicationId).url
    }
  }

  "overrideNotesPage" should {
    "return 200" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(applicationId)

      val result = controller.overrideNotesPage(applicationId)(fakeRequest)

      status(result) shouldBe Status.OK
    }
  }

  "overrideNotesAction" should {
    "return 200" in new Setup {
      val fakeSubmitApproverRequest = fakeRequest.withFormUrlEncodedBody("notes" -> "Reasons to be cheerful")
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(applicationId)
      SubmissionReviewServiceMock.UpdateGrantWarnings.thenReturn(submissionReview)

      val result = controller.overrideNotesAction(applicationId)(fakeSubmitApproverRequest)

      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result).value shouldBe uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.routes.TermsOfUseFailedJourneyController.overrideConfirmPage(applicationId).url
    }
  }

  "overrideConfirmPage" should {
    "return 200" in new Setup {
      val review = submissionReview.copy(escalatedTo = Some("Mr Bob"))
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(applicationId)
      SubmissionReviewServiceMock.FindReview.thenReturn(review)

      val result = controller.overrideConfirmPage(applicationId)(fakeRequest)

      status(result) shouldBe Status.OK
    }

    "return 400 if no submission review" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(applicationId)
      SubmissionReviewServiceMock.FindReview.thenReturnNone()

      val result = controller.overrideConfirmPage(applicationId)(fakeRequest)

      status(result) shouldBe Status.BAD_REQUEST
    }

    "return 400 if escalated to not set" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(applicationId)
      SubmissionReviewServiceMock.FindReview.thenReturn(submissionReview)

      val result = controller.overrideConfirmPage(applicationId)(fakeRequest)

      status(result) shouldBe Status.BAD_REQUEST
    }
  }

  "overrideConfirmAction" should {
    "return 200" in new Setup {
      val review = submissionReview.copy(escalatedTo = Some("Mr Bob"))
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(applicationId)
      SubmissionReviewServiceMock.FindReview.thenReturn(review)
      SubmissionServiceMock.GrantForTouUplift.thenReturn(applicationId, application)

      val result = controller.overrideConfirmAction(applicationId)(fakeRequest)

      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result).value shouldBe uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.routes.TermsOfUseGrantedConfirmationController.page(applicationId).url
    }
  }

  "confirmationPage" should {
    "return 200" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(applicationId)

      val result = controller.confirmationPage(applicationId)(fakeRequest)

      status(result) shouldBe Status.OK
    }
  }
}
