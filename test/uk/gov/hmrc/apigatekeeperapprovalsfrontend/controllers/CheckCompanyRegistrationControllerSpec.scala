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

import uk.gov.hmrc.apigatekeeperapprovalsfrontend.views.html.CheckCompanyRegistrationPage
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.ApplicationState
import uk.gov.hmrc.apiplatform.modules.submissions.services.SubmissionReviewServiceMockModule
import uk.gov.hmrc.apiplatform.modules.submissions.domain.models.MarkedSubmission
import uk.gov.hmrc.apiplatform.modules.submissions.MarkedSubmissionsTestData
import uk.gov.hmrc.apiplatform.modules.gkauth.services.StrideAuthorisationServiceMockModule
import uk.gov.hmrc.apiplatform.modules.gkauth.domain.models.GatekeeperRoles

class CheckCompanyRegistrationControllerSpec extends AbstractControllerSpec with MarkedSubmissionsTestData {
  
  trait Setup extends AbstractSetup with SubmissionReviewServiceMockModule
      with StrideAuthorisationServiceMockModule {
    val page = app.injector.instanceOf[CheckCompanyRegistrationPage]
    
    val controller = new CheckCompanyRegistrationController(
      StrideAuthorisationServiceMock.aMock,
      mcc,
      page,
      errorHandler,
      SubmissionReviewServiceMock.aMock,
      ApplicationActionServiceMock.aMock,
      SubmissionServiceMock.aMock
    )
  }

  "checkCompanyRegistrationPage" should {
    "return 200" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(markedSubmission)

      val result = controller.page(applicationId)(fakeRequest)
      
      status(result) shouldBe Status.OK
      contentAsString(result) should not include("This application has been deleted")
    }

    "return 200 with a deleted application" in new Setup {
      val deletedApp = application.copy(state = ApplicationState.deleted("delete-user@example.com"))
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      ApplicationActionServiceMock.Process.thenReturn(deletedApp)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(markedSubmission)

      val result = controller.page(applicationId)(fakeRequest)
      
      status(result) shouldBe Status.OK
      contentAsString(result) should include("This application has been deleted")
    }

    "return 400 if the submission is not submitted" in new Setup {
      val mySubmission = MarkedSubmission(answeredSubmission, markedAnswers)

      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(mySubmission)

      val result = controller.page(applicationId)(fakeRequest)
      
      status(result) shouldBe Status.BAD_REQUEST
    }

    "return 404 if not found" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenNotFound()

      val result = controller.page(applicationId)(fakeRequest)
      
      status(result) shouldBe Status.NOT_FOUND
    }
  }

  "checkCompanyRegistrationAction" should {
    "redirect to correct page when marking URLs as checked" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(applicationId)
      SubmissionReviewServiceMock.UpdateActionStatus.thenReturn(submissionReview)

      val result = controller.action(applicationId)(fakeSubmitCheckedRequest)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(s"/api-gatekeeper-approvals/applications/${applicationId.value}/checklist")
    }

    "redirect to correct page when marking URLs as come-back-later" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(applicationId)
      SubmissionReviewServiceMock.UpdateActionStatus.thenReturn(submissionReview)

      val result = controller.action(applicationId)(fakeSubmitComebackLaterRequest)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(s"/api-gatekeeper-approvals/applications/${applicationId.value}/checklist")
    }

    "return bad request when sending an empty submit-action" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(applicationId)

      val result = controller.action(applicationId)(fakeRequest)

      status(result) shouldBe BAD_REQUEST
    }
  }
}
