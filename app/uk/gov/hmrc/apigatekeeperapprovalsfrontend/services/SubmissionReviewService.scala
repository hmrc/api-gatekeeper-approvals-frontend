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

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.Future.successful
import uk.gov.hmrc.apiplatform.modules.submissions.domain.models.Submission
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.SubmissionReview
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.repositories.SubmissionReviewRepo
import cats.data.OptionT

@Singleton
class SubmissionReviewService @Inject()(
  repo: SubmissionReviewRepo
)(implicit val ec: ExecutionContext) {

  type UpdateFn = (SubmissionReview) => SubmissionReview

  def findOrCreateReview(submissionId: Submission.Id, instanceIndex: Int): Future[SubmissionReview] = {
    def createANewReview = repo.create(SubmissionReview(submissionId, instanceIndex))
    repo.find(submissionId, instanceIndex)
      .flatMap( _.fold(createANewReview)(r => successful(r)))
  }

  private def updateReview(fn: UpdateFn)(submissionId: Submission.Id, instanceIndex: Int): Future[Option[SubmissionReview]] = {
    OptionT(repo.find(submissionId, instanceIndex))
    .semiflatMap(repo.update)
    .value
  }

  def updateCheckedFailsAndWarningsStatus(newStatus: SubmissionReview.Status) = updateReview(_.copy(checkedFailsAndWarnings = newStatus)) _

  def updateEmailedResponsibleIndividualStatus(newStatus: SubmissionReview.Status) = updateReview(_.copy(emailedResponsibleIndividual = newStatus)) _

  def updateCheckedUrlsStatus(newStatus: SubmissionReview.Status) = updateReview(_.copy(checkedUrls = newStatus)) _

  def updateCheckedForSandboxTestingStatus(newStatus: SubmissionReview.Status) = updateReview(_.copy(checkedForSandboxTesting = newStatus)) _
  
  def updateCheckedPassedAnswersStatus(newStatus: SubmissionReview.Status) = updateReview(_.copy(checkedPassedAnswers = newStatus)) _
}
