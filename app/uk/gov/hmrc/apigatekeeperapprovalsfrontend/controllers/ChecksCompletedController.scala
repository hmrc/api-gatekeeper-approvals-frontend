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

import uk.gov.hmrc.apigatekeeperapprovalsfrontend.views.html.ChecksCompletedPage
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.views.html.ApplicationApprovedPage
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.views.html.ApplicationDeclinedPage

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
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.config.GatekeeperConfig

object ChecksCompletedController {
  case class ViewModel(applicationId: ApplicationId, appName: String)
}

@Singleton
class ChecksCompletedController @Inject()(
  config: GatekeeperConfig,
  strideAuthConfig: StrideAuthConfig,
  authConnector: AuthConnector,
  forbiddenHandler: ForbiddenHandler,
  mcc: MessagesControllerComponents,
  val errorHandler: ErrorHandler,
  val applicationActionService: ApplicationActionService,
  val submissionService: SubmissionService,
  checksCompletedPage: ChecksCompletedPage,
  applicationApprovedPage: ApplicationApprovedPage,
  applicationDeclinedPage: ApplicationDeclinedPage
)(implicit override val ec: ExecutionContext) extends GatekeeperBaseController(config, strideAuthConfig, authConnector, forbiddenHandler, mcc) with ApplicationActions {
  import ChecksCompletedController._

  def checksCompletedPage(applicationId: ApplicationId): Action[AnyContent] = loggedInWithApplicationAndSubmission(applicationId) { implicit request =>
    successful(Ok(checksCompletedPage(ViewModel(applicationId, request.application.name), breadcrumbsUrls)))
  }

  def checksCompletedAction(applicationId: ApplicationId): Action[AnyContent] = loggedInWithApplicationAndSubmission(applicationId) { implicit request => 
    request.body.asFormUrlEncoded.getOrElse(Map.empty).get("submit-action").flatMap(_.headOption) match {
      case Some("passed") => {
        submissionService.grant(applicationId, request.loggedInRequest.name.get).map(_ match {
          case Right(application)  => Ok(applicationApprovedPage(ViewModel(applicationId, request.application.name), breadcrumbsUrls))
          case Left(err)           => BadRequest(err) 
        })
      }
      case Some("failed") => {
        submissionService.decline(applicationId, request.loggedInRequest.name.get, "TODO - reason").map(_ match {
          case Right(application)  => Ok(applicationDeclinedPage(ViewModel(applicationId, request.application.name), breadcrumbsUrls))
          case Left(err)           => BadRequest(err)
        })
      }
      case _ => successful(BadRequest(errorHandler.badRequestTemplate))
    }
  }
}