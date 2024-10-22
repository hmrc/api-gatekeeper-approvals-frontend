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

import org.mockito.captor.ArgCaptor

import play.api.http.Status
import play.api.test.Helpers._

import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.ApplicationWithCollaborators
import uk.gov.hmrc.apiplatform.modules.gkauth.domain.models.GatekeeperRoles
import uk.gov.hmrc.apiplatform.modules.gkauth.services.StrideAuthorisationServiceMockModule
import uk.gov.hmrc.apiplatform.modules.submissions.domain.models.MarkedSubmission

import uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.ChecklistController.ViewModel
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.SubmissionReview
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.services.SubscriptionServiceMockModule
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.views.html.ChecklistPage

class ChecklistControllerSpec extends AbstractControllerSpec {
  import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.Implicits._

  trait BaseSetup extends AbstractSetup
      with SubscriptionServiceMockModule
      with StrideAuthorisationServiceMockModule {

    def appChecklistPage: ChecklistPage

    val viewModelCaptor = ArgCaptor[ViewModel]

    lazy val controller = new ChecklistController(
      StrideAuthorisationServiceMock.aMock,
      mcc,
      SubmissionReviewServiceMock.aMock,
      errorHandler,
      appChecklistPage,
      ApplicationActionServiceMock.aMock,
      SubmissionServiceMock.aMock
    )

    def setupForSuccessWith(
        markedSubmission: MarkedSubmission,
        requiresFraudCheck: Boolean = false,
        requiresDemo: Boolean = false,
        anApplication: ApplicationWithCollaborators = application
      ) = {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      ApplicationActionServiceMock.Process.thenReturn(anApplication)

      val markedSubmissionWithContext = markedSubmission.copy(submission = markedSubmission.submission.copy(context = if (requiresFraudCheck) vatContext else simpleContext))
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(markedSubmissionWithContext)
      SubmissionReviewServiceMock.FindOrCreateReview.thenReturn(SubmissionReview(submissionId, 0, true, true, requiresFraudCheck, requiresDemo))
    }
  }

  trait Setup extends BaseSetup {
    val appChecklistPage = mock[ChecklistPage]
    when(appChecklistPage.apply(*[ViewModel])(*, *)).thenReturn(play.twirl.api.HtmlFormat.empty)
  }

  trait LivePageSetup extends BaseSetup {
    val appChecklistPage = app.injector.instanceOf[ChecklistPage]
  }

  "Checklist Page" should {
    "display check warnings section if submission passed with warnings" in new Setup {
      setupForSuccessWith(warnMarkedSubmission)

      await(controller.checklistPage(applicationId)(fakeRequest))

      verify(appChecklistPage).apply(viewModelCaptor)(*, *)
      viewModelCaptor.value.sections.map(_.titleMsgId) shouldBe List(
        "checklist.checkwarnings.heading",
        "checklist.checkapplication.heading",
        "checklist.checkpassed.heading"
      )
    }
    "display check failures section if submission failed" in new Setup {
      setupForSuccessWith(failMarkedSubmission)

      await(controller.checklistPage(applicationId)(fakeRequest))

      verify(appChecklistPage).apply(viewModelCaptor)(*, *)
      viewModelCaptor.value.sections.map(_.titleMsgId) shouldBe List(
        "checklist.checkfailed.heading",
        "checklist.checkapplication.heading",
        "checklist.checkpassed.heading"
      )
    }
    "not display check failures/warnings section if submission passed without warnings" in new Setup {
      setupForSuccessWith(passMarkedSubmission)

      await(controller.checklistPage(applicationId)(fakeRequest))

      verify(appChecklistPage).apply(viewModelCaptor)(*, *)
      viewModelCaptor.value.sections.map(_.titleMsgId) shouldBe List(
        "checklist.checkapplication.heading",
        "checklist.checkpassed.heading"
      )
    }
    "display the correct check application items if fraud check and demo not required" in new Setup {
      setupForSuccessWith(passMarkedSubmission, false, false)

      await(controller.checklistPage(applicationId)(fakeRequest))

      verify(appChecklistPage).apply(viewModelCaptor)(*, *)
      viewModelCaptor.value.sections(0).items.map(_.labelMsgId) shouldBe List(
        "checklist.checkapplication.linktext.name",
        "checklist.checkapplication.linktext.company",
        "checklist.checkapplication.linktext.urls",
        "checklist.checkapplication.linktext.sandbox"
      )
    }

    "display the correct check application items if fraud check and demo are required" in new Setup {
      setupForSuccessWith(passMarkedSubmission, true, true)

      await(controller.checklistPage(applicationId)(fakeRequest))

      verify(appChecklistPage).apply(viewModelCaptor)(*, *)
      viewModelCaptor.value.sections(0).items.map(_.labelMsgId) shouldBe List(
        "checklist.checkapplication.linktext.name",
        "checklist.checkapplication.linktext.company",
        "checklist.checkapplication.linktext.urls",
        "checklist.checkapplication.linktext.sandbox",
        "checklist.checkapplication.linktext.fraud",
        "checklist.checkapplication.linktext.demo"
      )
    }

    "render checklist template with in house software section" in new LivePageSetup {
      setupForSuccessWith(passMarkedSubmission, true, true, inHouseApplication)
      inHouseApplication.isInHouseSoftware shouldBe true

      val result = controller.checklistPage(applicationId)(fakeRequest)

      status(result) shouldBe OK
      contentAsString(result) should include("This request is from an in-house developer")
      contentAsString(result) should not include ("This application has been deleted")
    }

    "render checklist template without in house software section" in new LivePageSetup {
      setupForSuccessWith(passMarkedSubmission, true, true, application)
      application.isInHouseSoftware shouldBe false

      val result = controller.checklistPage(applicationId)(fakeRequest)

      status(result) shouldBe OK
      contentAsString(result) should not include ("This request is from an in-house developer")
      contentAsString(result) should not include ("This application has been deleted")
    }

    "render checklist template with a deleted application" in new LivePageSetup {
      val deletedApplication = application.modifyState(_.toDeleted(instant).copy(requestedByName = Some("deletee@example.com")))
      setupForSuccessWith(passMarkedSubmission, true, true, deletedApplication)
      application.isInHouseSoftware shouldBe false

      val result = controller.checklistPage(applicationId)(fakeRequest)

      status(result) shouldBe OK
      contentAsString(result) should include("This application has been deleted")
    }
  }

  "GET /" should {
    "return 200" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(applicationId)
      SubmissionReviewServiceMock.FindOrCreateReview.thenReturn(submissionReview)

      val result = controller.checklistPage(applicationId)(fakeRequest)
      status(result) shouldBe Status.OK
    }

    "return 404 if no marked application is found" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenNotFound()

      val result = controller.checklistPage(applicationId)(fakeRequest)
      status(result) shouldBe Status.NOT_FOUND
    }

    "return 404 if no application is found" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      ApplicationActionServiceMock.Process.thenNotFound()

      val result = controller.checklistPage(applicationId)(fakeRequest)
      status(result) shouldBe Status.NOT_FOUND
    }

    "return 403 for InsufficientEnrolments" in new Setup {
      StrideAuthorisationServiceMock.Auth.hasInsufficientEnrolments()
      StrideAuthorisationServiceMock.Auth.hasInsufficientEnrolments()
      val result = controller.checklistPage(applicationId)(fakeRequest)
      status(result) shouldBe Status.FORBIDDEN
    }

    "return 303 for SessionRecordNotFound" in new Setup {
      StrideAuthorisationServiceMock.Auth.sessionRecordNotFound()
      val result = controller.checklistPage(applicationId)(fakeRequest)
      status(result) shouldBe Status.SEE_OTHER
    }
  }

  "POST /" should {
    "return 200 and send to checks completed page if Checks Completed button is clicked" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(applicationId)

      val result = controller.checklistAction(applicationId)(fakeSubmitCheckedRequest)
      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some(s"/api-gatekeeper-approvals/applications/${applicationId.value}/confirm-decision")
    }

    "return 200 and send to submissions page if Save and Come Back Later button is clicked" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(applicationId)

      val result = controller.checklistAction(applicationId)(fakeSubmitComebackLaterRequest)
      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some(s"/api-gatekeeper-approvals/applications/${applicationId.value}/reviews")
    }

    "return 400 if bad submission action is received" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(applicationId)

      val result = controller.checklistAction(applicationId)(brokenRequest)
      status(result) shouldBe Status.BAD_REQUEST
    }
  }
}
