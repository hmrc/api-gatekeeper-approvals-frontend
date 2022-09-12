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

import uk.gov.hmrc.apigatekeeperapprovalsfrontend.views.html.{ApplicationDeclinedPage, ProvideReasonsForDecliningPage}
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.views.html.AdminsToEmailPage
import uk.gov.hmrc.apiplatform.modules.gkauth.services.StrideAuthorisationServiceMockModule
import uk.gov.hmrc.apiplatform.modules.gkauth.services.ApplicationServiceMockModule
import uk.gov.hmrc.apiplatform.modules.gkauth.domain.models.GatekeeperRoles

class DeclinedJourneyControllerSpec extends AbstractControllerSpec
      with StrideAuthorisationServiceMockModule {
  trait Setup extends AbstractSetup with ApplicationServiceMockModule {
    val applicationDeclinedPage = app.injector.instanceOf[ApplicationDeclinedPage]
    val provideReasonsForDecliningPage = app.injector.instanceOf[ProvideReasonsForDecliningPage]
    val adminsToEmailPage = app.injector.instanceOf[AdminsToEmailPage]
  
    val controller = new DeclinedJourneyController(
        StrideAuthorisationServiceMock.aMock,
        mcc,
        errorHandler,
        ApplicationActionServiceMock.aMock,
        SubmissionServiceMock.aMock,
        ApplicationServiceMock.aMock,
        applicationDeclinedPage,
        provideReasonsForDecliningPage,
        adminsToEmailPage,
        SubmissionReviewServiceMock.aMock
      )
  }

  "provide reasons page" should {
    "return 200" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(applicationId)
    
      val result = controller.provideReasonsPage(applicationId)(fakeRequest)

      status(result) shouldBe Status.OK
    }

    "return 404 when no marked submission is found" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenNotFound()

      val result = controller.provideReasonsPage(applicationId)(fakeRequest)
      
      status(result) shouldBe Status.NOT_FOUND
    }
  }

  "provide reasons action" should {
    "go to the email admins page when valid form with decline reasons is submitted" in new Setup {
      val fakeDeclineRequest = fakeRequest.withFormUrlEncodedBody("reasons" -> "submission looks bad")

      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(applicationId)
      SubmissionReviewServiceMock.UpdateDeclineReasons.thenReturn(submissionReview)
    
      val result = controller.provideReasonsAction(applicationId)(fakeDeclineRequest)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result).value shouldBe uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.routes.DeclinedJourneyController.emailAddressesPage(applicationId).url
    }

    "return bad request when valid form and persisting decline reasons fails" in new Setup {
      val fakeDeclineRequest = fakeRequest.withFormUrlEncodedBody("reasons" -> "submission looks bad")

      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(applicationId)
      SubmissionReviewServiceMock.UpdateDeclineReasons.thenReturnError()
    
      val result = controller.provideReasonsAction(applicationId)(fakeDeclineRequest)

      status(result) shouldBe BAD_REQUEST
    }

    "return bad request and go back to page when not a valid form" in new Setup {
      val brokenFormRequest = fakeRequest.withFormUrlEncodedBody()

      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(applicationId)

      val result = controller.provideReasonsAction(applicationId)(brokenFormRequest)

      status(result) shouldBe BAD_REQUEST
    }
  }

  "admins to email page" should {
    "return 200" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(applicationId)
    
      val result = controller.emailAddressesPage(applicationId)(fakeRequest)

      status(result) shouldBe Status.OK
    }

    "return 404 when no marked submission is found" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenNotFound()

      val result = controller.emailAddressesPage(applicationId)(fakeRequest)
      
      status(result) shouldBe Status.NOT_FOUND
    }
  }

  "admins to email action" should {
    "go to the application declined page" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(applicationId)
      SubmissionReviewServiceMock.FindOrCreateReview.thenReturn(submissionReview)
      ApplicationServiceMock.DeclineApplicationApprovalRequest.thenReturnSuccess()
    
      val result = controller.emailAddressesAction(applicationId)(fakeRequest)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result).value shouldBe uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.routes.DeclinedJourneyController.declinedPage(applicationId).url
    }
    
    "throw RuntimeException when the decline fails" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(applicationId)
      SubmissionReviewServiceMock.FindOrCreateReview.thenReturn(submissionReview)
      ApplicationServiceMock.DeclineApplicationApprovalRequest.thenReturnFailure()
    
      intercept[RuntimeException](await(controller.emailAddressesAction(applicationId)(fakeRequest))).getMessage shouldBe "Application id not found"
   }
  }
}
