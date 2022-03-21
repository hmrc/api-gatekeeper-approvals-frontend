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
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.ApplicationId
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import scala.concurrent.ExecutionContext
import uk.gov.hmrc.apiplatform.modules.stride.config.StrideAuthConfig
import uk.gov.hmrc.apiplatform.modules.stride.controllers.actions.ForbiddenHandler
import uk.gov.hmrc.apiplatform.modules.stride.connectors.AuthConnector
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.config.ErrorHandler
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.services.ApplicationActionService
import uk.gov.hmrc.apiplatform.modules.submissions.services.SubmissionService
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.views.html.CheckAnswersThatPassedPage
import scala.concurrent.Future.successful
import uk.gov.hmrc.apiplatform.modules.submissions.domain.models._
import uk.gov.hmrc.apiplatform.modules.submissions.domain.services.ActualAnswersAsText
import cats.data.NonEmptyList
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.services.SubmissionReviewService
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.SubmissionReview

object CheckAnswersThatPassedController {  
  case class AnswerDetails(question: String, answer: String)
  case class Section(heading: String, answerDetails: NonEmptyList[AnswerDetails] )
  case class ViewModel(applicationId: ApplicationId, appName: String, sections: List[Section])
}

@Singleton
class CheckAnswersThatPassedController @Inject()(
  strideAuthConfig: StrideAuthConfig,
  authConnector: AuthConnector,
  forbiddenHandler: ForbiddenHandler,
  mcc: MessagesControllerComponents,
  errorHandler: ErrorHandler,
  submissionReviewService: SubmissionReviewService,
  checkAnswersThatPassedPage: CheckAnswersThatPassedPage,
  val applicationActionService: ApplicationActionService,
  val submissionService: SubmissionService
)(implicit override val ec: ExecutionContext) extends AbstractCheckController(strideAuthConfig, authConnector, forbiddenHandler, mcc, errorHandler, submissionReviewService) {

  import CheckAnswersThatPassedController._

  def checkAnswersThatPassedPage(applicationId: ApplicationId): Action[AnyContent] = loggedInWithApplicationAndSubmission(applicationId) { implicit request =>
    def isPass(id: Question.Id): Boolean = {
      request.markedAnswers.get(id).map(_ == Pass).getOrElse(false)
    }

    val groupedPassedQuestionsIds = 
      request.submission.groups
      .map(group =>
        (
          group.heading,
          group.links
            .flatMap(l => l.questions.map(_.question))
            .filter(q => isPass(q.id))
            .map(q => (q, request.answersToQuestions.get(q.id)))
            .collect {
              case (question, Some(answer)) => AnswerDetails(question.wording.value, ActualAnswersAsText(answer))
            }
        )
      )
      .collect {
        case (heading, head::tail) => Section(heading, NonEmptyList.of(head, tail:_*))
      }

    successful(
      Ok(
        checkAnswersThatPassedPage(
          CheckAnswersThatPassedController.ViewModel(
            applicationId,
            request.application.name,
            groupedPassedQuestionsIds
          )
        )
      )
    )
  }

  
  def checkAnswersThatPassedAction(applicationId: ApplicationId): Action[AnyContent] = updateActionStatus(SubmissionReview.Action.CheckPassedAnswers)(applicationId)
}
