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
import scala.concurrent.Future.successful
import scala.concurrent.{ExecutionContext, Future}

import play.api.mvc.{MessagesControllerComponents, _}
import uk.gov.hmrc.apiplatform.modules.stride.config.StrideAuthConfig
import uk.gov.hmrc.apiplatform.modules.stride.connectors.AuthConnector
import uk.gov.hmrc.apiplatform.modules.stride.controllers.actions.ForbiddenHandler
import uk.gov.hmrc.apiplatform.modules.submissions.services.SubmissionService

import uk.gov.hmrc.apigatekeeperapprovalsfrontend.config.ErrorHandler
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.models.MarkedSubmissionApplicationRequest
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.ApplicationId
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.services.ApplicationActionService
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.views.html.ConfirmYourDecisionPage

object ConfirmYourDecisionController {
  case class ViewModel(applicationId: ApplicationId, appName: String)
}

@Singleton
class ConfirmYourDecisionController @Inject()(
  strideAuthConfig: StrideAuthConfig,
  authConnector: AuthConnector,
  forbiddenHandler: ForbiddenHandler,
  mcc: MessagesControllerComponents,
  errorHandler: ErrorHandler,
  val applicationActionService: ApplicationActionService,
  val submissionService: SubmissionService,
  confirmYourDecisionPage: ConfirmYourDecisionPage
)(implicit override val ec: ExecutionContext) 
    extends AbstractApplicationController(strideAuthConfig, authConnector, forbiddenHandler, mcc, errorHandler) {
      
  import ConfirmYourDecisionController._

  def page(applicationId: ApplicationId) = loggedInWithApplicationAndSubmission(applicationId) { implicit request =>
    if(request.markedSubmission.isFail) 
      successful(Redirect(uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.routes.DeclinedJourneyController.provideReasonsPage(applicationId)))
    else
      successful(Ok(confirmYourDecisionPage(ViewModel(applicationId, request.application.name))))
  }

  def action(applicationId: ApplicationId) = loggedInWithApplicationAndSubmission(applicationId) { implicit request => 
    request.body.asFormUrlEncoded.getOrElse(Map.empty).get("grant-decision").flatMap(_.headOption) match {
      case Some("decline")              => successful(Redirect(uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.routes.DeclinedJourneyController.provideReasonsPage(applicationId)))
      case Some("grant-with-warnings")  => successful(Redirect(uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.routes.GrantedJourneyController.provideWarningsPage(applicationId)))
      case Some("grant")                => grantAccess(applicationId)
      case _                            => successful(Redirect(uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.routes.ConfirmYourDecisionController.page(applicationId)))
    }
  }

  private def grantAccess(applicationId: ApplicationId)(implicit request: MarkedSubmissionApplicationRequest[AnyContent]) = {
    submissionService.grant(applicationId, request.name.get)
    .map {
      _ match {
        case Right(value) => Redirect(uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.routes.GrantedJourneyController.grantedPage(applicationId).url)
        case Left(err) => {
          logger.warn(s"Error granting access: $err")
          InternalServerError(errorHandler.internalServerErrorTemplate)
        }
      }
    }
  }
}
