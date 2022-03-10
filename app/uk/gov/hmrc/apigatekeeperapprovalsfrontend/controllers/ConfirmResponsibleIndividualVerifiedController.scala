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
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.views.html.ConfirmResponsibleIndividualVerified1Page
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.views.html.ConfirmResponsibleIndividualVerified2Page
import scala.concurrent.Future.successful
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.Standard
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.services.SubmissionReviewService
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.SubmissionReview
import org.joda.time.DateTime

object ConfirmResponsibleIndividualVerifiedController {  
  case class ViewModel1(appName: String, applicationId: ApplicationId, verified: Option[Boolean], errors: Option[String] = None) {
  }

  case class ViewModel2(appName: String, applicationId: ApplicationId, day: Option[String], month: Option[String], year: Option[String], errors: Option[String] = None) {
  }
}

@Singleton
class ConfirmResponsibleIndividualVerifiedController @Inject()(
  strideAuthConfig: StrideAuthConfig,
  authConnector: AuthConnector,
  forbiddenHandler: ForbiddenHandler,
  mcc: MessagesControllerComponents,
  errorHandler: ErrorHandler,
  submissionReviewService: SubmissionReviewService,
  confirmResponsibleIndividualVerified1Page: ConfirmResponsibleIndividualVerified1Page,
  confirmResponsibleIndividualVerified2Page: ConfirmResponsibleIndividualVerified2Page,
  val applicationActionService: ApplicationActionService,
  val submissionService: SubmissionService
)(implicit override val ec: ExecutionContext) extends AbstractCheckController(strideAuthConfig, authConnector, forbiddenHandler, mcc, errorHandler, submissionReviewService) {

  import ConfirmResponsibleIndividualVerifiedController._

  def page1(applicationId: ApplicationId): Action[AnyContent] = loggedInWithApplicationAndSubmission(applicationId) { implicit request =>

    def getVerified(submissionReview: SubmissionReview): Option[Boolean] = {
      submissionReview.verifiedByDetails.fold(Option.empty[Boolean])(vbd => Some(vbd.verified))
    } 

    def gotoPage1() = {
      val log = logBadRequest(SubmissionReview.Action.ConfirmResponsibleIndividualVerified) _
      (
        for {
          submissionReview <- fromOptionF(submissionReviewService.findReview(request.submission.id, request.submission.latestInstance.index), log("No submission review found"))
        } yield Ok(
                confirmResponsibleIndividualVerified1Page(
                  ViewModel1(
                    request.application.name,
                    applicationId,
                    getVerified(submissionReview)
                  )
                )
        )
      ).fold(identity(_), identity(_))  
    }

    // Should only be uplifting and checking Standard apps
    (request.application.access) match {
      case (std: Standard) if(request.submission.status.isSubmitted) =>
        gotoPage1()
 
      case _ => successful(BadRequest(errorHandler.badRequestTemplate))
    }
  }

  def page2(applicationId: ApplicationId): Action[AnyContent] = loggedInWithApplicationAndSubmission(applicationId) { implicit request =>

    def getVerifiedTimestamp(submissionReview: SubmissionReview): Option[DateTime] = {
      submissionReview.verifiedByDetails.fold(Option.empty[DateTime])(vbd => vbd.timestamp.fold(Option.empty[DateTime])(tim => Some(tim)))
    } 

    def getVerifiedDay(submissionReview: SubmissionReview): Option[String] = {
      getVerifiedTimestamp(submissionReview).fold(Option.empty[String])(tim => Some(String.valueOf(tim.getDayOfMonth())))
    } 

    def getVerifiedMonth(submissionReview: SubmissionReview): Option[String] = {
      getVerifiedTimestamp(submissionReview).fold(Option.empty[String])(tim => Some(String.valueOf(tim.getMonthOfYear())))
    } 

    def getVerifiedYear(submissionReview: SubmissionReview): Option[String] = {
      getVerifiedTimestamp(submissionReview).fold(Option.empty[String])(tim => Some(String.valueOf(tim.getYear())))
    } 

    def gotoPage2() = {
      val log = logBadRequest(SubmissionReview.Action.ConfirmResponsibleIndividualVerified) _
      (
        for {
          submissionReview <- fromOptionF(submissionReviewService.findReview(request.submission.id, request.submission.latestInstance.index), log("No submission review found"))
        } yield Ok(
                confirmResponsibleIndividualVerified2Page(
                  ViewModel2(
                    request.application.name,
                    applicationId,
                    getVerifiedDay(submissionReview),
                    getVerifiedMonth(submissionReview),
                    getVerifiedYear(submissionReview)
                  )
                )
        )
      ).fold(identity(_), identity(_))  
    }

    // Should only be uplifting and checking Standard apps
    (request.application.access) match {
      case (std: Standard) if(request.submission.status.isSubmitted) =>
        gotoPage2()
 
      case _ => successful(BadRequest(errorHandler.badRequestTemplate))
    }
  }

  private def getVerifiedByDetails(verifyAnswer: String, submissionReview: SubmissionReview) = {
    val verifyAnswerBoolean: Boolean = verifyAnswer == "yes"
    if (verifyAnswerBoolean) {
      val timestamp = submissionReview.verifiedByDetails.fold(Option.empty[DateTime])(vbd => vbd.timestamp)
      SubmissionReview.VerifiedByDetails(verifyAnswerBoolean, timestamp)
    } else {
      SubmissionReview.VerifiedByDetails(verifyAnswerBoolean)
    }
  }

  def action1(applicationId: ApplicationId): Action[AnyContent] = loggedInWithApplicationAndSubmission(applicationId) { implicit request =>
    lazy val success = (verifiedByDetails: SubmissionReview.VerifiedByDetails) => if(verifiedByDetails.verified)
        Redirect(uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.routes.ConfirmResponsibleIndividualVerifiedController.page2(applicationId))
      else
        Redirect(uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.routes.ChecklistController.checklistPage(applicationId))
    val checklist = Redirect(uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.routes.ChecklistController.checklistPage(applicationId))
    val log = logBadRequest(SubmissionReview.Action.ConfirmResponsibleIndividualVerified) _
    val failed = (msg: String) => {
      BadRequest(confirmResponsibleIndividualVerified1Page(ViewModel1(request.application.name, applicationId, None, Some(msg))))
    }

    def saveAndContinue() = {
      (
        for {
          submissionReview  <- fromOptionF(submissionReviewService.findReview(request.submission.id, request.submission.latestInstance.index), log("No submission review found"))
          verifyAnswer      <- fromOption(request.body.asFormUrlEncoded.getOrElse(Map.empty).get("verify-answer").flatMap(_.headOption), failed("Please provide an answer to the question"))
          verifiedByDetails =  getVerifiedByDetails(verifyAnswer, submissionReview)
          _                 <- fromOptionF(submissionReviewService.updateVerifiedByDetails(verifiedByDetails)(request.submission.id, request.submission.latestInstance.index), log("Failed to find existing review"))
          _                 <- fromOptionF(submissionReviewService.updateActionStatus(SubmissionReview.Action.ConfirmResponsibleIndividualVerified, SubmissionReview.Status.Completed)(request.submission.id, request.submission.latestInstance.index), log("Failed to find existing review"))
        } yield success(verifiedByDetails)
      ).fold(identity(_), identity(_))
    }

    def comeBackLater() = {
      (
        for {
          _                 <- fromOptionF(submissionReviewService.updateActionStatus(SubmissionReview.Action.ConfirmResponsibleIndividualVerified, SubmissionReview.Status.InProgress)(request.submission.id, request.submission.latestInstance.index), log("Failed to find existing review"))
        } yield checklist
      ).fold(identity(_), identity(_))
    }

    val formValues = request.body.asFormUrlEncoded.getOrElse(Map.empty).filterNot(_._1 == "csrfToken")
    val submitAction = formValues.get("submit-action").flatMap(_.headOption)

    submitAction match {
      case Some("checked")         => saveAndContinue
      case Some("come-back-later") => comeBackLater
      case _                       => successful(log("No submit-action"))
    }
  }

  private def getVerifiedByDetails(verified: Boolean, dateVerifiedDay: String, dateVerifiedMonth: String, dateVerifiedYear: String): Option[SubmissionReview.VerifiedByDetails] = {
    val dateAsString = dateVerifiedYear + "-" + dateVerifiedMonth + "-" + dateVerifiedDay + "T00:00:00.000Z"
    try {
      val timestamp: DateTime = DateTime.parse(dateAsString)
      Some(SubmissionReview.VerifiedByDetails(verified, Some(timestamp)))
    } catch {
      case e: Exception => None
    }
  }

  def action2(applicationId: ApplicationId): Action[AnyContent] = loggedInWithApplicationAndSubmission(applicationId) { implicit request =>
    val log = logBadRequest(SubmissionReview.Action.ConfirmResponsibleIndividualVerified) _
    val checklist = Redirect(uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.routes.ChecklistController.checklistPage(applicationId))
    val failed = (msg: String, day: String, month: String, year: String) => {
      BadRequest(confirmResponsibleIndividualVerified2Page(ViewModel2(request.application.name, applicationId, Some(day), Some(month), Some(year), Some(msg))))
    }
    
    def saveAndContinue() = {
      (
        for {
          submissionReview  <- fromOptionF(submissionReviewService.findReview(request.submission.id, request.submission.latestInstance.index), log("No submission review found"))
          dateVerifiedDay   <- fromOption(request.body.asFormUrlEncoded.getOrElse(Map.empty).get("date-verified-day").flatMap(_.headOption), log("No date-verified-day found in request"))
          dateVerifiedMonth <- fromOption(request.body.asFormUrlEncoded.getOrElse(Map.empty).get("date-verified-month").flatMap(_.headOption), log("No date-verified-month found in request"))
          dateVerifiedYear  <- fromOption(request.body.asFormUrlEncoded.getOrElse(Map.empty).get("date-verified-year").flatMap(_.headOption), log("No date-verified-year found in request"))
          verifiedByDetails <- fromOption(getVerifiedByDetails(true, dateVerifiedDay, dateVerifiedMonth, dateVerifiedYear), failed("Invalid date", dateVerifiedDay, dateVerifiedMonth, dateVerifiedYear))
          _                 <- fromOptionF(submissionReviewService.updateVerifiedByDetails(verifiedByDetails)(request.submission.id, request.submission.latestInstance.index), log("Failed to find existing review"))
          _                 <- fromOptionF(submissionReviewService.updateActionStatus(SubmissionReview.Action.ConfirmResponsibleIndividualVerified, SubmissionReview.Status.Completed)(request.submission.id, request.submission.latestInstance.index), log("Failed to find existing review"))
        } yield checklist
      ).fold(identity(_), identity(_))
    }

    def comeBackLater() = {
      (
        for {
          _                 <- fromOptionF(submissionReviewService.updateActionStatus(SubmissionReview.Action.ConfirmResponsibleIndividualVerified, SubmissionReview.Status.InProgress)(request.submission.id, request.submission.latestInstance.index), log("Failed to find existing review"))
        } yield checklist
      ).fold(identity(_), identity(_))
    }

    val formValues = request.body.asFormUrlEncoded.getOrElse(Map.empty).filterNot(_._1 == "csrfToken")
    val submitAction = formValues.get("submit-action").flatMap(_.headOption)

    submitAction match {
      case Some("checked")         => saveAndContinue
      case Some("come-back-later") => comeBackLater
      case _                       => successful(log("No submit-action"))
    }
  }
}
