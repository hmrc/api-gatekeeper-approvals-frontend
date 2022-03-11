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

import scala.concurrent.ExecutionContext.Implicits.global

import play.api.http.Status
import play.api.test.Helpers._
import uk.gov.hmrc.apiplatform.modules.submissions.domain.models.MarkedSubmission
import uk.gov.hmrc.apiplatform.modules.submissions.services.SubmissionReviewServiceMockModule

import uk.gov.hmrc.apigatekeeperapprovalsfrontend.views.html.ConfirmResponsibleIndividualVerified1Page
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.views.html.ConfirmResponsibleIndividualVerified2Page
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.SubmissionReview

class ConfirmResponsibleIndividualVerifiedControllerSpec extends AbstractControllerSpec {

  trait Setup extends AbstractSetup with SubmissionReviewServiceMockModule {
    val page1 = app.injector.instanceOf[ConfirmResponsibleIndividualVerified1Page]
    val page2 = app.injector.instanceOf[ConfirmResponsibleIndividualVerified2Page]

    val controller = new ConfirmResponsibleIndividualVerifiedController(
      strideAuthConfig,
      AuthConnectorMock.aMock,
      forbiddenHandler,
      mcc,
      errorHandler,
      SubmissionReviewServiceMock.aMock,
      page1,
      page2,
      ApplicationActionServiceMock.aMock,
      SubmissionServiceMock.aMock
    )
  }

  "confirmResponsibleIndividualVerifiedPage1" should {
    "return 200" in new Setup {
      val mySubmission = MarkedSubmission(submittedSubmission, markedAnswers)
      val mySubmissionReview = SubmissionReview(mySubmission.submission.id, mySubmission.submission.latestInstance.index, true, true, true, true)

      AuthConnectorMock.Authorise.thenReturn()
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(mySubmission)
      SubmissionReviewServiceMock.FindReview.thenReturn(mySubmissionReview)

      val result = controller.page1(applicationId)(fakeRequest)
      
      status(result) shouldBe Status.OK
    }

    "return 404 if the submission is not submitted" in new Setup {
      val mySubmission = MarkedSubmission(answeredSubmission, markedAnswers)

      AuthConnectorMock.Authorise.thenReturn()
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(mySubmission)

      val result = controller.page1(applicationId)(fakeRequest)
      
      status(result) shouldBe Status.BAD_REQUEST
    }

    "return 404 if not found" in new Setup {
      AuthConnectorMock.Authorise.thenReturn()
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenNotFound()

      val result = controller.page1(applicationId)(fakeRequest)
      
      status(result) shouldBe Status.NOT_FOUND
    }
  }

  "confirmResponsibleIndividualVerifiedPage2" should {
    "return 200" in new Setup {
      val mySubmission = MarkedSubmission(submittedSubmission, markedAnswers)
      val mySubmissionReview = SubmissionReview(mySubmission.submission.id, mySubmission.submission.latestInstance.index, true, true, true, true)

      AuthConnectorMock.Authorise.thenReturn()
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(mySubmission)
      SubmissionReviewServiceMock.FindReview.thenReturn(mySubmissionReview)

      val result = controller.page2(applicationId)(fakeRequest)
      
      status(result) shouldBe Status.OK
    }

    "return 404 if the submission is not submitted" in new Setup {
      val mySubmission = MarkedSubmission(answeredSubmission, markedAnswers)

      AuthConnectorMock.Authorise.thenReturn()
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(mySubmission)

      val result = controller.page2(applicationId)(fakeRequest)
      
      status(result) shouldBe Status.BAD_REQUEST
    }

    "return 404 if not found" in new Setup {
      AuthConnectorMock.Authorise.thenReturn()
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenNotFound()

      val result = controller.page2(applicationId)(fakeRequest)
      
      status(result) shouldBe Status.NOT_FOUND
    }
  }

  "confirmResponsibleIndividualVerifiedAction1" should {
    "redirect to checklist page when selected 'not verified'" in new Setup {
      val mySubmission = MarkedSubmission(submittedSubmission, markedAnswers)
      val mySubmissionReview = SubmissionReview(mySubmission.submission.id, mySubmission.submission.latestInstance.index, true, true, true, true)
      val fakeNotVerifiedRequest = fakeRequest.withFormUrlEncodedBody("submit-action" -> "checked", "verified" -> "no")

      AuthConnectorMock.Authorise.thenReturn()
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(applicationId)
      SubmissionReviewServiceMock.FindReview.thenReturn(mySubmissionReview)
      SubmissionReviewServiceMock.UpdateActionStatus.thenReturn(mySubmissionReview)
      SubmissionReviewServiceMock.UpdateVerifiedByDetails.thenReturn(mySubmissionReview)

      val result = controller.action1(applicationId)(fakeNotVerifiedRequest)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result).value shouldBe uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.routes.ChecklistController.checklistPage(applicationId).url
    }

    "redirect to page 2 when selected 'is verified'" in new Setup {
      val mySubmission = MarkedSubmission(submittedSubmission, markedAnswers)
      val mySubmissionReview = SubmissionReview(mySubmission.submission.id, mySubmission.submission.latestInstance.index, true, true, true, true)
      val fakeNotVerifiedRequest = fakeRequest.withFormUrlEncodedBody("submit-action" -> "checked", "verified" -> "yes")

      AuthConnectorMock.Authorise.thenReturn()
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(applicationId)
      SubmissionReviewServiceMock.FindReview.thenReturn(mySubmissionReview)
      SubmissionReviewServiceMock.UpdateActionStatus.thenReturn(mySubmissionReview)
      SubmissionReviewServiceMock.UpdateVerifiedByDetails.thenReturn(mySubmissionReview)

      val result = controller.action1(applicationId)(fakeNotVerifiedRequest)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result).value shouldBe uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.routes.ConfirmResponsibleIndividualVerifiedController.page2(applicationId).url
    }

    "bad request when no answer given" in new Setup {
      val mySubmission = MarkedSubmission(submittedSubmission, markedAnswers)
      val mySubmissionReview = SubmissionReview(mySubmission.submission.id, mySubmission.submission.latestInstance.index, true, true, true, true)
      val fakeNotVerifiedRequest = fakeRequest.withFormUrlEncodedBody("submit-action" -> "checked")

      AuthConnectorMock.Authorise.thenReturn()
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(applicationId)
      SubmissionReviewServiceMock.FindReview.thenReturn(mySubmissionReview)

      val result = controller.action1(applicationId)(fakeNotVerifiedRequest)

      status(result) shouldBe BAD_REQUEST
    }

    "redirect to checklist page when selecting come-back-later" in new Setup {
      AuthConnectorMock.Authorise.thenReturn()
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(applicationId)
      SubmissionReviewServiceMock.UpdateActionStatus.thenReturn(submissionReview)

      val result = controller.action1(applicationId)(fakeSubmitComebackLaterRequest)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result).value shouldBe uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.routes.ChecklistController.checklistPage(applicationId).url
    }

    "return bad request when sending an empty submit-action" in new Setup {
      AuthConnectorMock.Authorise.thenReturn()
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(applicationId)

      val result = controller.action1(applicationId)(fakeRequest)

      status(result) shouldBe BAD_REQUEST
    }
  }

  "confirmResponsibleIndividualVerifiedAction2" should {
    "redirect to checklist page when date entered" in new Setup {
      val mySubmission = MarkedSubmission(submittedSubmission, markedAnswers)
      val mySubmissionReview = SubmissionReview(mySubmission.submission.id, mySubmission.submission.latestInstance.index, true, true, true, true)
      val fakeNotVerifiedRequest = fakeRequest.withFormUrlEncodedBody("submit-action" -> "checked", "day" -> "21", "month" -> "11", "year" -> "2022")

      AuthConnectorMock.Authorise.thenReturn()
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(applicationId)
      SubmissionReviewServiceMock.FindReview.thenReturn(mySubmissionReview)
      SubmissionReviewServiceMock.UpdateActionStatus.thenReturn(mySubmissionReview)
      SubmissionReviewServiceMock.UpdateVerifiedByDetails.thenReturn(mySubmissionReview)

      val result = controller.action2(applicationId)(fakeNotVerifiedRequest)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result).value shouldBe uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.routes.ChecklistController.checklistPage(applicationId).url
    }

    "bad request when no date entered" in new Setup {
      val mySubmission = MarkedSubmission(submittedSubmission, markedAnswers)
      val mySubmissionReview = SubmissionReview(mySubmission.submission.id, mySubmission.submission.latestInstance.index, true, true, true, true)
      val fakeNotVerifiedRequest = fakeRequest.withFormUrlEncodedBody("submit-action" -> "checked")

      AuthConnectorMock.Authorise.thenReturn()
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(applicationId)
      SubmissionReviewServiceMock.FindReview.thenReturn(mySubmissionReview)

      val result = controller.action2(applicationId)(fakeNotVerifiedRequest)

      status(result) shouldBe BAD_REQUEST
    }

    "bad request when invalid date entered" in new Setup {
      val mySubmission = MarkedSubmission(submittedSubmission, markedAnswers)
      val mySubmissionReview = SubmissionReview(mySubmission.submission.id, mySubmission.submission.latestInstance.index, true, true, true, true)
      val fakeNotVerifiedRequest = fakeRequest.withFormUrlEncodedBody("submit-action" -> "checked", "day" -> "31", "month" -> "02", "year" -> "2022")

      AuthConnectorMock.Authorise.thenReturn()
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(applicationId)
      SubmissionReviewServiceMock.FindReview.thenReturn(mySubmissionReview)

      val result = controller.action2(applicationId)(fakeNotVerifiedRequest)

      status(result) shouldBe BAD_REQUEST
    }

    "redirect to checklist page when selecting come-back-later" in new Setup {
      AuthConnectorMock.Authorise.thenReturn()
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(applicationId)
      SubmissionReviewServiceMock.UpdateActionStatus.thenReturn(submissionReview)

      val result = controller.action2(applicationId)(fakeSubmitComebackLaterRequest)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result).value shouldBe uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.routes.ChecklistController.checklistPage(applicationId).url
    }

    "return bad request when sending an empty submit-action" in new Setup {
      AuthConnectorMock.Authorise.thenReturn()
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(applicationId)

      val result = controller.action2(applicationId)(fakeRequest)

      status(result) shouldBe BAD_REQUEST
    }
  }
}
