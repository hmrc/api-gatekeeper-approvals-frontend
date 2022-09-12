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
import scala.concurrent.ExecutionContext
import scala.concurrent.Future.successful
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.apiplatform.modules.gkauth.services.StrideAuthorisationService
import uk.gov.hmrc.apiplatform.modules.submissions.services.SubmissionService
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.config.ErrorHandler
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.ApplicationId
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.services.ApplicationService
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.services.ApplicationActionService
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.views.html._
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.services.SubmissionReviewService
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.CollaboratorRole
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.Collaborator
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.services.{SubmissionRequiresDemo, SubmissionRequiresFraudCheck}
import uk.gov.hmrc.play.bootstrap.controller.WithDefaultFormBinding

object DeclinedJourneyController {
  case class ViewModel(applicationId: ApplicationId, appName: String, adminsToEmail: Set[Collaborator] = Set.empty)

  case class ProvideReasonsForm(reasons: String)

  val provideReasonsForm: Form[ProvideReasonsForm] = Form(
    mapping(
      "reasons" -> nonEmptyText
    )(ProvideReasonsForm.apply)(ProvideReasonsForm.unapply)
  )
}

@Singleton
class DeclinedJourneyController @Inject()(
  strideAuthorisationService: StrideAuthorisationService,
  mcc: MessagesControllerComponents,
  errorHandler: ErrorHandler,
  val applicationActionService: ApplicationActionService,
  val submissionService: SubmissionService,
  val applicationService: ApplicationService,
  applicationDeclinedPage: ApplicationDeclinedPage,
  provideReasonsForDecliningPage: ProvideReasonsForDecliningPage,
  adminsToEmailPage: AdminsToEmailPage,
  submissionReviewService: SubmissionReviewService
)(implicit override val ec: ExecutionContext)
  extends AbstractApplicationController(strideAuthorisationService, mcc, errorHandler) with WithDefaultFormBinding {

  import DeclinedJourneyController._

  def provideReasonsPage(applicationId: ApplicationId) = loggedInThruStrideWithApplicationAndSubmission(applicationId) { implicit request =>
    successful(Ok(provideReasonsForDecliningPage(provideReasonsForm, ViewModel(applicationId, request.application.name))))
  }

  def provideReasonsAction(applicationId: ApplicationId) = loggedInThruStrideWithApplicationAndSubmission(applicationId) { implicit request => 
    def handleValidForm(form: DeclinedJourneyController.ProvideReasonsForm) = {
      submissionReviewService.updateDeclineReasons(form.reasons)(request.submission.id, request.submission.latestInstance.index).map {
        case Some(value) => Redirect(uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.routes.DeclinedJourneyController.emailAddressesPage(applicationId))
        case None => {
          logger.warn("Persisting decline reasons failed")
          BadRequest(errorHandler.badRequestTemplate)
        }
      }
    }

    def handleInvalidForm(form: Form[ProvideReasonsForm]) = {
      successful(BadRequest(provideReasonsForDecliningPage(form, ViewModel(applicationId, request.application.name))))
    }

    DeclinedJourneyController.provideReasonsForm.bindFromRequest.fold(handleInvalidForm, handleValidForm)
  }

  def declinedPage(applicationId: ApplicationId) = loggedInThruStrideWithApplicationAndSubmission(applicationId) { implicit request =>
    successful(Ok(applicationDeclinedPage(ViewModel(applicationId, request.application.name))))
  }

  def emailAddressesPage(applicationId: ApplicationId) = loggedInThruStrideWithApplicationAndSubmission(applicationId) { implicit request =>
    val adminsToEmail = request.application.collaborators.filter(_.role.is(CollaboratorRole.ADMINISTRATOR))
    
    successful(Ok(adminsToEmailPage(ViewModel(applicationId, request.application.name, adminsToEmail))))
  }

  def emailAddressesAction(applicationId: ApplicationId) = loggedInThruStrideWithApplicationAndSubmission(applicationId) { implicit request =>
    val requiresFraudCheck = SubmissionRequiresFraudCheck(request.submission)
    val requiresDemo = SubmissionRequiresDemo(request.submission)
    val ok = Redirect(uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.routes.DeclinedJourneyController.declinedPage(applicationId))

    for {
      review <- submissionReviewService.findOrCreateReview(request.submission.id, request.submission.latestInstance.index, !request.markedSubmission.isFail, request.markedSubmission.isWarn, requiresFraudCheck, requiresDemo)
      result <- applicationService.declineApplicationApprovalRequest(applicationId, request.name.get, review.declineReasons)
    } yield ok
  }
}
