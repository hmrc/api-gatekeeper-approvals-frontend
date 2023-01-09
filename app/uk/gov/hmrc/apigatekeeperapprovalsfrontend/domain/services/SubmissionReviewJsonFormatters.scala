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

import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.SubmissionReview
import play.api.libs.json.Json
import play.api.libs.json.JodaReads
import uk.gov.hmrc.play.json.Union
import play.api.libs.json.KeyReads
import play.api.libs.json.JsSuccess
import play.api.libs.json.KeyWrites
import play.api.libs.json.JsError
import play.api.libs.json.JsResult
import org.joda.time.DateTimeZone

trait SubmissionReviewJsonFormatters {
  import SubmissionReview._

  implicit val utcReads = JodaReads.DefaultJodaDateTimeReads.map(dt => dt.withZone(DateTimeZone.UTC))

  implicit val ReviewNotStartedStatusFormat = Json.format[Status.NotStarted.type]
  implicit val ReviewInProgressStatusFormat = Json.format[Status.InProgress.type]
  implicit val ReviewCompletedStatusFormat  = Json.format[Status.Completed.type]

  implicit val reviewStatus = Union.from[Status]("Review.StatusType")
    .and[Status.NotStarted.type]("notstarted")
    .and[Status.InProgress.type]("inprogress")
    .and[Status.Completed.type]("completed")
    .format

  implicit val actionKeyReads: KeyReads[Action]   = key => SubmissionReview.Action.fromText(key).fold[JsResult[Action]](JsError(s"Bad action key $key"))(a => JsSuccess(a))
  implicit val actionKeyWrites: KeyWrites[Action] = action => SubmissionReview.Action.toText(action)

  implicit val submissionReviewFormat = Json.format[SubmissionReview]
}

object SubmissionReviewJsonFormatters extends SubmissionReviewJsonFormatters
