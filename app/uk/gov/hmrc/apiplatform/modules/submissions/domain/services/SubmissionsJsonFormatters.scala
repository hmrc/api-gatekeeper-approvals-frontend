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

package uk.gov.hmrc.apiplatform.modules.submissions.domain.services

import java.time.Instant

import play.api.libs.json._
import uk.gov.hmrc.apiplatform.modules.common.domain.services.InstantJsonFormatter.WithTimeZone.instantWithTimeZoneWrites
import uk.gov.hmrc.apiplatform.modules.common.domain.services.InstantJsonFormatter.lenientInstantReads
import uk.gov.hmrc.apiplatform.modules.submissions.domain.models._
import uk.gov.hmrc.play.json.Union

trait BaseSubmissionsJsonFormatters extends GroupOfQuestionnairesJsonFormatters with EnvReads with EnvWrites {

  implicit val keyReadsQuestionnaireId: KeyReads[Questionnaire.Id]   = key => JsSuccess(Questionnaire.Id(key))
  implicit val keyWritesQuestionnaireId: KeyWrites[Questionnaire.Id] = _.value

  implicit val stateWrites: Writes[QuestionnaireState] = Writes {
    case QuestionnaireState.NotStarted    => JsString("NotStarted")
    case QuestionnaireState.InProgress    => JsString("InProgress")
    case QuestionnaireState.NotApplicable => JsString("NotApplicable")
    case QuestionnaireState.Completed     => JsString("Completed")
  }

  implicit val stateReads: Reads[QuestionnaireState] = Reads {
    case JsString("NotStarted")    => JsSuccess(QuestionnaireState.NotStarted)
    case JsString("InProgress")    => JsSuccess(QuestionnaireState.InProgress)
    case JsString("NotApplicable") => JsSuccess(QuestionnaireState.NotApplicable)
    case JsString("Completed")     => JsSuccess(QuestionnaireState.Completed)
    case _                         => JsError("Failed to parse QuestionnaireState value")
  }

  implicit val questionnaireProgressFormat: Format[QuestionnaireProgress] = Json.format[QuestionnaireProgress]

  implicit val answersToQuestionsFormat: OFormat[Map[Question.Id, Option[ActualAnswer]]] = implicitly

  implicit val questionIdsOfInterestFormat: Format[QuestionIdsOfInterest] = Json.format[QuestionIdsOfInterest]
}

trait SubmissionsJsonFormatters extends BaseSubmissionsJsonFormatters {
  import Submission.Status._

  implicit val dateFormat: Format[Instant] = Format(lenientInstantReads, instantWithTimeZoneWrites)

  implicit val rejectedStatusFormat: OFormat[Declined]                                         = Json.format[Declined]
  implicit val acceptedStatusFormat: OFormat[Granted]                                          = Json.format[Granted]
  implicit val acceptedWithWarningsStatusFormat: OFormat[GrantedWithWarnings]                  = Json.format[GrantedWithWarnings]
  implicit val failedStatusFormat: OFormat[Failed]                                             = Json.format[Failed]
  implicit val warningsStatusFormat: OFormat[Warnings]                                         = Json.format[Warnings]
  implicit val pendingResponsibleIndividualStatusFormat: OFormat[PendingResponsibleIndividual] = Json.format[PendingResponsibleIndividual]
  implicit val submittedStatusFormat: OFormat[Submitted]                                       = Json.format[Submitted]
  implicit val answeringStatusFormat: OFormat[Answering]                                       = Json.format[Answering]
  implicit val createdStatusFormat: OFormat[Created]                                           = Json.format[Created]

  implicit val submissionStatus: OFormat[Submission.Status] = Union.from[Submission.Status]("Submission.StatusType")
    .and[Declined]("declined")
    .and[Granted]("granted")
    .and[GrantedWithWarnings]("grantedWithWarnings")
    .and[Failed]("failed")
    .and[Warnings]("warnings")
    .and[PendingResponsibleIndividual]("pendingResponsibleIndividual")
    .and[Submitted]("submitted")
    .and[Answering]("answering")
    .and[Created]("created")
    .format

  implicit val submissionInstanceFormat: Format[Submission.Instance] = Json.format[Submission.Instance]
  implicit val submissionFormat: Format[Submission]                  = Json.format[Submission]
  implicit val extendedSubmissionFormat: Format[ExtendedSubmission]  = Json.format[ExtendedSubmission]
  implicit val markedSubmissionFormat: Format[MarkedSubmission]      = Json.format[MarkedSubmission]
}

object SubmissionsJsonFormatters extends SubmissionsJsonFormatters
