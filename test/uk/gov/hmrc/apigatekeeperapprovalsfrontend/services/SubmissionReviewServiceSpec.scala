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

class SubmissionReviewServiceSpec extends AsyncHmrcSpec {

  trait Setup extends SubmissionReviewRepoMockModule with SubmissionsTestData{
    val underTest = new SubmissionReviewService(SubmissionReviewRepoMock.aMock)
    val review = SubmissionReview(submission.id, 0)
    implicit val hc = HeaderCarrier()
  }

  "findOrCreateReview" should {
    "return the existing review if one is already in the database" in new Setup {
      SubmissionReviewRepoMock.Find.thenReturn(review)
      val result = await(underTest.findOrCreateReview(review.submissionId, review.instanceIndex))
      result shouldBe review
      SubmissionReviewRepoMock.Create.verifyNotCalled
    }

    "create a new review if one does not exist in the database" in new Setup {
      SubmissionReviewRepoMock.Find.thenFindNone(review.submissionId, review.instanceIndex)
      SubmissionReviewRepoMock.Create.thenReturn(review)
      val result = await(underTest.findOrCreateReview(review.submissionId, review.instanceIndex))
      result shouldBe review
      SubmissionReviewRepoMock.Create.verifyCalled
    }
  }

  "updateCheckedFailsAndWarningsStatus" should {
    "set correct status in SubmissionReview for checkedFailsAndWarnings" in new Setup {
      SubmissionReviewRepoMock.Find.thenReturn(review)    
      val updatedReview = review.copy(checkedFailsAndWarnings = SubmissionReview.Status.InProgress)
      SubmissionReviewRepoMock.Update.thenReturn()

      val result = await(underTest.updateCheckedFailsAndWarningsStatus(SubmissionReview.Status.InProgress)(review.submissionId, review.instanceIndex))
      result shouldBe Some(updatedReview)
    }

    "set correct status in SubmissionReview for emailedResponsibleIndividual" in new Setup {
      SubmissionReviewRepoMock.Find.thenReturn(review)    
      val updatedReview = review.copy(emailedResponsibleIndividual = SubmissionReview.Status.InProgress)
      SubmissionReviewRepoMock.Update.thenReturn()

      val result = await(underTest.updateEmailedResponsibleIndividualStatus(SubmissionReview.Status.InProgress)(review.submissionId, review.instanceIndex))
      result shouldBe Some(updatedReview)
    }

    "set correct status in SubmissionReview for checkedUrls" in new Setup {
      SubmissionReviewRepoMock.Find.thenReturn(review)    
      val updatedReview = review.copy(checkedUrls = SubmissionReview.Status.InProgress)
      SubmissionReviewRepoMock.Update.thenReturn()

      val result = await(underTest.updateCheckedUrlsStatus(SubmissionReview.Status.InProgress)(review.submissionId, review.instanceIndex))
      result shouldBe Some(updatedReview)
    }

    "set correct status in SubmissionReview for checkedForSandboxTesting" in new Setup {
      SubmissionReviewRepoMock.Find.thenReturn(review)    
      val updatedReview = review.copy(checkedForSandboxTesting = SubmissionReview.Status.InProgress)
      SubmissionReviewRepoMock.Update.thenReturn()

      val result = await(underTest.updateCheckedForSandboxTestingStatus(SubmissionReview.Status.InProgress)(review.submissionId, review.instanceIndex))
      result shouldBe Some(updatedReview)
    }

    "set correct status in SubmissionReview for checkedPassedAnswers" in new Setup {
      SubmissionReviewRepoMock.Find.thenReturn(review)    
      val updatedReview = review.copy(checkedPassedAnswers = SubmissionReview.Status.InProgress)
      SubmissionReviewRepoMock.Update.thenReturn()

      val result = await(underTest.updateCheckedPassedAnswersStatus(SubmissionReview.Status.InProgress)(review.submissionId, review.instanceIndex))
      result shouldBe Some(updatedReview)
    }
  }
}