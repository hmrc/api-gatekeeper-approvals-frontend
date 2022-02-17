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

import uk.gov.hmrc.apigatekeeperapprovalsfrontend.config.AppConfig
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.SubmissionReview
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.views.html.ChecklistPage

class ChecklistControllerSpec extends AbstractControllerSpec {
  trait Setup extends AbstractSetup {
    implicit val appConfig = app.injector.instanceOf[AppConfig]
    val appChecklistPage = app.injector.instanceOf[ChecklistPage]

    val controller = new ChecklistController(
      strideAuthConfig,
      AuthConnectorMock.aMock,
      forbiddenHandler,
      mcc,
      appChecklistPage,
      errorHandler,
      ApplicationActionServiceMock.aMock,
      SubmissionServiceMock.aMock,
      SubmissionReviewServiceMock.aMock
    )
  }

  "GET /" should {
    "return 200" in new Setup {
      val submissionReview = SubmissionReview(markedSubmission.submission.id, 0)

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
      val fakeRequestWithAction = fakeRequest.withFormUrlEncodedBody("submit-action" -> "checked")

      AuthConnectorMock.Authorise.thenReturn()
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(applicationId)

      val result = controller.checklistAction(applicationId)(fakeRequestWithAction)
      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some(s"/api-gatekeeper-approvals/applications/${applicationId.value}/confirm-decision")
    }

    "return 200 and send to submissions page if Save and Come Back Later button is clicked" in new Setup {
      val fakeRequestWithAction = fakeRequest.withFormUrlEncodedBody("submit-action" -> "come-back-later")

      AuthConnectorMock.Authorise.thenReturn()
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(applicationId)

      val result = controller.checklistAction(applicationId)(fakeRequestWithAction)
      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some(s"/api-gatekeeper-approvals/applications/${applicationId.value}/reviews")
    }

    "return 400 if bad submission action is received" in new Setup {
      val fakeRequestWithAction = fakeRequest.withFormUrlEncodedBody("submit-action" -> "nope")

      AuthConnectorMock.Authorise.thenReturn()
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(applicationId)

      val result = controller.checklistAction(applicationId)(fakeRequestWithAction)
      status(result) shouldBe Status.BAD_REQUEST
    }
  }
}
