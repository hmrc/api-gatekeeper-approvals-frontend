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

package uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.services

import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.SubmissionReview
import play.api.libs.json.Json
import uk.gov.hmrc.play.json.Union

trait SubmissionReviewJsonFormatters {
  implicit val ReviewNotStartedStatusFormat = Json.format[SubmissionReview.Status.ReviewNotStarted.type]
  implicit val ReviewInProgressStatusFormat = Json.format[SubmissionReview.Status.ReviewInProgress.type]
  implicit val ReviewCompletedStatusFormat = Json.format[SubmissionReview.Status.ReviewCompleted.type]

  implicit val reviewStatus = Union.from[SubmissionReview.Status]("Review.StatusType")
    .and[SubmissionReview.Status.ReviewNotStarted.type]("notstarted")
    .and[SubmissionReview.Status.ReviewInProgress.type]("inprogress")
    .and[SubmissionReview.Status.ReviewCompleted.type]("completed")
    .format

  implicit val submissionReviewFormat = Json.format[SubmissionReview]    
}

object SubmissionReviewJsonFormatters extends SubmissionReviewJsonFormatters