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

import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.apiplatform.modules.applications.domain.models.{PrivacyPolicyLocation, TermsAndConditionsLocation}
import uk.gov.hmrc.apiplatform.modules.common.domain.models.ApplicationId
import uk.gov.hmrc.apiplatform.modules.gkauth.services.StrideAuthorisationService
import uk.gov.hmrc.apiplatform.modules.submissions.services.SubmissionService

import uk.gov.hmrc.apigatekeeperapprovalsfrontend.config.ErrorHandler
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models._
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.services.{ApplicationActionService, SubmissionReviewService}
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.views.html.CheckUrlsPage

object CheckUrlsController {

  case class ViewModel(
      appName: String,
      applicationId: ApplicationId,
      organisationUrl: Option[String],
      privacyPolicyLocation: PrivacyPolicyLocation,
      termsAndConditionsLocation: TermsAndConditionsLocation,
      isDeleted: Boolean
    ) {
    lazy val hasOrganisationUrl: Boolean = organisationUrl.isDefined
  }
}

@Singleton
class CheckUrlsController @Inject() (
    strideAuthorisationService: StrideAuthorisationService,
    mcc: MessagesControllerComponents,
    submissionReviewService: SubmissionReviewService,
    errorHandler: ErrorHandler,
    checkUrlsPage: CheckUrlsPage,
    val applicationActionService: ApplicationActionService,
    val submissionService: SubmissionService
  )(implicit override val ec: ExecutionContext
  ) extends AbstractCheckController(strideAuthorisationService, mcc, errorHandler, submissionReviewService) {

  def checkUrlsPage(applicationId: ApplicationId): Action[AnyContent] = loggedInThruStrideWithApplicationAndSubmission(applicationId) { implicit request =>
    request.application.access match {
      // Should only be uplifting and checking Standard apps having gone thru uplift
      case std @ Standard(_, _, Some(importantSubmissionData)) =>
        val isDeleted = request.application.state.name == State.DELETED
        successful(
          Ok(
            checkUrlsPage(
              CheckUrlsController.ViewModel(
                request.application.name,
                applicationId,
                importantSubmissionData.organisationUrl,
                importantSubmissionData.privacyPolicyLocation,
                importantSubmissionData.termsAndConditionsLocation,
                isDeleted
              )
            )
          )
        )
      case _                                                   => successful(BadRequest(errorHandler.badRequestTemplate))
    }
  }

  def checkUrlsAction(applicationId: ApplicationId): Action[AnyContent] =
    updateActionStatus(SubmissionReview.Action.CheckUrls)(applicationId)
}
