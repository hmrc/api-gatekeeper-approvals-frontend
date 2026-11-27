/*
 * Copyright 2023 HM Revenue & Customs
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

import play.api.mvc.*

import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.*
import uk.gov.hmrc.apiplatform.modules.commands.applications.domain.models.{CommandFailure, CommandFailures}
import uk.gov.hmrc.apiplatform.modules.common.domain.models.ApplicationId
import uk.gov.hmrc.apiplatform.modules.gkauth.domain.models.GatekeeperRoles
import uk.gov.hmrc.apiplatform.modules.gkauth.services.StrideAuthorisationService
import uk.gov.hmrc.apiplatform.modules.submissions.services.SubmissionService

import uk.gov.hmrc.apigatekeeperapprovalsfrontend.config.ErrorHandler
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.models.MarkedSubmissionApplicationRequest
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.services.{ApplicationActionService, SubmissionReviewService}
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.views.html.ConfirmYourDecisionPage

object ConfirmYourDecisionController {
  case class ViewModel(applicationId: ApplicationId, appName: ApplicationName, isFailed: Boolean)
}

@Singleton
class ConfirmYourDecisionController @Inject() (
    strideAuthorisationService: StrideAuthorisationService,
    mcc: MessagesControllerComponents,
    errorHandler: ErrorHandler,
    val applicationActionService: ApplicationActionService,
    val submissionService: SubmissionService,
    confirmYourDecisionPage: ConfirmYourDecisionPage,
    submissionReviewService: SubmissionReviewService
  )(implicit override val ec: ExecutionContext
  ) extends AbstractApplicationController(strideAuthorisationService, mcc, errorHandler) {

  import ConfirmYourDecisionController.*

  def page(applicationId: ApplicationId): Action[AnyContent] =
    loggedInThruStrideWithRoleAndApplicationAndSubmission(GatekeeperRoles.USER)(applicationId) { implicit request =>
      successful(Ok(confirmYourDecisionPage(ViewModel(applicationId, request.application.name, request.markedSubmission.isFail))))
    }

  def action(applicationId: ApplicationId): Action[AnyContent] =
    loggedInThruStrideWithRoleAndApplicationAndSubmission(GatekeeperRoles.USER)(applicationId) { implicit request =>
      request.body.asFormUrlEncoded.getOrElse(Map.empty).get("grant-decision").flatMap(_.headOption) match {
        case Some("decline")                                                   => successful(Redirect(uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.routes.DeclinedJourneyController.provideReasonsPage(applicationId)))
        case Some("grant-with-warnings") if (!request.markedSubmission.isFail) =>
          successful(Redirect(uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.routes.GrantedJourneyController.provideWarningsPage(applicationId)))
        case Some("grant-with-warnings") if (request.markedSubmission.isFail)  =>
          successful(Redirect(uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.routes.GrantedJourneyController.provideEscalatedToPage(applicationId)))
        case Some("grant")                                                     =>
          successful(Redirect(uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.routes.GrantedJourneyController.grantedPage(applicationId)))
        case _                                                                 => successful(Redirect(uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.routes.ConfirmYourDecisionController.page(applicationId)))
      }
    }

}
