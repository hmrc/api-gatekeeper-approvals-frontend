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

import cats.data.{EitherT, NonEmptyList}

import play.api.data.Form
import play.api.data.Forms.*
import play.api.mvc.{MessagesControllerComponents, *}

import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.*
import uk.gov.hmrc.apiplatform.modules.commands.applications.domain.models.{CommandFailure, CommandFailures}
import uk.gov.hmrc.apiplatform.modules.common.domain.models.ApplicationId
import uk.gov.hmrc.apiplatform.modules.gkauth.services.StrideAuthorisationService
import uk.gov.hmrc.apiplatform.modules.submissions.domain.models.*
import uk.gov.hmrc.apiplatform.modules.submissions.domain.services.ActualAnswersAsText
import uk.gov.hmrc.apiplatform.modules.submissions.services.SubmissionService

import uk.gov.hmrc.apigatekeeperapprovalsfrontend.config.ErrorHandler
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.services.{ApplicationActionService, SubmissionReviewService}
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.views.html.*

object TermsOfUseFailedJourneyController {

  case class ViewModel(applicationId: ApplicationId, appName: ApplicationName)

  case class OverrideViewModel(applicationId: ApplicationId, appName: ApplicationName, escalatedTo: String, notes: String)

  case class AnswerDetails(question: String, answer: String, status: Mark)

  case class AnswersViewModel(applicationId: ApplicationId, appName: ApplicationName, answers: List[AnswerDetails], isDeleted: Boolean, submissionStatus: Submission.Status) {
    lazy val hasFails: Boolean = answers.exists(_.status == Mark.Fail)
    lazy val hasWarns: Boolean = answers.exists(_.status == Mark.Warn)

    lazy val messageKey: String = if (hasFails) { if (hasWarns) "failsandwarns" else "failsonly" }
    else "warnsonly"
  }

  case class EmailsViewModel(applicationId: ApplicationId, appName: ApplicationName, adminsToEmail: Set[Collaborator] = Set.empty)

  case class ApproverForm(firstName: String, lastName: String)

  val approverForm: Form[ApproverForm] = Form(
    mapping(
      "first-name" -> nonEmptyText,
      "last-name"  -> nonEmptyText
    )(ApproverForm.apply)(a => Some(a.firstName, a.lastName))
  )

  case class ProvideNotesForm(notes: String)

  val provideNotesForm: Form[ProvideNotesForm] = Form(
    mapping(
      "notes" -> nonEmptyText
    )(ProvideNotesForm.apply)(p => Some(p.notes))
  )
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
    termsOfUseFailOverridePage: TermsOfUseFailOverridePage,
    termsOfUseOverrideApproverPage: TermsOfUseOverrideApproverPage,
    termsOfUseOverrideNotesPage: TermsOfUseOverrideNotesPage,
    termsOfUseOverrideConfirmPage: TermsOfUseOverrideConfirmPage,
    val applicationActionService: ApplicationActionService,
    val submissionService: SubmissionService
  )(implicit override val ec: ExecutionContext
  ) extends AbstractCheckController(strideAuthorisationService, mcc, errorHandler, submissionReviewService) {

  import TermsOfUseFailedJourneyController.*

  private def setupSubmissionReview(submission: Submission, isSuccessful: Boolean, hasWarnings: Boolean) = {
    submissionReviewService.findOrCreateReview(submission.id, submission.latestInstance.index, isSuccessful, hasWarnings, false, false)
  }

  def listPage(rawApplicationId: java.util.UUID): Action[AnyContent] = loggedInThruStrideWithApplicationAndSubmission(rawApplicationId) { implicit request =>
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

    val isSuccessful = !request.markedSubmission.isFail
    val hasWarnings  = request.markedSubmission.isWarn

    for {
      _ <- setupSubmissionReview(request.submission, isSuccessful, hasWarnings)
    } yield Ok(
      termsOfUseFailedListPage(
        AnswersViewModel(
          request.application.id,
          appName,
          answerDetails,
          isDeleted,
          request.submission.latestInstance.status
        )
      )
    )
  }

  def listAction(rawApplicationId: java.util.UUID): Action[AnyContent] = loggedInThruStrideWithApplicationAndSubmission(rawApplicationId) { implicit request =>
    request.body.asFormUrlEncoded.getOrElse(Map.empty).get("submit-action").flatMap(_.headOption) match {
      case Some("continue") => successful(Redirect(routes.TermsOfUseReasonsController.provideReasonsPage(rawApplicationId)))
      case _                => successful(Redirect(routes.TermsOfUseInvitationController.page))
    }
  }

  def answersWithWarningsOrFails(rawApplicationId: java.util.UUID): Action[AnyContent] = loggedInThruStrideWithApplicationAndSubmission(rawApplicationId) { implicit request =>
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

    successful(Ok(termsOfUseFailedPage(
      AnswersViewModel(
        request.application.id,
        appName,
        answerDetails,
        isDeleted,
        request.submission.latestInstance.status
      )
    )))
  }

  def emailAddressesPage(rawApplicationId: java.util.UUID): Action[AnyContent] = loggedInThruStrideWithApplicationAndSubmission(rawApplicationId) { implicit request =>
    val adminsToEmail = request.application.collaborators.filter(_.role.isAdministrator)

    successful(Ok(termsOfUseAdminsPage(EmailsViewModel(request.application.id, request.application.name, adminsToEmail))))
  }

  def emailAddressesAction(rawApplicationId: java.util.UUID): Action[AnyContent] = loggedInThruStrideWithApplicationAndSubmission(rawApplicationId) { implicit request =>
    val ok = Redirect(routes.TermsOfUseFailedJourneyController.confirmationPage(rawApplicationId))

    for {
      review <- submissionReviewService.findOrCreateReview(
                  request.submission.id,
                  request.submission.latestInstance.index,
                  !request.markedSubmission.isFail,
                  request.markedSubmission.isWarn,
                  false,
                  false
                )
      _      <- submissionService.grantWithWarningsOrDeclineForTouUplift(request.application.id, request.submission, request.name.get, review.grantWarnings)
    } yield ok
  }

  def failOverridePage(rawApplicationId: java.util.UUID): Action[AnyContent] = loggedInThruStrideWithApplicationAndSubmission(rawApplicationId) { implicit request =>
    successful(Ok(termsOfUseFailOverridePage(ViewModel(request.application.id, request.application.name))))
  }

  def failOverrideAction(rawApplicationId: java.util.UUID): Action[AnyContent] = loggedInThruStrideWithApplicationAndSubmission(rawApplicationId) { implicit request =>
    request.body.asFormUrlEncoded.getOrElse(Map.empty).get("override").flatMap(_.headOption) match {
      case Some("yes") => successful(Redirect(routes.TermsOfUseFailedJourneyController.overrideApproverPage(rawApplicationId)))
      case _           => successful(Redirect(routes.TermsOfUseFailedJourneyController.listPage(rawApplicationId)))
    }
  }

  def overrideApproverPage(rawApplicationId: java.util.UUID): Action[AnyContent] = loggedInThruStrideWithApplicationAndSubmission(rawApplicationId) { implicit request =>
    successful(Ok(termsOfUseOverrideApproverPage(approverForm, ViewModel(request.application.id, request.application.name))))
  }

  def overrideApproverAction(rawApplicationId: java.util.UUID): Action[AnyContent] = loggedInThruStrideWithApplicationAndSubmission(rawApplicationId) { implicit request =>
    def handleValidForm(form: ApproverForm) = {
      submissionReviewService.updateEscalatedTo(form.firstName + " " + form.lastName)(request.submission.id, request.submission.latestInstance.index).flatMap {
        case Some(value) => successful(Redirect(routes.TermsOfUseFailedJourneyController.overrideNotesPage(rawApplicationId)))
        case None        => {
          logger.warn(s"Failed to save escalated to in submission review for applicationId: ${request.application.id}")
          errorHandler.badRequestTemplate.map(BadRequest(_))
        }
      }
    }

    def handleInvalidForm(form: Form[ApproverForm]) = {
      successful(BadRequest(termsOfUseOverrideApproverPage(form, ViewModel(request.application.id, request.application.name))))
    }

    TermsOfUseFailedJourneyController.approverForm.bindFromRequest().fold(handleInvalidForm, handleValidForm)
  }

  def overrideNotesPage(rawApplicationId: java.util.UUID): Action[AnyContent] = loggedInThruStrideWithApplicationAndSubmission(rawApplicationId) { implicit request =>
    successful(Ok(termsOfUseOverrideNotesPage(provideNotesForm, ViewModel(request.application.id, request.application.name))))
  }

  def overrideNotesAction(rawApplicationId: java.util.UUID): Action[AnyContent] = loggedInThruStrideWithApplicationAndSubmission(rawApplicationId) { implicit request =>
    def handleValidForm(form: ProvideNotesForm) = {
      submissionReviewService.updateGrantWarnings(form.notes)(request.submission.id, request.submission.latestInstance.index).flatMap {
        case Some(value) => successful(Redirect(routes.TermsOfUseFailedJourneyController.overrideConfirmPage(rawApplicationId)))
        case None        => {
          logger.warn(s"Failed to save reasons in submission review for applicationId: ${request.application.id}")
          errorHandler.badRequestTemplate.map(BadRequest(_))
        }
      }
    }

    def handleInvalidForm(form: Form[ProvideNotesForm]) = {
      successful(BadRequest(termsOfUseOverrideNotesPage(form, ViewModel(request.application.id, request.application.name))))
    }

    TermsOfUseFailedJourneyController.provideNotesForm.bindFromRequest().fold(handleInvalidForm, handleValidForm)
  }

  def overrideConfirmPage(rawApplicationId: java.util.UUID): Action[AnyContent] = loggedInThruStrideWithApplicationAndSubmission(rawApplicationId) { implicit request =>
    (
      for {
        review      <- fromOptionF(
                         submissionReviewService.findReview(request.submission.id, request.submission.latestInstance.index),
                         BadRequest("Unable to find submission review")
                       )
        escalatedTo <- fromOption(
                         review.escalatedTo,
                         BadRequest("Unable to get escalatedTo in submission review")
                       )
      } yield Ok(termsOfUseOverrideConfirmPage(OverrideViewModel(request.application.id, request.application.name, escalatedTo, review.grantWarnings)))
    ).fold(identity(_), identity(_))
  }

  def overrideConfirmAction(rawApplicationId: java.util.UUID): Action[AnyContent] = loggedInThruStrideWithApplicationAndSubmission(rawApplicationId) { implicit request =>
    def handleCommandFailures(failures: NonEmptyList[CommandFailure]): Result = {
      val errString = failures.toList.map(error => CommandFailures.describe(error)).mkString(", ")
      InternalServerError(errString)
    }
    (
      for {
        review <- fromOptionF(
                    submissionReviewService.findReview(request.submission.id, request.submission.latestInstance.index),
                    BadRequest("Unable to find submission review")
                  )
        _      <- EitherT(submissionService.grantForTouUplift(request.application.id, request.name.get, review.grantWarnings, review.escalatedTo)).leftMap(handleCommandFailures)
      } yield Redirect(routes.TermsOfUseGrantedConfirmationController.page(rawApplicationId).url)
    ).fold(identity(_), identity(_))
  }

  def confirmationPage(rawApplicationId: java.util.UUID): Action[AnyContent] = loggedInThruStrideWithApplicationAndSubmission(rawApplicationId) { implicit request =>
    successful(Ok(termsOfUseConfirmationPage(ViewModel(request.application.id, request.application.name))))
  }
}
