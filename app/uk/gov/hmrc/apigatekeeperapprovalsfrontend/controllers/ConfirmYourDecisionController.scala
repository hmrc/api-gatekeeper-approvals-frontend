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

import uk.gov.hmrc.apigatekeeperapprovalsfrontend.views.html.ConfirmYourDecisionPage

import scala.concurrent.Future.successful
import scala.concurrent.ExecutionContext
import uk.gov.hmrc.apiplatform.modules.stride.controllers.GatekeeperBaseController
import uk.gov.hmrc.apiplatform.modules.stride.config.StrideAuthConfig
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.apiplatform.modules.stride.connectors.AuthConnector
import uk.gov.hmrc.apiplatform.modules.stride.controllers.actions.ForbiddenHandler
import play.api.mvc._
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.ApplicationId
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.actions.ApplicationActions
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.config.ErrorHandler
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.services.ApplicationActionService
import uk.gov.hmrc.apiplatform.modules.submissions.services.SubmissionService
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.views.html.{ApplicationApprovedPage, ApplicationDeclinedPage}
import uk.gov.hmrc.apiplatform.modules.stride.controllers.models.LoggedInRequest

object ConfirmYourDecisionController {
  case class ViewModel(applicationId: ApplicationId, appName: String)
}

@Singleton
class ConfirmYourDecisionController @Inject()(
  strideAuthConfig: StrideAuthConfig,
  authConnector: AuthConnector,
  forbiddenHandler: ForbiddenHandler,
  mcc: MessagesControllerComponents,
  val errorHandler: ErrorHandler,
  val applicationActionService: ApplicationActionService,
  val submissionService: SubmissionService,
  confirmYourDecisionPage: ConfirmYourDecisionPage,
  applicationApprovedPage: ApplicationApprovedPage,
  applicationDeclinedPage: ApplicationDeclinedPage
)(implicit override val ec: ExecutionContext) extends GatekeeperBaseController(strideAuthConfig, authConnector, forbiddenHandler, mcc) with ApplicationActions {
  import ConfirmYourDecisionController._

  def page(applicationId: ApplicationId): Action[AnyContent] = loggedInWithApplicationAndSubmission(applicationId) { implicit request =>
    successful(Ok(confirmYourDecisionPage(ViewModel(applicationId, request.application.name))))
  }

  def action(applicationId: ApplicationId): Action[AnyContent] = loggedInWithApplicationAndSubmission(applicationId) { implicit request => 
    request.body.asFormUrlEncoded.getOrElse(Map.empty).get("grant-decision").flatMap(_.headOption) match {
      case Some("decline")              => actionDecline(applicationId)
      case Some("grant-with-warnings")  => successful(Ok(confirmYourDecisionPage(ViewModel(applicationId, request.application.name))))
      case Some("grant")                => actionGrant(applicationId)
      case _                            => successful(Redirect(uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.routes.ConfirmYourDecisionController.page(applicationId)))
    }
  }

  def grantedPage(applicationId: ApplicationId): Action[AnyContent] = loggedInWithApplicationAndSubmission(applicationId) { implicit request =>
    successful(Ok(applicationApprovedPage(ViewModel(applicationId, request.application.name))))
  }

  def declinedPage(applicationId: ApplicationId): Action[AnyContent] = loggedInWithApplicationAndSubmission(applicationId) { implicit request =>
    successful(Ok(applicationDeclinedPage(ViewModel(applicationId, request.application.name))))
  }

  def actionGrant(applicationId: ApplicationId)(implicit request: LoggedInRequest[_]) = {
    submissionService.grant(applicationId, request.name.get).map(_ match {
      case Right(application)  => Redirect(uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.routes.ConfirmYourDecisionController.grantedPage(applicationId))
      case Left(err)           => BadRequest(errorHandler.badRequestTemplate)
    })
    .recover {
      case _ => BadRequest(errorHandler.badRequestTemplate)
    }
  }

  def actionDecline(applicationId: ApplicationId)(implicit request: LoggedInRequest[_]) = {
    submissionService.decline(applicationId, request.name.get, "Coz it's bobbins").map( _ match {
      case Right(application)  => Redirect(uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.routes.ConfirmYourDecisionController.declinedPage(applicationId))
      case Left(err)           => BadRequest(errorHandler.badRequestTemplate)
    })
    .recover {
      case _ => BadRequest(errorHandler.badRequestTemplate)
    }
  }
}
