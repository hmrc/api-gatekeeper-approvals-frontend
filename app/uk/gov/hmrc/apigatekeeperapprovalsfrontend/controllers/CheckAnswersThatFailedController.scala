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
import uk.gov.hmrc.modules.stride.config.StrideAuthConfig
import uk.gov.hmrc.modules.stride.connectors.AuthConnector
import uk.gov.hmrc.modules.stride.controllers.actions.ForbiddenHandler
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.config.ErrorHandler
import scala.concurrent.ExecutionContext
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.actions.ApplicationActions
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.ApplicationId
import uk.gov.hmrc.modules.stride.controllers.GatekeeperBaseController
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents,Request}
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.views.html.CheckAnswersThatFailedPage
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.services.ApplicationActionService
import uk.gov.hmrc.modules.submissions.domain.models._
import uk.gov.hmrc.modules.submissions.services.SubmissionService

import scala.concurrent.Future.successful
import uk.gov.hmrc.modules.submissions.domain.services.ActualAnswersAsText

object CheckAnswersThatFailedController {  
  case class AnswerDetails(question: String, answer: String, status: Mark)

  case class ViewModel(applicationId: ApplicationId, appName: String, answers: List[AnswerDetails]) {
    lazy val hasFails: Boolean = answers.exists(_.status == Fail)
    lazy val hasWarns: Boolean = answers.exists(_.status == Warn)
    lazy val messageKey: String = if (hasFails) { if (hasWarns) "failsAndWarns" else "failsOnly"} else "warnsOnly"
  }
}

@Singleton
class CheckAnswersThatFailedController @Inject()(
  strideAuthConfig: StrideAuthConfig,
  authConnector: AuthConnector,
  forbiddenHandler: ForbiddenHandler,
  mcc: MessagesControllerComponents,
  checkAnswersThatFailedPage: CheckAnswersThatFailedPage,
  val errorHandler: ErrorHandler,
  val applicationActionService: ApplicationActionService,
  val submissionService: SubmissionService

)(implicit override val ec: ExecutionContext) extends GatekeeperBaseController(strideAuthConfig, authConnector, forbiddenHandler, mcc) with ApplicationActions {
  import CheckAnswersThatFailedController._

  implicit override def hc(implicit request: Request[_]): HeaderCarrier =
    HeaderCarrierConverter.fromRequestAndSession(request, request.session)


  def checkAnswersThatFailedPage(applicationId: ApplicationId): Action[AnyContent] = loggedInWithApplicationAndSubmission(applicationId) { implicit request =>
    val appName = request.application.name

    val questionsAndAnswers: Map[Question, ActualAnswer] = 
      request.submission.answersToQuestions.map {
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

  def checkAnswersThatFailedAction(applicationId: ApplicationId): Action[AnyContent] = loggedInWithApplicationAndSubmission(applicationId) { implicit request =>
    def handleAction(action: String) = action match {
      case "checked"          => Redirect(uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.routes.ApplicationController.applicationPage(applicationId)) // TODO: Add actual route when implementing button actions
      case "come-back-later"  => Redirect(uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.routes.ApplicationController.applicationPage(applicationId)) // TODO: Add actual route when implementing button actions
      case _                  => BadRequest(errorHandler.badRequestTemplate)
    }

    request.body.asFormUrlEncoded.getOrElse(Map.empty).get("submit-action") match {
      case Some(value) => successful(value.headOption.fold(BadRequest(errorHandler.badRequestTemplate))(handleAction(_)))
      case None => successful(BadRequest(errorHandler.badRequestTemplate))
    }
  }
}