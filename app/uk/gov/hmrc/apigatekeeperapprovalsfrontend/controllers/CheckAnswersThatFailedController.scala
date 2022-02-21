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

package uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.apiplatform.modules.stride.config.StrideAuthConfig
import uk.gov.hmrc.apiplatform.modules.stride.connectors.AuthConnector
import uk.gov.hmrc.apiplatform.modules.stride.controllers.actions.ForbiddenHandler
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.config.ErrorHandler
import scala.concurrent.ExecutionContext
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.ApplicationId
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.views.html.CheckAnswersThatFailedPage
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.services.ApplicationActionService
import uk.gov.hmrc.apiplatform.modules.submissions.domain.models._
import uk.gov.hmrc.apiplatform.modules.submissions.services.SubmissionService

import scala.concurrent.Future.successful
import uk.gov.hmrc.apiplatform.modules.submissions.domain.services.ActualAnswersAsText
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.services.SubmissionReviewService
import play.api.mvc._
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.SubmissionReview

object CheckAnswersThatFailedController {  
  case class AnswerDetails(question: String, answer: String, status: Mark)

  case class ViewModel(applicationId: ApplicationId, appName: String, answers: List[AnswerDetails]) {
    lazy val hasFails: Boolean = answers.exists(_.status == Fail)
    lazy val hasWarns: Boolean = answers.exists(_.status == Warn)
    lazy val messageKey: String = if (hasFails) { if (hasWarns) "failsandwarns" else "failsonly"} else "warnsonly"
  }
}

@Singleton
class CheckAnswersThatFailedController @Inject()(
  strideAuthConfig: StrideAuthConfig,
  authConnector: AuthConnector,
  forbiddenHandler: ForbiddenHandler,
  mcc: MessagesControllerComponents,
  errorHandler: ErrorHandler,
  submissionReviewService: SubmissionReviewService,
  checkAnswersThatFailedPage: CheckAnswersThatFailedPage,
  val applicationActionService: ApplicationActionService,
  val submissionService: SubmissionService

)(implicit override val ec: ExecutionContext) extends AbstractCheckController(strideAuthConfig, authConnector, forbiddenHandler, mcc, errorHandler, submissionReviewService) {
  
  import CheckAnswersThatFailedController._

  def page(applicationId: ApplicationId) = loggedInWithApplicationAndSubmission(applicationId) { implicit request =>
    val appName = request.application.name

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
          request.markedAnswers.getOrElse(question.id, Pass)
        )
    }
    .toList
    .filter(_.status != Pass)

    successful(
      Ok(
        checkAnswersThatFailedPage(
          ViewModel(
            applicationId,
            appName,
            answerDetails
          )
        )
      )
    )
  }

  def action(applicationId: ApplicationId): Action[AnyContent] = updateActionStatus(SubmissionReview.Action.CheckFailsAndWarnings)(applicationId)
}