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

import play.api.http.Status
import play.api.test.Helpers._
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.SubmissionReview
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.views.html.CheckFraudPage

import scala.concurrent.ExecutionContext.Implicits.global

class CheckFraudControllerSpec extends AbstractControllerSpec {
  
  trait Setup extends AbstractSetup {
    val page = app.injector.instanceOf[CheckFraudPage]
    
    val controller = new CheckFraudController(
      strideAuthConfig,
      AuthConnectorMock.aMock,
      forbiddenHandler,
      mcc,
      page,
      errorHandler,
      ApplicationActionServiceMock.aMock,
      SubmissionServiceMock.aMock,
      SubmissionReviewServiceMock.aMock
    )
  }

  "checkFraudPage" should {
    "return 200" in new Setup {
      AuthConnectorMock.Authorise.thenReturn()
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(applicationId)

      val result = controller.checkFraudPage(applicationId)(fakeRequest)
      
      status(result) shouldBe Status.OK
    }

    "return 404" in new Setup {
      AuthConnectorMock.Authorise.thenReturn()
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenNotFound()

      val result = controller.checkFraudPage(applicationId)(fakeRequest)
      
      status(result) shouldBe Status.NOT_FOUND
    }
  }

  "checkFraudAction" should {
    "redirect to correct page when marking fraud check as complete" in new Setup {
      AuthConnectorMock.Authorise.thenReturn()
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(applicationId)
      SubmissionReviewServiceMock.UpdateActionStatus.thenReturn(SubmissionReview(submissionId, 0, true, true))

      val result = controller.checkFraudAction(applicationId)(fakeSubmitCheckedRequest)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.routes.ChecklistController.checklistPage(applicationId).url)
    }

    "redirect to correct page when marking URLs as come-back-later" in new Setup {
      AuthConnectorMock.Authorise.thenReturn()
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(applicationId)
      SubmissionReviewServiceMock.UpdateActionStatus.thenReturn(SubmissionReview(submissionId, 0, true, true))

      val result = controller.checkFraudAction(applicationId)(fakeSubmitComebackLaterRequest)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.routes.ChecklistController.checklistPage(applicationId).url)
    }

    "return bad request when sending an empty submit-action" in new Setup {
      AuthConnectorMock.Authorise.thenReturn()
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(applicationId)

      val result = controller.checkFraudAction(applicationId)(fakeRequest)

      status(result) shouldBe BAD_REQUEST
    }
  }
}
