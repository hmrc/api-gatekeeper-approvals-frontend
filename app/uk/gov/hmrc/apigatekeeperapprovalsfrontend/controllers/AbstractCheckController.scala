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

import scala.concurrent.Future.successful
import scala.concurrent.{ExecutionContext, Future}

import play.api.mvc._

import uk.gov.hmrc.apiplatform.modules.applications.submissions.domain.models.SubmissionId
import uk.gov.hmrc.apiplatform.modules.common.domain.models.ApplicationId
import uk.gov.hmrc.apiplatform.modules.gkauth.services.StrideAuthorisationService

import uk.gov.hmrc.apigatekeeperapprovalsfrontend.config.ErrorHandler
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.models.MarkedSubmissionApplicationRequest
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.SubmissionReview
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.services.SubmissionReviewService

abstract class AbstractCheckController(
    strideAuthorisationService: StrideAuthorisationService,
    mcc: MessagesControllerComponents,
    errorHandler: ErrorHandler,
    submissionReviewService: SubmissionReviewService
  )(implicit override val ec: ExecutionContext
  ) extends AbstractApplicationController(strideAuthorisationService, mcc, errorHandler) {

  type Fn = (SubmissionReview.Status) => (SubmissionId, Int) => Future[Option[SubmissionReview]]

  def logBadRequest(reviewAction: SubmissionReview.Action)(errorMsg: String)(implicit request: MarkedSubmissionApplicationRequest[_]): Future[Result] = {
    val description = SubmissionReview.Action.toText(reviewAction)
    logger.error(s"$description : $errorMsg for ${request.submission.id}-${request.submission.latestInstance.index}")
    errorHandler.badRequestTemplate.map(BadRequest(_))
  }

  def deriveStatusFromAction(formAction: String): Option[SubmissionReview.Status] = formAction match {
    case "checked"         => Some(SubmissionReview.Status.Completed)
    case "come-back-later" => Some(SubmissionReview.Status.InProgress)
    case _                 => None
  }

  def updateActionStatus(
      reviewAction: SubmissionReview.Action
    )(
      applicationId: ApplicationId
    ): Action[AnyContent] = loggedInThruStrideWithApplicationAndSubmission(applicationId) { implicit request =>
    val ok  = Redirect(uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.routes.ChecklistController.checklistPage(applicationId))
    val log = logBadRequest(reviewAction) _

    (
      for {
        formAction <- fromOptionM(
                        successful(request.body.asFormUrlEncoded.getOrElse(Map.empty).get("submit-action").flatMap(_.headOption)),
                        log("No submit-action found in request")
                      )
        newStatus  <- fromOptionM(
                        successful(deriveStatusFromAction(formAction)),
                        log("Invalid submit-action found in request")
                      )
        _          <- fromOptionM(
                        submissionReviewService.updateActionStatus(reviewAction, newStatus)(request.submission.id, request.submission.latestInstance.index),
                        log("Failed to find existing review")
                      )
      } yield ok
    ).fold(identity(_), identity(_))
  }
}
