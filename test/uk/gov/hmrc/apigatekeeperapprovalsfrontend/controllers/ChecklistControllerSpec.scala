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

import org.mockito.captor.ArgCaptor

import scala.concurrent.ExecutionContext.Implicits.global
import play.api.http.Status
import play.api.test.Helpers._
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.ChecklistController.ViewModel
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.SubmissionReview
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.views.html.ChecklistPage
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.services.SubscriptionServiceMockModule
import uk.gov.hmrc.apiplatform.modules.submissions.domain.models.MarkedSubmission

class ChecklistControllerSpec extends AbstractControllerSpec {
  trait Setup extends AbstractSetup with SubscriptionServiceMockModule {
    val appChecklistPage = mock[ChecklistPage]
    when(appChecklistPage.apply(*[ViewModel])(*,*)).thenReturn(play.twirl.api.HtmlFormat.empty)
    val viewModelCaptor = ArgCaptor[ViewModel]

    val controller = new ChecklistController(
      strideAuthConfig,
      AuthConnectorMock.aMock,
      forbiddenHandler,
      mcc,
      SubmissionReviewServiceMock.aMock,
      errorHandler,
      appChecklistPage,
      ApplicationActionServiceMock.aMock,
      SubmissionServiceMock.aMock
    )

    def setupForSuccessWith(markedSubmission: MarkedSubmission, requiresFraudCheck: Boolean = false, requiresDemo: Boolean = false) = {
      AuthConnectorMock.Authorise.thenReturn()
      ApplicationActionServiceMock.Process.thenReturn(application)

      val markedSubmissionWithContext = markedSubmission.copy(submission = markedSubmission.submission.copy(context = if (requiresFraudCheck) vatContext else simpleContext))
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(markedSubmissionWithContext)
      SubmissionReviewServiceMock.FindOrCreateReview.thenReturn(SubmissionReview(submissionId, 0, true, true, requiresFraudCheck, requiresDemo))
    }
  }

  "Checklist Page" should {
    "should display check warnings section if submission passed with warnings" in new Setup {
      setupForSuccessWith(warnMarkedSubmission)

      await(controller.checklistPage(applicationId)(fakeRequest))

      verify(appChecklistPage).apply(viewModelCaptor)(*, *)
      viewModelCaptor.value.sections.map(_.titleMsgId) shouldBe List(
        "checklist.checkwarnings.heading",
        "checklist.checkapplication.heading",
        "checklist.checkpassed.heading"
      )
    }
    "should display check failures section if submission failed" in new Setup {
      setupForSuccessWith(failMarkedSubmission)

      await(controller.checklistPage(applicationId)(fakeRequest))

      verify(appChecklistPage).apply(viewModelCaptor)(*, *)
      viewModelCaptor.value.sections.map(_.titleMsgId) shouldBe List(
        "checklist.checkfailed.heading",
        "checklist.checkapplication.heading",
        "checklist.checkpassed.heading"
      )
    }
    "should not display check failures/warnings section if submission passed without warnings" in new Setup {
      setupForSuccessWith(passMarkedSubmission)

      await(controller.checklistPage(applicationId)(fakeRequest))

      verify(appChecklistPage).apply(viewModelCaptor)(*, *)
      viewModelCaptor.value.sections.map(_.titleMsgId) shouldBe List(
        "checklist.checkapplication.heading",
        "checklist.checkpassed.heading"
      )
    }
    "should display the correct check application items if fraud check and demo not required" in new Setup {
      setupForSuccessWith(passMarkedSubmission, false, false)

      await(controller.checklistPage(applicationId)(fakeRequest))

      verify(appChecklistPage).apply(viewModelCaptor)(*, *)
      viewModelCaptor.value.sections(0).items.map(_.labelMsgId) shouldBe List(
        "checklist.checkapplication.linktext.email",
        "checklist.checkapplication.linktext.confirmverified",
        "checklist.checkapplication.linktext.name",
        "checklist.checkapplication.linktext.company",
        "checklist.checkapplication.linktext.urls",
        "checklist.checkapplication.linktext.sandbox"
      )
    }
    "should display the correct check application items if fraud check and demo are required" in new Setup {
      setupForSuccessWith(passMarkedSubmission, true, true)

      await(controller.checklistPage(applicationId)(fakeRequest))

      verify(appChecklistPage).apply(viewModelCaptor)(*, *)
      viewModelCaptor.value.sections(0).items.map(_.labelMsgId) shouldBe List(
        "checklist.checkapplication.linktext.email",
        "checklist.checkapplication.linktext.confirmverified",
        "checklist.checkapplication.linktext.name",
        "checklist.checkapplication.linktext.company",
        "checklist.checkapplication.linktext.urls",
        "checklist.checkapplication.linktext.sandbox",
        "checklist.checkapplication.linktext.fraud",
        "checklist.checkapplication.linktext.demo"
      )
    }
  }

  "GET /" should {
    "return 200" in new Setup {
      AuthConnectorMock.Authorise.thenReturn()
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(applicationId)
      SubmissionReviewServiceMock.FindOrCreateReview.thenReturn(submissionReview)

      val result = controller.checklistPage(applicationId)(fakeRequest)
      status(result) shouldBe Status.OK
    }

    "return 404 if no marked application is found" in new Setup {
      AuthConnectorMock.Authorise.thenReturn()
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenNotFound()

      val result = controller.checklistPage(applicationId)(fakeRequest)
      status(result) shouldBe Status.NOT_FOUND
    }

    "return 404 if no application is found" in new Setup {
      AuthConnectorMock.Authorise.thenReturn()
      ApplicationActionServiceMock.Process.thenNotFound()

      val result = controller.checklistPage(applicationId)(fakeRequest)
      status(result) shouldBe Status.NOT_FOUND
    }

    "return 403 for InsufficientEnrolments" in new Setup {
      AuthConnectorMock.Authorise.thenReturnInsufficientEnrolments()
      val result = controller.checklistPage(applicationId)(fakeRequest)
      status(result) shouldBe Status.FORBIDDEN
    }

    "return 303 for SessionRecordNotFound" in new Setup {
      AuthConnectorMock.Authorise.thenReturnSessionRecordNotFound()
      val result = controller.checklistPage(applicationId)(fakeRequest)
      status(result) shouldBe Status.SEE_OTHER
    }
  }

  "POST /" should {
    "return 200 and send to checks completed page if Checks Completed button is clicked" in new Setup {
      AuthConnectorMock.Authorise.thenReturn()
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(applicationId)

      val result = controller.checklistAction(applicationId)(fakeSubmitCheckedRequest)
      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some(s"/api-gatekeeper-approvals/applications/${applicationId.value}/confirm-decision")
    }

    "return 200 and send to submissions page if Save and Come Back Later button is clicked" in new Setup {
      AuthConnectorMock.Authorise.thenReturn()
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(applicationId)

      val result = controller.checklistAction(applicationId)(fakeSubmitComebackLaterRequest)
      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some(s"/api-gatekeeper-approvals/applications/${applicationId.value}/reviews")
    }

    "return 400 if bad submission action is received" in new Setup {
      AuthConnectorMock.Authorise.thenReturn()
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(applicationId)

      val result = controller.checklistAction(applicationId)(brokenRequest)
      status(result) shouldBe Status.BAD_REQUEST
    }
  }
}
