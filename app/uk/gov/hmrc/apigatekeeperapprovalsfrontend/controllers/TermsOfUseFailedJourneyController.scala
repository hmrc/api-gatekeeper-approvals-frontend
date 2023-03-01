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
import uk.gov.hmrc.apiplatform.modules.gkauth.services.StrideAuthorisationService
import uk.gov.hmrc.apiplatform.modules.submissions.domain.models._
import uk.gov.hmrc.apiplatform.modules.submissions.domain.services.ActualAnswersAsText
import uk.gov.hmrc.apiplatform.modules.submissions.services.SubmissionService

import uk.gov.hmrc.apigatekeeperapprovalsfrontend.config.ErrorHandler
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.{ApplicationId, Collaborator, CollaboratorRole, State}
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.services.{ApplicationActionService, SubmissionReviewService}
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.views.html.{TermsOfUseAdminsPage, TermsOfUseConfirmationPage, TermsOfUseFailedListPage, TermsOfUseFailedPage}

object TermsOfUseFailedJourneyController {
  case class AnswerDetails(question: String, answer: String, status: Mark)

  case class ViewModel(applicationId: ApplicationId, appName: String, answers: List[AnswerDetails], isDeleted: Boolean) {
    lazy val hasFails: Boolean = answers.exists(_.status == Fail)
    lazy val hasWarns: Boolean = answers.exists(_.status == Warn)

    lazy val messageKey: String = if (hasFails) { if (hasWarns) "failsandwarns" else "failsonly" }
    else "warnsonly"
  }

  case class EmailsViewModel(applicationId: ApplicationId, appName: String, adminsToEmail: Set[Collaborator] = Set.empty)

  case class ConfirmationViewModel(applicationId: ApplicationId, appName: String)
}

@Singleton
class TermsOfUseFailedJourneyController @Inject() (
    strideAuthorisationService: StrideAuthorisationService,
    mcc: MessagesControllerComponents,
    errorHandler: ErrorHandler,
    submissionReviewService: SubmissionReviewService,
    termsOfUseFailedListPage: TermsOfUseFailedListPage,
    termsOfUseFailedPage: TermsOfUseFailedPage,
    termsOfUseAdminsPage: TermsOfUseAdminsPage,
    termsOfUseConfirmationPage: TermsOfUseConfirmationPage,
    val applicationActionService: ApplicationActionService,
    val submissionService: SubmissionService
  )(implicit override val ec: ExecutionContext
  ) extends AbstractCheckController(strideAuthorisationService, mcc, errorHandler, submissionReviewService) {

  import TermsOfUseFailedJourneyController._

  private def setupSubmissionReview(submission: Submission, isSuccessful: Boolean, hasWarnings: Boolean) = {
    submissionReviewService.findOrCreateReview(submission.id, submission.latestInstance.index, isSuccessful, hasWarnings, false, false)
  }

  def listPage(applicationId: ApplicationId) = loggedInThruStrideWithApplicationAndSubmission(applicationId) { implicit request =>
    val appName   = request.application.name
    val isDeleted = request.application.state.name == State.DELETED

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

    val isSuccessful = !request.markedSubmission.isFail
    val hasWarnings  = request.markedSubmission.isWarn

    for {
      review <- setupSubmissionReview(request.submission, isSuccessful, hasWarnings)
    } yield Ok(
      termsOfUseFailedListPage(
        ViewModel(
          applicationId,
          appName,
          answerDetails,
          isDeleted
        )
      )
    )
  }

  def listAction(applicationId: ApplicationId): Action[AnyContent] = loggedInThruStrideWithApplicationAndSubmission(applicationId) { implicit request =>
    request.body.asFormUrlEncoded.getOrElse(Map.empty).get("submit-action").flatMap(_.headOption) match {
      case Some("continue") => successful(Redirect(uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.routes.TermsOfUseReasonsController.provideReasonsPage(applicationId)))
      case _                => successful(Redirect(uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.routes.TermsOfUseInvitationController.page))
    }
  }

  def answersWithWarningsOrFails(applicationId: ApplicationId) = loggedInThruStrideWithApplicationAndSubmission(applicationId) { implicit request =>
    val appName   = request.application.name
    val isDeleted = request.application.state.name == State.DELETED

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

    successful(Ok(termsOfUseFailedPage(
      ViewModel(
        applicationId,
        appName,
        answerDetails,
        isDeleted
      )
    )))
  }

  def emailAddressesPage(applicationId: ApplicationId) = loggedInThruStrideWithApplicationAndSubmission(applicationId) { implicit request =>
    val adminsToEmail = request.application.collaborators.filter(_.role.is(CollaboratorRole.ADMINISTRATOR))

    successful(Ok(termsOfUseAdminsPage(EmailsViewModel(applicationId, request.application.name, adminsToEmail))))
  }

  def emailAddressesAction(applicationId: ApplicationId) = loggedInThruStrideWithApplicationAndSubmission(applicationId) { implicit request =>
    val ok = Redirect(uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.routes.TermsOfUseFailedJourneyController.confirmationPage(applicationId))

    for {
      review <- submissionReviewService.findOrCreateReview(
                  request.submission.id,
                  request.submission.latestInstance.index,
                  !request.markedSubmission.isFail,
                  request.markedSubmission.isWarn,
                  false,
                  false
                )
      _      <- submissionService.grantOrDeclineForTouUplift(applicationId, request.submission, request.name.get, review.grantWarnings)
    } yield ok
  }

  def confirmationPage(applicationId: ApplicationId) = loggedInThruStrideWithApplicationAndSubmission(applicationId) { implicit request =>
    successful(Ok(termsOfUseConfirmationPage(ConfirmationViewModel(applicationId, request.application.name))))
  }
}
