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

package uk.gov.hmrc.apiplatform.modules.submissions.services

import scala.concurrent.Future
import scala.concurrent.Future.successful

import org.mockito.{ArgumentMatchersSugar, MockitoSugar}

import uk.gov.hmrc.apiplatform.modules.submissions.SubmissionReviewTestData
import uk.gov.hmrc.apiplatform.modules.submissions.domain.models.Submission

import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models._
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.services.SubmissionReviewService

trait SubmissionReviewServiceMockModule extends MockitoSugar with ArgumentMatchersSugar with SubmissionReviewTestData {

  trait BaseSubmissionReviewServiceMock {
    def aMock: SubmissionReviewService

    object FindOrCreateReview {

      def thenReturn(review: SubmissionReview) = {
        when(aMock.findOrCreateReview(eqTo(review.submissionId), eqTo(review.instanceIndex), *, *, *, *)).thenReturn(successful(review))
      }
    }

    object FindReview {

      def thenReturn(review: SubmissionReview) = {
        when(aMock.findReview(eqTo(review.submissionId), eqTo(review.instanceIndex))).thenReturn(successful(Some(review)))
      }
    }

    object UpdateActionStatus {

      def thenReturn(review: SubmissionReview) = {
        when(aMock.updateActionStatus(*, *)(*[Submission.Id], *)).thenReturn(Future.successful(Some(review)))
      }

      def thenReturn(action: SubmissionReview.Action, status: SubmissionReview.Status, review: SubmissionReview) = {
        when(aMock.updateActionStatus(eqTo(action), eqTo(status))(*[Submission.Id], *)).thenReturn(Future.successful(Some(review.copy(requiredActions =
          review.requiredActions + (action -> status)
        ))))
      }
    }

    object UpdateDeclineReasons {

      def thenReturn(review: SubmissionReview) = {
        when(aMock.updateDeclineReasons(*)(*[Submission.Id], *)).thenReturn(Future.successful(Some(review)))
      }

      def thenReturnError() = {
        when(aMock.updateDeclineReasons(*)(*[Submission.Id], *)).thenReturn(Future.successful(None))
      }
    }

    object UpdateGrantWarnings {

      def thenReturn(review: SubmissionReview) = {
        when(aMock.updateGrantWarnings(*)(*[Submission.Id], *)).thenReturn(Future.successful(Some(review)))
      }

      def thenReturnError() = {
        when(aMock.updateGrantWarnings(*)(*[Submission.Id], *)).thenReturn(Future.successful(None))
      }
    }

    object UpdateEscalatedTo {

      def thenReturn(review: SubmissionReview) = {
        when(aMock.updateEscalatedTo(*)(*[Submission.Id], *)).thenReturn(Future.successful(Some(review)))
      }

      def thenReturnError() = {
        when(aMock.updateEscalatedTo(*)(*[Submission.Id], *)).thenReturn(Future.successful(None))
      }
    }
  }

  object SubmissionReviewServiceMock extends BaseSubmissionReviewServiceMock {
    val aMock = mock[SubmissionReviewService](withSettings.lenient())
  }
}
