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
import uk.gov.hmrc.apiplatform.modules.stride.config.StrideAuthConfig
import uk.gov.hmrc.apiplatform.modules.stride.connectors.AuthConnector
import uk.gov.hmrc.apiplatform.modules.stride.controllers.actions.ForbiddenHandler
import uk.gov.hmrc.apiplatform.modules.submissions.services.SubmissionService

import uk.gov.hmrc.apigatekeeperapprovalsfrontend.config.ErrorHandler
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.ApplicationId
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.services.ApplicationActionService
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.views.html.ProvideWarningsForGrantingPage
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.services.SubmissionReviewService
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.views.html.ApplicationApprovedPage

object GrantedJourneyController {
  case class ViewModel(applicationId: ApplicationId, appName: String)

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
  val applicationApprovedPage: ApplicationApprovedPage
)(implicit override val ec: ExecutionContext)
  extends AbstractApplicationController(strideAuthConfig, authConnector, forbiddenHandler, mcc, errorHandler) {
  import GrantedJourneyController._

  def provideWarningsPage(applicationId: ApplicationId) = loggedInWithApplicationAndSubmission(applicationId) { implicit request =>
    successful(Ok(provideWarningsForGrantingPage(provideWarningsForm, ViewModel(applicationId, request.application.name))))
  }

  def provideWarningsAction(applicationId: ApplicationId) = loggedInWithApplicationAndSubmission(applicationId) { implicit request => 
    def handleValidForm(form: ProvideWarningsForm) = {
      submissionReviewService.updateGrantWarnings(form.warnings)(request.submission.id, request.submission.latestInstance.index).map {
        case Some(value) => Redirect(uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.routes.GrantedJourneyController.grantedPage(applicationId).url)
        case None => {
          logger.warn("Persisting decline reasons failed")
          BadRequest(errorHandler.badRequestTemplate)
        }
      }
    }

    def handleInvalidForm(form: Form[ProvideWarningsForm]) = {
      successful(BadRequest(provideWarningsForGrantingPage(form, ViewModel(applicationId, request.application.name))))
    }

    GrantedJourneyController.provideWarningsForm.bindFromRequest.fold(handleInvalidForm, handleValidForm)
  }

  def grantedPage(applicationId: ApplicationId) = loggedInWithApplicationAndSubmission(applicationId) { implicit request =>
    successful(Ok(applicationApprovedPage(GrantedJourneyController.ViewModel(applicationId, request.application.name))))
  }
}
