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

import uk.gov.hmrc.modules.stride.config.StrideAuthConfig
import uk.gov.hmrc.modules.stride.connectors.AuthConnector
import uk.gov.hmrc.modules.stride.controllers.actions.ForbiddenHandler
import scala.concurrent.ExecutionContext

import scala.concurrent.Future
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.SubmissionReview
import uk.gov.hmrc.modules.stride.controllers.GatekeeperBaseController
import uk.gov.hmrc.apiplatform.modules.submissions.domain.models.Submission
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.models.MarkedSubmissionApplicationRequest
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.ApplicationId
import play.api.mvc._
import uk.gov.hmrc.apiplatform.modules.common.services.ApplicationLogger
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.config.ErrorHandler
import uk.gov.hmrc.apiplatform.modules.common.services.EitherTHelper
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.actions.ApplicationActions


abstract class AbstractCheckController(
  strideAuthConfig: StrideAuthConfig,
  authConnector: AuthConnector,
  forbiddenHandler: ForbiddenHandler,
  mcc: MessagesControllerComponents,
  val errorHandler: ErrorHandler
)(implicit override val ec: ExecutionContext) extends GatekeeperBaseController(strideAuthConfig, authConnector, forbiddenHandler, mcc) with ApplicationActions with EitherTHelper[Result] with ApplicationLogger {

  type Fn = (SubmissionReview.Status) => (Submission.Id, Int) => Future[Option[SubmissionReview]]

  def logBadRequest(location: String)(errorMsg: String)(implicit request: MarkedSubmissionApplicationRequest[_]): Result = {
    logger.error(s"$location : $errorMsg for ${request.submission.id}-${request.submission.latestInstance.index}")
    BadRequest(errorHandler.badRequestTemplate)
  }

  def actionAsStatus(action: String): Option[SubmissionReview.Status] = action match {
    case "checked"          => Some(SubmissionReview.Status.ReviewCompleted)
    case "come-back-later"  => Some(SubmissionReview.Status.ReviewInProgress)
    case _                  => None
  }

  def updateReviewAction(location: String, updateSubmissionReview: Fn)(applicationId: ApplicationId): Action[AnyContent] = loggedInWithApplicationAndSubmission(applicationId) { implicit request =>
    val ok = Redirect(uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.routes.ApplicationController.applicationPage(applicationId))
    val log = logBadRequest(location) _

    (
      for {
        action <- fromOption(request.body.asFormUrlEncoded.getOrElse(Map.empty).get("submit-action").flatMap(_.headOption), log("No submit-action found in request"))
        status <- fromOption(actionAsStatus(action), log("Invalid submit-action found in request"))
        _      <- fromOptionF(updateSubmissionReview(status)(request.submission.id, request.submission.latestInstance.index), log("Failed to find existing review"))
      } yield ok
    ).fold(identity(_), identity(_))
  }

}