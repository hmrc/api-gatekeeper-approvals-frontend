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

import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}

import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models._
import uk.gov.hmrc.apiplatform.modules.common.domain.models.ApplicationId
import uk.gov.hmrc.apiplatform.modules.gkauth.services.StrideAuthorisationService
import uk.gov.hmrc.apiplatform.modules.submissions.services.SubmissionService

import uk.gov.hmrc.apigatekeeperapprovalsfrontend.config.ErrorHandler
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.CheckSandboxController.ViewModel
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.SubmissionReview
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.services.{ApplicationActionService, ApplicationService, SubmissionReviewService, SubscriptionService}
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.views.html.CheckSandboxPage

object CheckSandboxController {

  case class ViewModel(
      appName: ApplicationName,
      applicationId: ApplicationId,
      sandboxAppName: ApplicationName,
      sandboxAppId: ApplicationId,
      sandboxClientId: String,
      apiSubscriptions: String,
      isDeleted: Boolean
    )
}

@Singleton
class CheckSandboxController @Inject() (
    strideAuthorisationService: StrideAuthorisationService,
    mcc: MessagesControllerComponents,
    checkSandboxPage: CheckSandboxPage,
    errorHandler: ErrorHandler,
    submissionReviewService: SubmissionReviewService,
    val applicationActionService: ApplicationActionService,
    val submissionService: SubmissionService,
    val applicationService: ApplicationService,
    val subscriptionService: SubscriptionService
  )(implicit override val ec: ExecutionContext
  ) extends AbstractCheckController(strideAuthorisationService, mcc, errorHandler, submissionReviewService) {

  def checkSandboxPage(applicationId: ApplicationId): Action[AnyContent] = loggedInThruStrideWithApplicationAndSubmission(applicationId) { implicit request =>
    val isDeleted = request.application.state.isDeleted

    (
      for {
        linkedSubordinateApplication <- fromOptionM(
                                          applicationService.fetchLinkedSubordinateApplicationByApplicationId(applicationId),
                                          errorHandler.notFoundTemplate(Request(request, request.messagesApi)).map(NotFound(_))
                                        )
        apiSubscriptions             <- liftF(subscriptionService.fetchSubscriptionsByApplicationId(applicationId))
      } yield Ok(checkSandboxPage(
        ViewModel(
          request.application.name,
          applicationId,
          linkedSubordinateApplication.name,
          linkedSubordinateApplication.id,
          linkedSubordinateApplication.clientId.value,
          apiSubscriptions.map(_.name).mkString(", "),
          isDeleted
        )
      ))
    )
      .merge
  }

  def checkSandboxAction(applicationId: ApplicationId): Action[AnyContent] =
    updateActionStatus(SubmissionReview.Action.CheckSandboxTesting)(applicationId)
}
