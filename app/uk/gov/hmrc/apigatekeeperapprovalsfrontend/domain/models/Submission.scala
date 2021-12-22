/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models
import org.joda.time.DateTime
import java.util.UUID
import cats.data.NonEmptyList
import play.api.libs.json.JsSuccess
import play.api.libs.json.OFormat

case class SubmissionId(value: String) extends AnyVal

object SubmissionId {
  implicit val format = play.api.libs.json.Json.valueFormat[SubmissionId]
  
  def random: SubmissionId = SubmissionId(UUID.randomUUID().toString())
}

case class Submission(
  id: SubmissionId,
  applicationId: ApplicationId
)

object MarkedSubmission {
  import play.api.libs.json.Json

  implicit val submissionReads = Json.reads[Submission]
  implicit val markedSubmissionReads = Json.reads[MarkedSubmission]
}

case class MarkedSubmission(
  submission: Submission,
  markedAnswers: Map[String, Map[String,String]] //TODO can't get Json.read to work using proper types so using strings for now
) {
  lazy val marks = markedAnswers.values.flatMap(_.values).toList
  lazy val isFail = marks.contains("fail") | marks.filter(_ == "warn").size >= 4
  lazy val hasWarnOrFail = marks.contains("fail") | marks.contains("warn") 
}