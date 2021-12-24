/*
 * Copyright 2021 HM Revenue & Customs
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

import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}

import uk.gov.hmrc.apigatekeeperapprovalsfrontend.config.ErrorHandler
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.actions.ApplicationActions
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.ApplicationId
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.services.ApplicationActionService
import uk.gov.hmrc.modules.stride.config.StrideAuthConfig
import uk.gov.hmrc.modules.stride.connectors.AuthConnector
import uk.gov.hmrc.modules.stride.controllers.GatekeeperBaseController
import uk.gov.hmrc.modules.stride.controllers.actions.ForbiddenHandler
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.views.html.ApplicationChecklistPage


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
  case class ViewModel(appName: String, isSuccessful: Boolean, hasWarnings: Boolean, itemStatuses: ChecklistItemStatuses)
}

@Singleton
class ApplicationController @Inject()(
  strideAuthConfig: StrideAuthConfig,
  authConnector: AuthConnector,
  forbiddenHandler: ForbiddenHandler,
  mcc: MessagesControllerComponents,
  applicationChecklistPage: ApplicationChecklistPage,
  val errorHandler: ErrorHandler,
  val applicationActionService: ApplicationActionService
)(implicit override val ec: ExecutionContext) extends GatekeeperBaseController(strideAuthConfig, authConnector, forbiddenHandler, mcc) with ApplicationActions {
  import ApplicationController._

  def getApplication(applicationId: ApplicationId): Action[AnyContent] = loggedInWithApplication(applicationId) { implicit request =>

    val itemStatuses = ChecklistItemStatuses(Complete, InProgress, NotStarted, NotStarted, NotStarted)
    successful(Ok(applicationChecklistPage(ViewModel(request.application.name, false, true, itemStatuses))))
  }
}
