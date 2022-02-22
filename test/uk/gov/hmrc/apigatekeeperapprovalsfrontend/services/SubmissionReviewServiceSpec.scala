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

package uk.gov.hmrc.apigatekeeperapprovalsfrontend.services

import uk.gov.hmrc.apigatekeeperapprovalsfrontend.utils.AsyncHmrcSpec
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.SubmissionReview
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.repositories.SubmissionReviewRepoMockModule
import uk.gov.hmrc.apiplatform.modules.submissions.SubmissionsTestData
import scala.concurrent.ExecutionContext.Implicits.global
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.utils.ApplicationTestData

class SubmissionReviewServiceSpec extends AsyncHmrcSpec {

  trait Setup extends SubmissionReviewRepoMockModule with SubmissionsTestData with ApplicationTestData {
    val underTest = new SubmissionReviewService(SubmissionReviewRepoMock.aMock)

    val review = SubmissionReview(aSubmission.id, 0, true, true)

    implicit val hc = HeaderCarrier()
  }

  "findOrCreateReview" should {
    "return the existing review if one is already in the database" in new Setup {
      SubmissionReviewRepoMock.Find.thenReturn(review)
      val result = await(underTest.findOrCreateReview(review.submissionId, review.instanceIndex, true, true))
      result shouldBe review
      SubmissionReviewRepoMock.Create.verifyNotCalled
    }

    "create a new review if one does not exist in the database" in new Setup {
      SubmissionReviewRepoMock.Find.thenFindNone(review.submissionId, review.instanceIndex)
      SubmissionReviewRepoMock.Create.thenReturn(review)
      val result = await(underTest.findOrCreateReview(review.submissionId, review.instanceIndex, true, true))
      result shouldBe review
      SubmissionReviewRepoMock.Create.verifyCalled
    }
  }

  "updateCheckedFailsAndWarningsStatus" should {
    "set correct status in SubmissionReview for checkedFailsAndWarnings" in new Setup {
      val hasFailsReview = SubmissionReview(aSubmission.id, 0, false, true)

      SubmissionReviewRepoMock.Find.thenReturn(hasFailsReview)
      SubmissionReviewRepoMock.Update.thenReturn()
      
      val result = await(underTest.updateActionStatus(SubmissionReview.Action.CheckFailsAndWarnings, SubmissionReview.Status.InProgress)(hasFailsReview.submissionId, hasFailsReview.instanceIndex))
      
      result.value.requiredActions(SubmissionReview.Action.CheckFailsAndWarnings) shouldBe SubmissionReview.Status.InProgress
    }

    "set correct status in SubmissionReview for emailedResponsibleIndividual" in new Setup {
      SubmissionReviewRepoMock.Find.thenReturn(review)    
      SubmissionReviewRepoMock.Update.thenReturn()
      
      val result = await(underTest.updateActionStatus(SubmissionReview.Action.EmailResponsibleIndividual, SubmissionReview.Status.InProgress)(review.submissionId, review.instanceIndex))
      
      result.value.requiredActions(SubmissionReview.Action.EmailResponsibleIndividual) shouldBe SubmissionReview.Status.InProgress
    }
    
    "set correct status in SubmissionReview for checkedUrls" in new Setup {
      SubmissionReviewRepoMock.Find.thenReturn(review)    
      SubmissionReviewRepoMock.Update.thenReturn()
      
      val result = await(underTest.updateActionStatus(SubmissionReview.Action.CheckUrls, SubmissionReview.Status.InProgress)(review.submissionId, review.instanceIndex))

      result.value.requiredActions(SubmissionReview.Action.CheckUrls) shouldBe SubmissionReview.Status.InProgress
    }

    "set correct status in SubmissionReview for checkedForSandboxTesting" in new Setup {
      SubmissionReviewRepoMock.Find.thenReturn(review)    
      SubmissionReviewRepoMock.Update.thenReturn()

      val result = await(underTest.updateActionStatus(SubmissionReview.Action.CheckSandboxTesting, SubmissionReview.Status.InProgress)(review.submissionId, review.instanceIndex))

      result.value.requiredActions(SubmissionReview.Action.CheckSandboxTesting) shouldBe SubmissionReview.Status.InProgress
    }
    
    "set correct status in SubmissionReview for checkedPassedAnswers" in new Setup {
      val passedReview = SubmissionReview(aSubmission.id, 0, true,false)
      SubmissionReviewRepoMock.Find.thenReturn(passedReview)    
      SubmissionReviewRepoMock.Update.thenReturn()
      
      val result = await(underTest.updateActionStatus(SubmissionReview.Action.CheckPassedAnswers, SubmissionReview.Status.InProgress)(passedReview.submissionId, passedReview.instanceIndex))

      result.value.requiredActions(SubmissionReview.Action.CheckPassedAnswers) shouldBe SubmissionReview.Status.InProgress
    }
  }
}