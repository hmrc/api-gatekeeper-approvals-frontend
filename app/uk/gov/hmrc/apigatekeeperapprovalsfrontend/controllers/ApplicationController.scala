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
import uk.gov.hmrc.http.HeaderCarrier

import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}

import uk.gov.hmrc.apigatekeeperapprovalsfrontend.config.ErrorHandler
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.actions.ApplicationActions
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.ApplicationId
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.services.ApplicationActionService
import uk.gov.hmrc.modules.submissions.domain.models._
import uk.gov.hmrc.modules.stride.config.StrideAuthConfig
import uk.gov.hmrc.modules.stride.connectors.AuthConnector
import uk.gov.hmrc.modules.stride.controllers.GatekeeperBaseController
import uk.gov.hmrc.modules.stride.controllers.actions.ForbiddenHandler
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.views.html.ApplicationChecklistPage
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models._
import uk.gov.hmrc.modules.submissions.services._
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import play.api.mvc.Request

import scala.concurrent.Future.successful

object ApplicationController {
  sealed trait ChecklistItemStatus
  case object Complete extends ChecklistItemStatus
  case object InProgress extends ChecklistItemStatus
  case object NotStarted extends ChecklistItemStatus

  case class ChecklistItemStatuses(
    failsAndWarnings: ChecklistItemStatus,
    email: ChecklistItemStatus,
    urls: ChecklistItemStatus,
    sandboxTesting: ChecklistItemStatus,
    passed: ChecklistItemStatus
  )
  case class ViewModel(applicationId: ApplicationId, appName: String, isSuccessful: Boolean, hasWarnings: Boolean, itemStatuses: ChecklistItemStatuses)
}

@Singleton
class ApplicationController @Inject()(
  strideAuthConfig: StrideAuthConfig,
  authConnector: AuthConnector,
  forbiddenHandler: ForbiddenHandler,
  mcc: MessagesControllerComponents,
  applicationChecklistPage: ApplicationChecklistPage,
  val errorHandler: ErrorHandler,
  val applicationActionService: ApplicationActionService,
  val submissionService: SubmissionService
)(implicit override val ec: ExecutionContext) extends GatekeeperBaseController(strideAuthConfig, authConnector, forbiddenHandler, mcc) with ApplicationActions {
  import ApplicationController._

  implicit override def hc(implicit request: Request[_]): HeaderCarrier =
    HeaderCarrierConverter.fromRequestAndSession(request, request.session) //TODO

  private def buildChecklistItemStatuses(markedSubmission: MarkedSubmission): ChecklistItemStatuses = {
    ChecklistItemStatuses(NotStarted, NotStarted, NotStarted, NotStarted, NotStarted)
  }

  def applicationPage(applicationId: ApplicationId): Action[AnyContent] = loggedInWithApplicationAndSubmission(applicationId) { implicit request =>
      val appName = request.application.name
      val isSuccessful = ! request.markedSubmission.isFail
      val hasWarnings = request.markedSubmission.hasWarnings
      val itemStatuses = buildChecklistItemStatuses(request.markedSubmission)

      successful(Ok(applicationChecklistPage(ViewModel(applicationId, appName, isSuccessful, hasWarnings, itemStatuses))))
  }
}
