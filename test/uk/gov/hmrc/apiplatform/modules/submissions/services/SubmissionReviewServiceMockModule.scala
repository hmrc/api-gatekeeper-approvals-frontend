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

package uk.gov.hmrc.apiplatform.modules.submissions.services

import org.mockito.MockitoSugar
import org.mockito.ArgumentMatchersSugar

import scala.concurrent.Future.successful
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models._
import uk.gov.hmrc.apiplatform.modules.submissions.domain.models._
import uk.gov.hmrc.apiplatform.modules.submissions.SubmissionsTestData
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.services.SubmissionReviewService

trait SubmissionReviewServiceMockModule extends MockitoSugar with ArgumentMatchersSugar with SubmissionsTestData {
  trait BaseSubmissionReviewServiceMock {
    def aMock: SubmissionReviewService

    object FindOrCreateReview {
      def thenReturn(review: SubmissionReview) = {
        when(aMock.findOrCreateReview(*[Submission.Id], *)(*)).thenReturn(successful(review))
      }
    }
  }

  object SubmissionReviewServiceMock extends BaseSubmissionReviewServiceMock {
    val aMock = mock[SubmissionReviewService](withSettings.lenient())
  }
}
