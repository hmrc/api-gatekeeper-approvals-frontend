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

import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.SubmissionReview
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.views.html.CheckApplicationNamePage

class CheckApplicationNameControllerSpec extends AbstractControllerSpec {

  trait Setup extends AbstractSetup with SubmissionReviewServiceMockModule {
    val page = app.injector.instanceOf[CheckApplicationNamePage]

    val controller = new CheckApplicationNameController(
      strideAuthConfig,
      AuthConnectorMock.aMock,
      forbiddenHandler,
      mcc,
      SubmissionReviewServiceMock.aMock,
      errorHandler,
      page,
      ApplicationActionServiceMock.aMock,
      SubmissionServiceMock.aMock
    )
  }

  "checkApplicationNamePage" should {
    "return 200" in new Setup {
      val mySubmission = MarkedSubmission(submittedSubmission, markedAnswers)
      
      AuthConnectorMock.Authorise.thenReturn()
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(mySubmission)

      val result = controller.page(applicationId)(fakeRequest)
      
      status(result) shouldBe Status.OK
    }

    "return 400 if the submission is not submitted" in new Setup {
      val mySubmission = MarkedSubmission(answeredSubmission, markedAnswers)

      AuthConnectorMock.Authorise.thenReturn()
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(mySubmission)

      val result = controller.page(applicationId)(fakeRequest)
      
      status(result) shouldBe Status.BAD_REQUEST
    }

    "return 404 if not found" in new Setup {
      AuthConnectorMock.Authorise.thenReturn()
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenNotFound()

      val result = controller.page(applicationId)(fakeRequest)
      
      status(result) shouldBe Status.NOT_FOUND
    }
  }

  "checkApplicationNameAction" should {
    "redirect to correct page when marking URLs as checked" in new Setup {
      AuthConnectorMock.Authorise.thenReturn()
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(applicationId)
      SubmissionReviewServiceMock.UpdateActionStatus.thenReturn(SubmissionReview(submissionId, 0, true, true))

      val result = controller.action(applicationId)(fakeSubmitCheckedRequest)

      status(result) shouldBe SEE_OTHER
    }

    "redirect to correct page when marking URLs as come-back-later" in new Setup {
      AuthConnectorMock.Authorise.thenReturn()
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(applicationId)
      SubmissionReviewServiceMock.UpdateActionStatus.thenReturn(SubmissionReview(submissionId, 0, true, true))

      val result = controller.action(applicationId)(fakeSubmitComebackLaterRequest)

      status(result) shouldBe SEE_OTHER
    }

    "return bad request when sending an empty submit-action" in new Setup {
      AuthConnectorMock.Authorise.thenReturn()
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(applicationId)

      val result = controller.action(applicationId)(fakeRequest)

      status(result) shouldBe BAD_REQUEST
    }
  }
}