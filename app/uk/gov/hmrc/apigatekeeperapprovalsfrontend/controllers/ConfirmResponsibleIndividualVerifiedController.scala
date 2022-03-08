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
import uk.gov.hmrc.apiplatform.modules.submissions.domain.services.ResponsibleIndividualExtractor
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.SubmissionReview
import uk.gov.hmrc.thirdpartyapplication.domain
import org.joda.time.DateTime

object ConfirmResponsibleIndividualVerifiedController {  
  case class ViewModel(appName: String, applicationId: ApplicationId) {
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


    // Should only be uplifting and checking Standard apps
    (request.application.access) match {
      case (std: Standard) if(request.submission.status.isSubmitted) =>
        successful(
          Ok(
            confirmResponsibleIndividualVerified1Page(
              ViewModel(
                request.application.name,
                applicationId
              )
            )
          )
        )

      case _ => successful(BadRequest(errorHandler.badRequestTemplate))
    }
  }

  def page2(applicationId: ApplicationId): Action[AnyContent] = loggedInWithApplicationAndSubmission(applicationId) { implicit request =>

    // Should only be uplifting and checking Standard apps
    (request.application.access) match {
      case (std: Standard) if(request.submission.status.isSubmitted) =>
        successful(
          Ok(
            confirmResponsibleIndividualVerified2Page(
              ViewModel(
                request.application.name,
                applicationId
              )
            )
          )
        )

      case _ => successful(BadRequest(errorHandler.badRequestTemplate))
    }
  }

  private def getVerifiedByDetails(verifyAnswer: String) = {
    val verifyAnswerBoolean: Boolean = verifyAnswer == "yes"
    SubmissionReview.VerifiedByDetails(verifyAnswerBoolean)
  }

  def action1(applicationId: ApplicationId): Action[AnyContent] = loggedInWithApplicationAndSubmission(applicationId) { implicit request =>
    lazy val success = (verifiedByDetails: SubmissionReview.VerifiedByDetails) => if(verifiedByDetails.verified)
        Redirect(uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.routes.ConfirmResponsibleIndividualVerifiedController.page2(applicationId))
      else
        Redirect(uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.routes.ChecklistController.checklistPage(applicationId))
    val log = logBadRequest(SubmissionReview.Action.ConfirmResponsibleIndividualVerified) _

    val formValues = request.body.asFormUrlEncoded.get.filterNot(_._1 == "csrfToken")
    val submitAction = formValues.get("submit-action").flatMap(_.headOption)
    val answer = formValues.get("verify-answer").flatMap(_.headOption)

    import cats.implicits._
    import cats.instances.future.catsStdInstancesForFuture

    def validateAnswers(submitAction: Option[String], answer: Option[String]): Either[String, Option[String]] = (submitAction, answer) match {
      case (Some("checked"), None) => Either.left("save action requires values")
      case (Some("checked"), Some(ans)) => Either.right(Some(ans))
      case (Some("come-back-later"), _) => Either.right(None)
      case (None, _) => Either.left("Bad request - no action")
      case (Some(_), _) => Either.left("Bad request - no such action")
    }

    (
      for {
        formAction        <- fromOption(request.body.asFormUrlEncoded.getOrElse(Map.empty).get("submit-action").flatMap(_.headOption), log("No submit-action found in request"))
        verifyAnswer      <- fromOption(request.body.asFormUrlEncoded.getOrElse(Map.empty).get("verify-answer").flatMap(_.headOption), log("No verify-answer found in request"))
        verifiedByDetails =  getVerifiedByDetails(verifyAnswer)
        newStatus         <- fromOption(deriveStatusFromAction(formAction), log("Invalid submit-action found in request"))
        _                 <- fromOptionF(submissionReviewService.updateVerifiedByDetails(verifiedByDetails)(request.submission.id, request.submission.latestInstance.index), log("Failed to find existing review"))
        _                 <- fromOptionF(submissionReviewService.updateActionStatus(SubmissionReview.Action.ConfirmResponsibleIndividualVerified, newStatus)(request.submission.id, request.submission.latestInstance.index), log("Failed to find existing review"))
      } yield success(verifiedByDetails)
    ).fold(identity(_), identity(_))
  }

  private def getVerifiedByDetails(verified: Boolean, dateVerifiedDay: String, dateVerifiedMonth: String, dateVerifiedYear: String) = {
    val dateAsString = dateVerifiedYear + "-" + dateVerifiedMonth + "-" + dateVerifiedDay + "T00:00:00.000Z"
    val timestamp: DateTime = DateTime.parse(dateAsString)
    SubmissionReview.VerifiedByDetails(verified, Some(timestamp))
  }

  def action2(applicationId: ApplicationId): Action[AnyContent] = loggedInWithApplicationAndSubmission(applicationId) { implicit request =>
    val ok = Redirect(uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.routes.ChecklistController.checklistPage(applicationId))
    val log = logBadRequest(SubmissionReview.Action.ConfirmResponsibleIndividualVerified) _

    (
      for {
        formAction        <- fromOption(request.body.asFormUrlEncoded.getOrElse(Map.empty).get("submit-action").flatMap(_.headOption), log("No submit-action found in request"))
        dateVerifiedDay   <- fromOption(request.body.asFormUrlEncoded.getOrElse(Map.empty).get("date-verified-day").flatMap(_.headOption), log("No date-verified-day found in request"))
        dateVerifiedMonth <- fromOption(request.body.asFormUrlEncoded.getOrElse(Map.empty).get("date-verified-month").flatMap(_.headOption), log("No date-verified-month found in request"))
        dateVerifiedYear  <- fromOption(request.body.asFormUrlEncoded.getOrElse(Map.empty).get("date-verified-year").flatMap(_.headOption), log("No date-verified-year found in request"))
        verifiedByDetails =  getVerifiedByDetails(true, dateVerifiedDay, dateVerifiedMonth, dateVerifiedYear)
        newStatus         <- fromOption(deriveStatusFromAction(formAction), log("Invalid submit-action found in request"))
        _                 <- fromOptionF(submissionReviewService.updateVerifiedByDetails(verifiedByDetails)(request.submission.id, request.submission.latestInstance.index), log("Failed to find existing review"))
        _                 <- fromOptionF(submissionReviewService.updateActionStatus(SubmissionReview.Action.ConfirmResponsibleIndividualVerified, newStatus)(request.submission.id, request.submission.latestInstance.index), log("Failed to find existing review"))
      } yield ok
    ).fold(identity(_), identity(_))
  }
}
