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
import play.api.mvc
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.apiplatform.modules.common.services.ApplicationLogger
import uk.gov.hmrc.apiplatform.modules.stride.config.StrideAuthConfig
import uk.gov.hmrc.apiplatform.modules.stride.connectors.AuthConnector
import uk.gov.hmrc.apiplatform.modules.stride.controllers.GatekeeperBaseController
import uk.gov.hmrc.apiplatform.modules.stride.controllers.actions.ForbiddenHandler
import uk.gov.hmrc.apiplatform.modules.submissions.services.SubmissionService

import uk.gov.hmrc.apigatekeeperapprovalsfrontend.config.ErrorHandler
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.actions.ApplicationActions
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.ApplicationId
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.services.ApplicationActionService
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.views.html._

object DeclinedJourneyController {
  case class ViewModel(applicationId: ApplicationId, appName: String)

  case class ProvideReasonsForm(reasons: String)

  val provideReasonsForm: Form[ProvideReasonsForm] = Form(
    mapping(
      "reasons" -> nonEmptyText
    )(ProvideReasonsForm.apply)(ProvideReasonsForm.unapply)
  )
}

@Singleton
class DeclinedJourneyController @Inject()(
  strideAuthConfig: StrideAuthConfig,
  authConnector: AuthConnector,
  forbiddenHandler: ForbiddenHandler,
  mcc: MessagesControllerComponents,
  val errorHandler: ErrorHandler,
  val applicationActionService: ApplicationActionService,
  val submissionService: SubmissionService,
  applicationDeclinedPage: ApplicationDeclinedPage,
  provideReasonsForDecliningPage: ProvideReasonsForDecliningPage
)(implicit override val ec: ExecutionContext)
  extends GatekeeperBaseController(strideAuthConfig, authConnector, forbiddenHandler, mcc)
    with ApplicationActions
    with ApplicationLogger {

  import DeclinedJourneyController._

  def provideReasonsPage(applicationId: ApplicationId): mvc.Action[mvc.AnyContent] = loggedInWithApplicationAndSubmission(applicationId) { implicit request =>
    successful(Ok(provideReasonsForDecliningPage(provideReasonsForm, ViewModel(applicationId, request.application.name))))
  }

  def provideReasonsAction(applicationId: ApplicationId) = loggedInWithApplicationAndSubmission(applicationId) { implicit request => 
    def handleValidForm(form: DeclinedJourneyController.ProvideReasonsForm) = {
      submissionService.decline(applicationId, request.name.get, form.reasons).map( _ match {
        case Right(app) => Ok(applicationDeclinedPage(ViewModel(applicationId, request.application.name)))
        case Left(err) => {
          logger.warn(s"Decline application failed due to: $err")
          BadRequest(errorHandler.badRequestTemplate)
        }
      })
    }

    def handleInvalidForm(form: Form[ProvideReasonsForm]) = {
      successful(BadRequest(provideReasonsForDecliningPage(form, ViewModel(applicationId, request.application.name))))
    }

    DeclinedJourneyController.provideReasonsForm.bindFromRequest.fold(handleInvalidForm, handleValidForm)
  }

}
