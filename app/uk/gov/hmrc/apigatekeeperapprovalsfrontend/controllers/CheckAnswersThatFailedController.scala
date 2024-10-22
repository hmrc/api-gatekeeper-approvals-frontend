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

package uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext
import scala.concurrent.Future.successful

import play.api.mvc.{MessagesControllerComponents, _}

import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.ApplicationName
import uk.gov.hmrc.apiplatform.modules.common.domain.models.ApplicationId
import uk.gov.hmrc.apiplatform.modules.gkauth.services.StrideAuthorisationService
import uk.gov.hmrc.apiplatform.modules.submissions.domain.models._
import uk.gov.hmrc.apiplatform.modules.submissions.domain.services.ActualAnswersAsText
import uk.gov.hmrc.apiplatform.modules.submissions.services.SubmissionService

import uk.gov.hmrc.apigatekeeperapprovalsfrontend.config.ErrorHandler
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.SubmissionReview
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.services.{ApplicationActionService, SubmissionReviewService}
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.views.html.CheckAnswersThatFailedPage

object CheckAnswersThatFailedController {
  case class AnswerDetails(question: String, answer: String, status: Mark)

  case class ViewModel(applicationId: ApplicationId, appName: ApplicationName, answers: List[AnswerDetails], isDeleted: Boolean) {
    lazy val hasFails: Boolean = answers.exists(_.status == Mark.Fail)
    lazy val hasWarns: Boolean = answers.exists(_.status == Mark.Warn)

    lazy val messageKey: String = if (hasFails) { if (hasWarns) "failsandwarns" else "failsonly" }
    else "warnsonly"
  }
}

@Singleton
class CheckAnswersThatFailedController @Inject() (
    strideAuthorisationService: StrideAuthorisationService,
    mcc: MessagesControllerComponents,
    errorHandler: ErrorHandler,
    submissionReviewService: SubmissionReviewService,
    checkAnswersThatFailedPage: CheckAnswersThatFailedPage,
    val applicationActionService: ApplicationActionService,
    val submissionService: SubmissionService
  )(implicit override val ec: ExecutionContext
  ) extends AbstractCheckController(strideAuthorisationService, mcc, errorHandler, submissionReviewService) {

  import CheckAnswersThatFailedController._

  def page(applicationId: ApplicationId) = loggedInThruStrideWithApplicationAndSubmission(applicationId) { implicit request =>
    val appName   = request.application.name
    val isDeleted = request.application.state.isDeleted

    val questionsAndAnswers: Map[Question, ActualAnswer] =
      request.submission.latestInstance.answersToQuestions.map {
        case (questionId, answer) => (request.submission.findQuestion(questionId) -> answer)
      }
        .collect {
          case (q: Some[Question], a) => q.get -> a
        }

    val answerDetails = questionsAndAnswers.map {
      case (question, answer) =>
        AnswerDetails(
          question.wording.value,
          ActualAnswersAsText(answer),
          request.markedAnswers.getOrElse(question.id, Mark.Pass)
        )
    }
      .toList
      .filter(_.status != Mark.Pass)

    successful(
      Ok(
        checkAnswersThatFailedPage(
          ViewModel(
            applicationId,
            appName,
            answerDetails,
            isDeleted
          )
        )
      )
    )
  }

  def action(applicationId: ApplicationId): Action[AnyContent] = updateActionStatus(SubmissionReview.Action.CheckFailsAndWarnings)(applicationId)
}
