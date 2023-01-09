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
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.{ApplicationId, State}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import scala.concurrent.ExecutionContext
import uk.gov.hmrc.apiplatform.modules.gkauth.services.StrideAuthorisationService
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.config.ErrorHandler
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.services.ApplicationActionService
import uk.gov.hmrc.apiplatform.modules.submissions.services.SubmissionService
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.views.html.CheckApplicationNamePage
import scala.concurrent.Future.successful
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.Standard
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.services.SubmissionReviewService
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.SubmissionReview

object CheckApplicationNameController {  
  case class ViewModel(appName: String, applicationId: ApplicationId, isDeleted: Boolean)
}

@Singleton
class CheckApplicationNameController @Inject()(
  strideAuthorisationService: StrideAuthorisationService,
  mcc: MessagesControllerComponents,
  submissionReviewService: SubmissionReviewService,
  errorHandler: ErrorHandler,
  checkApplicationNamePage: CheckApplicationNamePage,
  val applicationActionService: ApplicationActionService,
  val submissionService: SubmissionService
)(implicit override val ec: ExecutionContext) extends AbstractCheckController(strideAuthorisationService, mcc, errorHandler, submissionReviewService) {
  def page(applicationId: ApplicationId): Action[AnyContent] = loggedInThruStrideWithApplicationAndSubmission(applicationId) { implicit request =>
    request.application.access match {
      // Should only be uplifting and checking Standard apps
      case std: Standard if(request.submission.status.isSubmitted) => 
        val isDeleted = request.application.state.name == State.DELETED
        successful(
          Ok(
            checkApplicationNamePage(
              CheckApplicationNameController.ViewModel(
                request.application.name, applicationId, isDeleted
              )
            )
          )
        )
      case _ => successful(BadRequest(errorHandler.badRequestTemplate))
    }
  }

  def action(applicationId: ApplicationId): Action[AnyContent] = 
    updateActionStatus(SubmissionReview.Action.CheckApplicationName)(applicationId)
}
