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

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

import org.mongodb.scala.model.{Filters, IndexModel, IndexOptions, Indexes}

import uk.gov.hmrc.apiplatform.modules.submissions.domain.models.Submission
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.SubmissionReview
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.services.SubmissionReviewJsonFormatters.submissionReviewFormat

@Singleton
class SubmissionReviewRepo @Inject() (mongo: MongoComponent)(implicit ec: ExecutionContext) extends PlayMongoRepository[SubmissionReview](
      mongoComponent = mongo,
      collectionName = "submissionReview",
      domainFormat = submissionReviewFormat,
      indexes = Seq(
        IndexModel(Indexes.ascending("submissionId", "instanceIndex"), IndexOptions().unique(true))
      )
    ) {

  def filterBy(submissionId: Submission.Id, instanceIndex: Int) =
    Filters.and(
      Filters.equal("submissionId", submissionId.value),
      Filters.equal("instanceIndex", instanceIndex)
    )

  def find(submissionId: Submission.Id, instanceIndex: Int): Future[Option[SubmissionReview]] = {
    collection.find(
      filter = filterBy(submissionId, instanceIndex)
    ).headOption()
  }

  def create(review: SubmissionReview): Future[SubmissionReview] = {
    collection.insertOne(review).toFuture().map(_ => review)
  }

  def update(review: SubmissionReview): Future[SubmissionReview] = {
    val filter = filterBy(review.submissionId, review.instanceIndex)
    collection.findOneAndReplace(filter, review).toFuture().map(_ => review)
  }
}
