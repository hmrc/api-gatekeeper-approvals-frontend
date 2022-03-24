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

import cats.data.EitherT

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.{Application, ApplicationId, ResponsibleIndividual, Standard, SubmissionReview, TermsOfUseAcceptance}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}

import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.apiplatform.modules.stride.config.StrideAuthConfig
import uk.gov.hmrc.apiplatform.modules.stride.controllers.actions.ForbiddenHandler
import uk.gov.hmrc.apiplatform.modules.stride.connectors.AuthConnector
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.config.ErrorHandler
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.services.ApplicationActionService
import uk.gov.hmrc.apiplatform.modules.submissions.services.SubmissionService
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.views.html.ConfirmResponsibleIndividualVerified1Page
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.views.html.ConfirmResponsibleIndividualVerified2Page

import scala.concurrent.Future.successful
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.services.SubmissionReviewService
import org.joda.time.DateTime
import play.api.data.Form
import play.api.data.Forms._
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.connectors.ThirdPartyApplicationConnector
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.connectors.AddTermsOfUseAcceptanceRequest
import uk.gov.hmrc.http.UpstreamErrorResponse

object ConfirmResponsibleIndividualVerifiedController {  
  case class ViewModel(appName: String, applicationId: ApplicationId)

  case class HasVerifiedForm(verified: String)

  val hasVerifiedForm: Form[HasVerifiedForm] = Form(
    mapping(
      "verified" -> nonEmptyText
    )(HasVerifiedForm.apply)(HasVerifiedForm.unapply)
  )

  case class VerifiedDateForm(day: Int, month: Int, year: Int)

  val verifiedDateForm: Form[VerifiedDateForm] = Form(
    mapping(
      "day" -> number(min = 1, max = 31),
      "month" -> number(min = 1, max = 12),
      "year" -> number(min = 2000, max = 2200)
    )(VerifiedDateForm.apply)(VerifiedDateForm.unapply)
  )
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
  val submissionService: SubmissionService,
  thirdPartyApplicationConnector: ThirdPartyApplicationConnector
)(implicit override val ec: ExecutionContext) extends AbstractCheckController(strideAuthConfig, authConnector, forbiddenHandler, mcc, errorHandler, submissionReviewService) {

  import ConfirmResponsibleIndividualVerifiedController._

  def page1(applicationId: ApplicationId): Action[AnyContent] = loggedInWithApplicationAndSubmission(applicationId) { implicit request =>

    def getHasVerifiedForm(submissionReview: SubmissionReview) = {
      val existingVerifyValue = submissionReview.verifiedByDetails.fold(Option.empty[String])(vbd => if(vbd.verified) Some("yes") else Some("no"))
      if (existingVerifyValue.isDefined)
        hasVerifiedForm.fill(HasVerifiedForm(existingVerifyValue.get))
      else
        hasVerifiedForm
    } 

    def gotoPage1() = {
      val log = logBadRequest(SubmissionReview.Action.ConfirmResponsibleIndividualVerified) _
      (
        for {
          submissionReview    <- fromOptionF(submissionReviewService.findReview(request.submission.id, request.submission.latestInstance.index), log("No submission review found"))
           } yield Ok(
                confirmResponsibleIndividualVerified1Page(
                  getHasVerifiedForm(submissionReview),
                  ViewModel(
                    request.application.name,
                    applicationId
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

    def getVerifiedDateForm(submissionReview: SubmissionReview) = {
      val existingTimestamp = submissionReview.verifiedByDetails.fold(Option.empty[DateTime])(vbd => vbd.timestamp.fold(Option.empty[DateTime])(tim => Some(tim)))
      if (existingTimestamp.isDefined)
        verifiedDateForm.fill(
          VerifiedDateForm(
            existingTimestamp.get.getDayOfMonth(), 
            existingTimestamp.get.getMonthOfYear(), 
            existingTimestamp.get.getYear()
          )
        )
      else
        verifiedDateForm
    }

    def gotoPage2() = {
      val log = logBadRequest(SubmissionReview.Action.ConfirmResponsibleIndividualVerified) _
      (
        for {
          submissionReview <- fromOptionF(submissionReviewService.findReview(request.submission.id, request.submission.latestInstance.index), log("No submission review found"))
        } yield Ok(
                confirmResponsibleIndividualVerified2Page(
                  getVerifiedDateForm(submissionReview),
                  ViewModel(
                    request.application.name,
                    applicationId
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

    def comeBackLater() = {
      (
        for {
          _                 <- fromOptionF(submissionReviewService.updateActionStatus(SubmissionReview.Action.ConfirmResponsibleIndividualVerified, SubmissionReview.Status.InProgress)(request.submission.id, request.submission.latestInstance.index), log("Failed to find existing review"))
        } yield checklist
      ).fold(identity(_), identity(_))
    }

    def handleValidForm(form: ConfirmResponsibleIndividualVerifiedController.HasVerifiedForm) = {
      (
        for {
          submissionReview  <- fromOptionF(submissionReviewService.findReview(request.submission.id, request.submission.latestInstance.index), log("No submission review found"))
          verifiedByDetails =  getVerifiedByDetails(form.verified, submissionReview)
          _                 <- fromOptionF(submissionReviewService.updateVerifiedByDetails(verifiedByDetails)(request.submission.id, request.submission.latestInstance.index), log("Failed to find existing review"))
          _                 <- fromOptionF(submissionReviewService.updateActionStatus(SubmissionReview.Action.ConfirmResponsibleIndividualVerified, SubmissionReview.Status.Completed)(request.submission.id, request.submission.latestInstance.index), log("Failed to find existing review"))
        } yield success(verifiedByDetails)
      ).fold(identity(_), identity(_))
    }

    def handleInvalidForm(form: Form[HasVerifiedForm]) = {
      successful(BadRequest(confirmResponsibleIndividualVerified1Page(form, ViewModel(request.application.name, applicationId))))
    }

    val formValues = request.body.asFormUrlEncoded.getOrElse(Map.empty).filterNot(_._1 == "csrfToken")
    val submitAction = formValues.get("submit-action").flatMap(_.headOption)

    submitAction match {
      case Some("checked")         => ConfirmResponsibleIndividualVerifiedController.hasVerifiedForm.bindFromRequest.fold(handleInvalidForm, handleValidForm)
      case Some("come-back-later") => comeBackLater
      case _                       => successful(log("No submit-action"))
    }
  }

  private def getVerifiedByDetails(verified: Boolean, dateVerifiedDay: Int, dateVerifiedMonth: Int, dateVerifiedYear: Int): Option[SubmissionReview.VerifiedByDetails] = {
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
    val failed = (msg: String, day: Int, month: Int, year: Int) => {
      BadRequest(confirmResponsibleIndividualVerified2Page(verifiedDateForm.fill(VerifiedDateForm(day, month, year)).withError("Date", "Invalid date"), ViewModel(request.application.name, applicationId)))
    }


//    def addTermsOfUseAcceptance(verifiedByDetails: SubmissionReview.VerifiedByDetails): EitherT[Future, String, Unit] = {
//      def maybeStandardAccess(application: Application) = application.access match {
//        case stdAccess: Standard => Some(stdAccess)
//        case _ => None
//      }
//
//      val termsOfUseVersion = "2.0" //TODO!!
//      val maybeAddTermsOfUseAcceptanceRequest = for {
//        standardAccess <- maybeStandardAccess(request.application)
//        importantSubmissionData <- standardAccess.importantSubmissionData
//        acceptanceDate <- verifiedByDetails.timestamp
//      } yield AddTermsOfUseAcceptanceRequest(
//        importantSubmissionData.responsibleIndividual.fullName,
//        importantSubmissionData.responsibleIndividual.emailAddress,
//        acceptanceDate,
//        request.submission.id,
//        termsOfUseVersion
//      )
//
//      maybeAddTermsOfUseAcceptanceRequest match {
//        case Some(addTermsOfUseAcceptanceRequest) => {
//          EitherT(thirdPartyApplicationConnector.addTermsOfUseAcceptance(request.application.id, addTermsOfUseAcceptanceRequest).map(_ match {
//            case err: UpstreamErrorResponse => Left(err.message)
//            case _ => Right()
//          }))
//        }
//        case None => EitherT.leftT("Missing application data")
//      }
//    }

    def addTermsOfUseAcceptance(verifiedByDetails: SubmissionReview.VerifiedByDetails): Future[Option[Unit]] = {
      def maybeStandardAccess(application: Application) = application.access match {
        case stdAccess: Standard => Some(stdAccess)
        case _ => None
      }

      val termsOfUseVersion = "2.0" //TODO!!
      val maybeAddTermsOfUseAcceptanceRequest = for {
        standardAccess <- maybeStandardAccess(request.application)
        importantSubmissionData <- standardAccess.importantSubmissionData
        acceptanceDate <- verifiedByDetails.timestamp
      } yield AddTermsOfUseAcceptanceRequest(
        importantSubmissionData.responsibleIndividual.fullName,
        importantSubmissionData.responsibleIndividual.emailAddress,
        acceptanceDate,
        request.submission.id,
        termsOfUseVersion
      )

      maybeAddTermsOfUseAcceptanceRequest match {
        case Some(addTermsOfUseAcceptanceRequest) => thirdPartyApplicationConnector.addTermsOfUseAcceptance(request.application.id, addTermsOfUseAcceptanceRequest).map(
          _ match {
            case Left(_) => None
            case _ => Some()
          }
        )
        case None => Future.successful(None)
      }
    }

    def handleValidForm(form: ConfirmResponsibleIndividualVerifiedController.VerifiedDateForm) = {
      (
        for {
          submissionReview  <- fromOptionF(submissionReviewService.findReview(request.submission.id, request.submission.latestInstance.index), log("No submission review found"))
          verifiedByDetails <- fromOption(getVerifiedByDetails(true, form.day, form.month, form.year), failed("Invalid date", form.day, form.month, form.year))
          _                 <- fromOptionF(submissionReviewService.updateVerifiedByDetails(verifiedByDetails)(request.submission.id, request.submission.latestInstance.index), log("Failed to find existing review"))
          _                 <- fromOptionF(submissionReviewService.updateActionStatus(SubmissionReview.Action.ConfirmResponsibleIndividualVerified, SubmissionReview.Status.Completed)(request.submission.id, request.submission.latestInstance.index), log("Failed to find existing review"))
          _                 <- fromOptionF(addTermsOfUseAcceptance(verifiedByDetails), log("Missing"))
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

    def handleInvalidForm(form: Form[VerifiedDateForm]) = {
      successful(BadRequest(confirmResponsibleIndividualVerified2Page(form, ViewModel(request.application.name, applicationId))))
    }

    val formValues = request.body.asFormUrlEncoded.getOrElse(Map.empty).filterNot(_._1 == "csrfToken")
    val submitAction = formValues.get("submit-action").flatMap(_.headOption)

    submitAction match {
      case Some("checked")         => ConfirmResponsibleIndividualVerifiedController.verifiedDateForm.bindFromRequest.fold(handleInvalidForm, handleValidForm)
      case Some("come-back-later") => comeBackLater
      case _                       => successful(log("No submit-action"))
    }
  }
}
