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

package uk.gov.hmrc.apigatekeeperapprovalsfrontend.repositories

import org.mockito.MockitoSugar
import org.mockito.ArgumentMatchersSugar

import scala.concurrent.Future.successful
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models._
import uk.gov.hmrc.apiplatform.modules.submissions.domain.models._
import uk.gov.hmrc.apiplatform.modules.submissions.SubmissionsTestData
import org.mockito.verification.VerificationMode

trait SubmissionReviewRepoMockModule extends MockitoSugar with ArgumentMatchersSugar with SubmissionsTestData {
  trait BaseSubmissionReviewRepoMock {
    def aMock: SubmissionReviewRepo

    def verify = MockitoSugar.verify(aMock)

    def verify(mode: VerificationMode) = MockitoSugar.verify(aMock,mode)

    def verifyZeroInteractions() = MockitoSugar.verifyZeroInteractions(aMock)

    object Create {
      def thenReturn(review: SubmissionReview) = {
        when(aMock.create(eqTo(review))).thenReturn(successful(review))
      }
      
      def verifyNotCalled() {
        verify(never).create(*)
      }

      def verifyCalled() {
        verify(atLeastOnce).create(*)
      }
    }

    object Find {
      def thenReturn(review: SubmissionReview) =
        when(aMock.find(eqTo(review.submissionId), eqTo(review.instanceIndex))).thenReturn(successful(Some(review)))

      def thenFindNone(submissionId: Submission.Id, instanceIndex: Int) =
        when(aMock.find(eqTo(submissionId), eqTo(instanceIndex))).thenReturn(successful(None))
    }

    object Update {
      def thenReturn() = {
        when(aMock.update(*[SubmissionReview])).thenAnswer((sr: SubmissionReview) => successful(sr))
      }
      def verifyCalledWith(review: SubmissionReview) = verify.update(review)

    }
  }

  object SubmissionReviewRepoMock extends BaseSubmissionReviewRepoMock {
    val aMock = mock[SubmissionReviewRepo](withSettings.lenient())
  }
}
