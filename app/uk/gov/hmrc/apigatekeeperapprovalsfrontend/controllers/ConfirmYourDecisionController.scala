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

import cats.data.EitherT

import play.api.mvc.{MessagesControllerComponents, _}
import uk.gov.hmrc.apiplatform.modules.common.domain.models.ApplicationId
import uk.gov.hmrc.apiplatform.modules.gkauth.services.StrideAuthorisationService
import uk.gov.hmrc.apiplatform.modules.submissions.services.SubmissionService

import uk.gov.hmrc.apigatekeeperapprovalsfrontend.config.ErrorHandler
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.models.MarkedSubmissionApplicationRequest
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.services.{ApplicationActionService, SubmissionReviewService}
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.views.html.ConfirmYourDecisionPage

object ConfirmYourDecisionController {
  case class ViewModel(applicationId: ApplicationId, appName: String, isFailed: Boolean)
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

  import ConfirmYourDecisionController._

  def page(applicationId: ApplicationId) = loggedInThruStrideWithApplicationAndSubmission(applicationId) { implicit request =>
    successful(Ok(confirmYourDecisionPage(ViewModel(applicationId, request.application.name, request.markedSubmission.isFail))))
  }

  def action(applicationId: ApplicationId) = loggedInThruStrideWithApplicationAndSubmission(applicationId) { implicit request =>
    request.body.asFormUrlEncoded.getOrElse(Map.empty).get("grant-decision").flatMap(_.headOption) match {
      case Some("decline")                                                   => successful(Redirect(uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.routes.DeclinedJourneyController.provideReasonsPage(applicationId)))
      case Some("grant-with-warnings") if (!request.markedSubmission.isFail) =>
        successful(Redirect(uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.routes.GrantedJourneyController.provideWarningsPage(applicationId)))
      case Some("grant-with-warnings") if (request.markedSubmission.isFail)  =>
        successful(Redirect(uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.routes.GrantedJourneyController.provideEscalatedToPage(applicationId)))
      case Some("grant")                                                     => grantAccess(applicationId)
      case _                                                                 => successful(Redirect(uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.routes.ConfirmYourDecisionController.page(applicationId)))
    }
  }

  private def grantAccess(applicationId: ApplicationId)(implicit request: MarkedSubmissionApplicationRequest[AnyContent]) = {
    (
      for {
        review      <- fromOptionF(submissionReviewService.findReview(request.submission.id, request.submission.latestInstance.index), BadRequest("Unable to find submission review"))
        application <- EitherT(submissionService.grant(applicationId, request.name.get)).leftMap(InternalServerError(_))
      } yield Redirect(uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.routes.GrantedJourneyController.grantedPage(applicationId).url)
    ).fold(identity(_), identity(_))
  }

}
