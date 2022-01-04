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

package uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models
import java.util.UUID
import play.api.libs.json._

case class SubmissionId(value: String) extends AnyVal

object SubmissionId {
  implicit val format = play.api.libs.json.Json.valueFormat[SubmissionId]
  
  def random: SubmissionId = SubmissionId(UUID.randomUUID().toString())
}

case class QuestionId(value: String) extends AnyVal

object QuestionId {
  implicit val format = play.api.libs.json.Json.valueFormat[QuestionId]
  
  def random: QuestionId = QuestionId(UUID.randomUUID().toString())
}

case class Submission(
  id: SubmissionId,
  applicationId: ApplicationId
)

sealed trait Mark
case object Fail extends Mark
case object Warn extends Mark
case object Pass extends Mark

object MarkedSubmission {
  implicit val submissionReads = Json.reads[Submission]
  implicit val markWrites : Writes[Mark] = new Writes[Mark] {
    override def writes(o: Mark): JsValue = o match {
      case Fail => JsString("fail")
      case Warn => JsString("warn")
      case Pass => JsString("pass")
    }
  }
  
  implicit val markReads : Reads[Mark] = Reads {
    case JsString("fail") => JsSuccess(Fail)
    case JsString("warn") => JsSuccess(Warn)
    case JsString("pass") => JsSuccess(Pass)
    case _ => JsError("Failed to parse Mark value")
  }

  implicit val keyReadsQuestionId: KeyReads[QuestionId] = key => JsSuccess(QuestionId(key))
  implicit val keyWritesQuestionId: KeyWrites[QuestionId] = _.value

  implicit val markedSubmissionReads = Json.reads[MarkedSubmission]
}

case class MarkedSubmission(
  submission: Submission,
  markedAnswers: Map[QuestionId, Mark]
) {
  lazy val marks = markedAnswers.values.toList
  lazy val isFail = marks.contains(Fail) | marks.filter(_ == Warn).size >= 4
  lazy val hasWarnings = marks.contains(Warn) 
}