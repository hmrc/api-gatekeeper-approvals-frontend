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
import cats.data.EitherT
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.apiplatform.modules.stride.config.StrideAuthConfig
import uk.gov.hmrc.apiplatform.modules.stride.connectors.AuthConnector
import uk.gov.hmrc.apiplatform.modules.stride.controllers.actions.ForbiddenHandler
import uk.gov.hmrc.apiplatform.modules.submissions.services.SubmissionService
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.config.ErrorHandler
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.ApplicationId
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.services.{ApplicationActionService, ApplicationService, SubmissionReviewService}
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.views.html.{ApplicationApprovedPage, ProvideWarningsForGrantingPage}

object GrantedJourneyController {
  case class ViewModel(appName: String, applicationId: ApplicationId)

  case class ProvideWarningsForm(warnings: String)

  val provideWarningsForm: Form[ProvideWarningsForm] = Form(
    mapping(
      "warnings" -> nonEmptyText
    )(ProvideWarningsForm.apply)(ProvideWarningsForm.unapply)
  )
}

@Singleton
class GrantedJourneyController @Inject()(
  strideAuthConfig: StrideAuthConfig,
  authConnector: AuthConnector,
  forbiddenHandler: ForbiddenHandler,
  mcc: MessagesControllerComponents,
  errorHandler: ErrorHandler,
  val applicationActionService: ApplicationActionService,
  val submissionService: SubmissionService,
  submissionReviewService: SubmissionReviewService,
  provideWarningsForGrantingPage: ProvideWarningsForGrantingPage,
  applicationApprovedPage: ApplicationApprovedPage,
  applicationService: ApplicationService
)(implicit override val ec: ExecutionContext)
  extends AbstractApplicationController(strideAuthConfig, authConnector, forbiddenHandler, mcc, errorHandler) {
  import GrantedJourneyController._

  def provideWarningsPage(applicationId: ApplicationId) = loggedInWithApplicationAndSubmission(applicationId) { implicit request =>
    successful(Ok(provideWarningsForGrantingPage(provideWarningsForm, ViewModel(request.application.name, applicationId))))
  }

  def provideWarningsAction(applicationId: ApplicationId) = loggedInWithApplicationAndSubmission(applicationId) { implicit request =>
    def handleValidForm(form: ProvideWarningsForm) = {
      (
        for {
          review      <- EitherT.fromOptionF(submissionReviewService.updateGrantWarnings(form.warnings)(request.submission.id, request.submission.latestInstance.index), "There was a problem updating the grant warnings on the submission review")
          application <- EitherT(submissionService.grantWithWarnings(applicationId, request.name.get, form.warnings, review.verifiedByDetails.flatMap(_.timestamp)))
          _           <- EitherT(applicationService.addTermsOfUseAcceptance(application, review))
        } yield Redirect(uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.routes.GrantedJourneyController.grantedPage(applicationId).url)
      )
      .value
      .map {
        case Right(value) => value
        case Left(err) => {
          logger.warn(s"Error granting access for application $applicationId: $err")
          InternalServerError(errorHandler.internalServerErrorTemplate)
        }
      }
    }

    def handleInvalidForm(form: Form[ProvideWarningsForm]) = {
      successful(BadRequest(provideWarningsForGrantingPage(form, ViewModel(request.application.name, applicationId))))
    }

    GrantedJourneyController.provideWarningsForm.bindFromRequest.fold(handleInvalidForm, handleValidForm)
  }

  def grantedPage(applicationId: ApplicationId) = loggedInWithApplicationAndSubmission(applicationId) { implicit request =>
    successful(Ok(applicationApprovedPage(GrantedJourneyController.ViewModel(request.application.name, applicationId))))
  }
}
