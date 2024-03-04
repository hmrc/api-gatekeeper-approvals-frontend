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

package uk.gov.hmrc.apigatekeeperapprovalsfrontend.services

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future.successful
import scala.concurrent.{ExecutionContext, Future}

import cats.data.OptionT

import uk.gov.hmrc.apiplatform.modules.applications.submissions.domain.models.SubmissionId

import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.SubmissionReview
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.repositories.SubmissionReviewRepo

@Singleton
class SubmissionReviewService @Inject() (
    repo: SubmissionReviewRepo
  )(implicit val ec: ExecutionContext
  ) {

  def findOrCreateReview(
      submissionId: SubmissionId,
      instanceIndex: Int,
      isSuccessful: Boolean,
      hasWarnings: Boolean,
      requiresFraudCheck: Boolean,
      requiresDemo: Boolean
    ): Future[SubmissionReview] = {
    def createANewReview = {

      repo.create(SubmissionReview(submissionId, instanceIndex, isSuccessful, hasWarnings, requiresFraudCheck, requiresDemo))
    }

    repo.find(submissionId, instanceIndex)
      .flatMap(_.fold(createANewReview)(r => successful(r)))
  }

  def findReview(submissionId: SubmissionId, instanceIndex: Int): Future[Option[SubmissionReview]] = {
    repo.find(submissionId, instanceIndex)
  }

  def updateDeclineReasons(reasons: String)(submissionId: SubmissionId, instanceIndex: Int): Future[Option[SubmissionReview]] = {
    (
      for {
        originalReview <- OptionT(repo.find(submissionId, instanceIndex))
        changedReview   = SubmissionReview.updateDeclineReasons(reasons)(originalReview)
        _              <- OptionT.liftF(repo.update(changedReview))
      } yield changedReview
    )
      .value
  }

  def updateGrantWarnings(warnings: String)(submissionId: SubmissionId, instanceIndex: Int): Future[Option[SubmissionReview]] = {
    (
      for {
        originalReview <- OptionT(repo.find(submissionId, instanceIndex))
        changedReview   = SubmissionReview.updateGrantWarnings(warnings)(originalReview)
        _              <- OptionT.liftF(repo.update(changedReview))
      } yield changedReview
    )
      .value
  }

  def updateEscalatedTo(escalatedTo: String)(submissionId: SubmissionId, instanceIndex: Int): Future[Option[SubmissionReview]] = {
    (
      for {
        originalReview <- OptionT(repo.find(submissionId, instanceIndex))
        changedReview   = SubmissionReview.updateEscalatedTo(escalatedTo)(originalReview)
        _              <- OptionT.liftF(repo.update(changedReview))
      } yield changedReview
    )
      .value
  }

  def updateActionStatus(action: SubmissionReview.Action, newStatus: SubmissionReview.Status)(submissionId: SubmissionId, instanceIndex: Int): Future[Option[SubmissionReview]] = {
    (
      for {
        originalReview <- OptionT(repo.find(submissionId, instanceIndex))
        changedReview   = SubmissionReview.updateReviewActionStatus(action, newStatus)(originalReview)
        _              <- OptionT.liftF(repo.update(changedReview))
      } yield changedReview
    )
      .value
  }
}
