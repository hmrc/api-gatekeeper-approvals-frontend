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

import uk.gov.hmrc.thirdpartyapplication.repository.SubmissionReviewRepo
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext
import uk.gov.hmrc.http.HeaderCarrier
import scala.concurrent.Future
import scala.concurrent.Future.successful
import uk.gov.hmrc.apiplatform.modules.submissions.domain.models.Submission
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.SubmissionReview

@Singleton
class SubmissionReviewService @Inject()(
  repo: SubmissionReviewRepo
)(implicit val ec: ExecutionContext) {
  
  def findOrCreateReview(submissionId: Submission.Id, instanceIndex: Int)(implicit hc: HeaderCarrier): Future[SubmissionReview] = {
    def createANewReview = repo.create(submissionId, instanceIndex)
    repo.find(submissionId, instanceIndex)
      .flatMap( _.fold(createANewReview)(r => successful(r)))
  }
}
