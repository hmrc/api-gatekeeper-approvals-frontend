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

import cats.data.NonEmptyList

import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.apiplatform.modules.gkauth.services.StrideAuthorisationService
import uk.gov.hmrc.apiplatform.modules.submissions.domain.models._
import uk.gov.hmrc.apiplatform.modules.submissions.domain.services.ActualAnswersAsText
import uk.gov.hmrc.apiplatform.modules.submissions.services.SubmissionService

import uk.gov.hmrc.apigatekeeperapprovalsfrontend.config.ErrorHandler
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.{ApplicationId, State}
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.services.{ApplicationActionService, SubmissionReviewService}
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.views.html.TermsOfUsePassedPage

object TermsOfUsePassedController {
  case class AnswerDetails(question: String, answer: String)
  case class Section(heading: String, answerDetails: NonEmptyList[AnswerDetails])
  case class ViewModel(applicationId: ApplicationId, appName: String, sections: List[Section], isDeleted: Boolean)
}

@Singleton
class TermsOfUsePassedController @Inject() (
    strideAuthorisationService: StrideAuthorisationService,
    mcc: MessagesControllerComponents,
    errorHandler: ErrorHandler,
    submissionReviewService: SubmissionReviewService,
    termsOfUsePassedPage: TermsOfUsePassedPage,
    val applicationActionService: ApplicationActionService,
    val submissionService: SubmissionService
  )(implicit override val ec: ExecutionContext
  ) extends AbstractCheckController(strideAuthorisationService, mcc, errorHandler, submissionReviewService) {

  import TermsOfUsePassedController._

  def answersThatPassedPage(applicationId: ApplicationId): Action[AnyContent] = loggedInThruStrideWithApplicationAndSubmission(applicationId) { implicit request =>
    def isPass(id: Question.Id): Boolean = {
      request.markedAnswers.get(id).map(_ == Pass).getOrElse(false)
    }
    val isDeleted                        = request.application.state.name == State.DELETED

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
          case (heading, head :: tail) => Section(heading, NonEmptyList.of(head, tail: _*))
        }

    successful(
      Ok(
        termsOfUsePassedPage(
          TermsOfUsePassedController.ViewModel(
            applicationId,
            request.application.name,
            groupedPassedQuestionsIds,
            isDeleted
          )
        )
      )
    )
  }
}
