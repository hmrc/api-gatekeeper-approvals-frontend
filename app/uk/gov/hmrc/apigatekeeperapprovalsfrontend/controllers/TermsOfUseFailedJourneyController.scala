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

import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc.{MessagesControllerComponents, _}
import uk.gov.hmrc.apiplatform.modules.gkauth.services.StrideAuthorisationService
import uk.gov.hmrc.apiplatform.modules.submissions.domain.models._
import uk.gov.hmrc.apiplatform.modules.submissions.domain.services.ActualAnswersAsText
import uk.gov.hmrc.apiplatform.modules.submissions.services.SubmissionService

import uk.gov.hmrc.apigatekeeperapprovalsfrontend.config.ErrorHandler
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.{ApplicationId, Collaborator, CollaboratorRole, State}
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.services.{ApplicationActionService, SubmissionReviewService}
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.views.html.{TermsOfUseAdminsPage, TermsOfUseConfirmationPage, TermsOfUseFailedListPage, TermsOfUseFailOverridePage, TermsOfUseFailedPage, TermsOfUseGrantedConfirmationPage, TermsOfUseOverrideApproverPage, TermsOfUseOverrideNotesPage}

object TermsOfUseFailedJourneyController {
  case class AnswerDetails(question: String, answer: String, status: Mark)

  case class ViewModel(applicationId: ApplicationId, appName: String, answers: List[AnswerDetails], isDeleted: Boolean) {
    lazy val hasFails: Boolean = answers.exists(_.status == Fail)
    lazy val hasWarns: Boolean = answers.exists(_.status == Warn)

    lazy val messageKey: String = if (hasFails) { if (hasWarns) "failsandwarns" else "failsonly" }
    else "warnsonly"
  }

  case class EmailsViewModel(applicationId: ApplicationId, appName: String, adminsToEmail: Set[Collaborator] = Set.empty)

  case class SummaryViewModel(applicationId: ApplicationId, appName: String)

  case class ApproverForm(firstName: String, lastName: String)

  val approverForm: Form[ApproverForm] = Form(
    mapping(
      "first-name" -> nonEmptyText,
      "last-name" -> nonEmptyText
    )(ApproverForm.apply)(ApproverForm.unapply)
  )  

  case class ProvideNotesForm(notes: String)

  val provideNotesForm: Form[ProvideNotesForm] = Form(
    mapping(
      "notes" -> nonEmptyText
    )(ProvideNotesForm.apply)(ProvideNotesForm.unapply)
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
    termsOfUseGrantedConfirmationPage: TermsOfUseGrantedConfirmationPage,
    termsOfUseOverrideNotesPage: TermsOfUseOverrideNotesPage,
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
      _      <- submissionService.grantWithWarningsOrDeclineForTouUplift(applicationId, request.submission, request.name.get, review.grantWarnings)
    } yield ok
  }

  def failOverridePage(applicationId: ApplicationId) = loggedInThruStrideWithApplicationAndSubmission(applicationId) { implicit request =>
    successful(Ok(termsOfUseFailOverridePage(SummaryViewModel(applicationId, request.application.name))))
  }

  def failOverrideAction(applicationId: ApplicationId) = loggedInThruStrideWithApplicationAndSubmission(applicationId) { implicit request =>
    request.body.asFormUrlEncoded.getOrElse(Map.empty).get("override").flatMap(_.headOption) match {
      case Some("yes") => successful(Redirect(uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.routes.TermsOfUseFailedJourneyController.overrideApproverPage(applicationId)))
      case _           => successful(Redirect(uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.routes.TermsOfUseFailedJourneyController.listPage(applicationId)))
    }
  }

  def overrideApproverPage(applicationId: ApplicationId) = loggedInThruStrideWithApplicationAndSubmission(applicationId) { implicit request =>
    successful(Ok(termsOfUseOverrideApproverPage(approverForm, SummaryViewModel(applicationId, request.application.name))))
  }

  def overrideApproverAction(applicationId: ApplicationId) = loggedInThruStrideWithApplicationAndSubmission(applicationId) { implicit request =>
    def handleValidForm(form: ApproverForm) = {
      submissionReviewService.updateEscalatedTo(form.firstName + " " + form.lastName)(request.submission.id, request.submission.latestInstance.index).map {
        case Some(value) => Redirect(uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.routes.TermsOfUseFailedJourneyController.overrideNotesPage(applicationId))
        case None        => {
          logger.warn("Persisting override escalated to failed")
          BadRequest(errorHandler.badRequestTemplate)
        }
      }
    }

    def handleInvalidForm(form: Form[ApproverForm]) = {
      successful(BadRequest(termsOfUseOverrideApproverPage(form, SummaryViewModel(applicationId, request.application.name))))
    }

    TermsOfUseFailedJourneyController.approverForm.bindFromRequest().fold(handleInvalidForm, handleValidForm)
  }

  def overrideNotesPage(applicationId: ApplicationId) = loggedInThruStrideWithApplicationAndSubmission(applicationId) { implicit request =>
    successful(Ok(termsOfUseOverrideNotesPage(provideNotesForm, SummaryViewModel(applicationId, request.application.name))))
  }

  def overrideNotesAction(applicationId: ApplicationId) = loggedInThruStrideWithApplicationAndSubmission(applicationId) { implicit request =>
    def handleValidForm(form: ProvideNotesForm) = {
      submissionReviewService.updateGrantWarnings(form.notes)(request.submission.id, request.submission.latestInstance.index).map {
        case Some(value) => Redirect(uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.routes.TermsOfUseFailedJourneyController.overrideNotesPage(applicationId))
        case None        => {
          logger.warn("Persisting override reasons failed")
          BadRequest(errorHandler.badRequestTemplate)
        }
      }
    }

    def handleInvalidForm(form: Form[ProvideNotesForm]) = {
      successful(BadRequest(termsOfUseOverrideNotesPage(form, SummaryViewModel(applicationId, request.application.name))))
    }

    TermsOfUseFailedJourneyController.provideNotesForm.bindFromRequest().fold(handleInvalidForm, handleValidForm)
  }

  def confirmationPage(applicationId: ApplicationId) = loggedInThruStrideWithApplicationAndSubmission(applicationId) { implicit request =>
    successful(Ok(termsOfUseConfirmationPage(SummaryViewModel(applicationId, request.application.name))))
  }
}
