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

package uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.services

import play.api.libs.json._
import uk.gov.hmrc.play.json.Union

import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.SubmissionReview

trait SubmissionReviewJsonFormatters {
  import SubmissionReview._

  implicit val reviewStatus: OFormat[Status] = Union.from[Status]("Review.StatusType")
    .andType[Status.NotStarted.type]("notstarted", () => Status.NotStarted)
    .andType[Status.InProgress.type]("inprogress", () => Status.InProgress)
    .andType[Status.Completed.type]("completed", () => Status.Completed)
    .format

  implicit val actionKeyReads: KeyReads[Action]   = key => SubmissionReview.Action.fromText(key).fold[JsResult[Action]](JsError(s"Bad action key $key"))(a => JsSuccess(a))
  implicit val actionKeyWrites: KeyWrites[Action] = action => SubmissionReview.Action.toText(action)

  implicit val submissionReviewFormat: Format[SubmissionReview] = Json.format[SubmissionReview]
}

object SubmissionReviewJsonFormatters extends SubmissionReviewJsonFormatters
