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

import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}

import uk.gov.hmrc.apigatekeeperapprovalsfrontend.config.ErrorHandler
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.actions.ApplicationActions
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.ApplicationId
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.services.ApplicationActionService
import uk.gov.hmrc.apiplatform.modules.submissions.domain.models._
import uk.gov.hmrc.apiplatform.modules.stride.config.StrideAuthConfig
import uk.gov.hmrc.apiplatform.modules.stride.connectors.AuthConnector
import uk.gov.hmrc.apiplatform.modules.stride.controllers.GatekeeperBaseController
import uk.gov.hmrc.apiplatform.modules.stride.controllers.actions.ForbiddenHandler
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.views.html.ChecklistPage
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models._
import uk.gov.hmrc.apiplatform.modules.submissions.services._
import scala.concurrent.Future.successful

import uk.gov.hmrc.apigatekeeperapprovalsfrontend.services.SubmissionReviewService

object ChecklistController {
  
  case class ChecklistItemStatuses(
    failsAndWarnings: SubmissionReview.Status,
    email: SubmissionReview.Status,
    urls: SubmissionReview.Status,
    sandboxTesting: SubmissionReview.Status,
    passed: SubmissionReview.Status
  )
  case class ViewModel(applicationId: ApplicationId, appName: String, isSuccessful: Boolean, hasWarnings: Boolean, itemStatuses: ChecklistItemStatuses)
}

@Singleton
class ChecklistController @Inject()(
  strideAuthConfig: StrideAuthConfig,
  authConnector: AuthConnector,
  forbiddenHandler: ForbiddenHandler,
  mcc: MessagesControllerComponents,
  checklistPage: ChecklistPage,
  val errorHandler: ErrorHandler,
  val applicationActionService: ApplicationActionService,
  val submissionService: SubmissionService,
  submissionReviewService: SubmissionReviewService
)(implicit override val ec: ExecutionContext) extends GatekeeperBaseController(strideAuthConfig, authConnector, forbiddenHandler, mcc) with ApplicationActions {
  import ChecklistController._

  private def buildChecklistItemStatuses(review: SubmissionReview, markedSubmission: MarkedSubmission): ChecklistItemStatuses = {
    ChecklistItemStatuses(
      failsAndWarnings = review.checkedFailsAndWarnings,
      email = review.emailedResponsibleIndividual,
      urls  = review.checkedUrls,
      sandboxTesting = review.checkedForSandboxTesting,
      passed = review.checkedPassedAnswers
    )
  }

  def checklistPage(applicationId: ApplicationId): Action[AnyContent] = loggedInWithApplicationAndSubmission(applicationId) { implicit request =>
      val appName = request.application.name
      val isSuccessful = ! request.markedSubmission.isFail
      val hasWarnings = request.markedSubmission.isWarn

      for {
        review <- submissionReviewService.findOrCreateReview(request.submission.id, request.submission.latestInstance.index)
        itemStatuses = buildChecklistItemStatuses(review, request.markedSubmission)
      } yield Ok(checklistPage(ViewModel(applicationId, appName, isSuccessful, hasWarnings, itemStatuses)))
  }

  def checklistAction(applicationId: ApplicationId): Action[AnyContent] = loggedInWithApplicationAndSubmission(applicationId) { implicit request =>
    request.body.asFormUrlEncoded.getOrElse(Map.empty).get("submit-action").flatMap(_.headOption) match {
      case Some("checked") => successful(Redirect(uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.routes.ChecksCompletedController.checksCompletedPage(applicationId)))
      case Some("come-back-later") => successful(Redirect(uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.routes.ApplicationSubmissionsController.page(applicationId)))
      case _ => successful(BadRequest("Invalid submit-action found in request"))
    }    
  }
}
