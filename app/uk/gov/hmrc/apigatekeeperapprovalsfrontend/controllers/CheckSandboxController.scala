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

import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.config.ErrorHandler
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.CheckSandboxController.ViewModel
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.{ApplicationId, Standard}
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.services.{ApplicationActionService, ApplicationService, SubmissionReviewService, SubscriptionService}
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.views.html.CheckSandboxPage
import uk.gov.hmrc.apiplatform.modules.stride.config.StrideAuthConfig
import uk.gov.hmrc.apiplatform.modules.stride.connectors.AuthConnector
import uk.gov.hmrc.apiplatform.modules.stride.controllers.actions.ForbiddenHandler
import uk.gov.hmrc.apiplatform.modules.submissions.services.SubmissionService

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

object CheckSandboxController {
  case class ViewModel(
    appName: String,
    applicationId: ApplicationId,
    sandboxAppName: String,
    sandboxAppId: ApplicationId,
    sandboxClientId: String,
    apiSubscriptions: String
  )
}

@Singleton
class CheckSandboxController @Inject()(
  strideAuthConfig: StrideAuthConfig,
  authConnector: AuthConnector,
  forbiddenHandler: ForbiddenHandler,
  mcc: MessagesControllerComponents,
  page: CheckSandboxPage,
  errorHandler: ErrorHandler,
  val applicationActionService: ApplicationActionService,
  val submissionService: SubmissionService,
  submissionReviewService: SubmissionReviewService,
  applicationService: ApplicationService,
  subscriptionService: SubscriptionService
)(implicit override val ec: ExecutionContext) extends AbstractCheckController(strideAuthConfig, authConnector, forbiddenHandler, mcc, errorHandler) {
  def checkSandboxPage(applicationId: ApplicationId): Action[AnyContent] = loggedInWithApplicationAndSubmission(applicationId) { implicit request =>
    for {
      linkedSubordinateApplication <- applicationService.fetchLinkedSubordinateApplicationByApplicationId(applicationId)
      apiSubscriptions <- subscriptionService.fetchSubscriptionsByApplicationId(applicationId)
    } yield linkedSubordinateApplication.fold[Result](NotFound)(sandboxApplication => Ok(page(
      ViewModel(
        request.application.name,
        applicationId,
        sandboxApplication.name,
        sandboxApplication.id,
        sandboxApplication.clientId.value,
        apiSubscriptions.map(_.name).mkString(",")
      )
    )))
  }

  def checkSandboxAction(applicationId: ApplicationId): Action[AnyContent] =
    updateReviewAction("checkSandboxAction", submissionReviewService.updateCheckedForSandboxTestingStatus _)(applicationId)
}