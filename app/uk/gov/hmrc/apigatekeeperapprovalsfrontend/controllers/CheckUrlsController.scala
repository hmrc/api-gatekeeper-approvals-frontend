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
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.ApplicationId
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import scala.concurrent.ExecutionContext
import uk.gov.hmrc.apiplatform.modules.stride.config.StrideAuthConfig
import uk.gov.hmrc.apiplatform.modules.stride.controllers.actions.ForbiddenHandler
import uk.gov.hmrc.apiplatform.modules.stride.connectors.AuthConnector
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.config.ErrorHandler
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.services.ApplicationActionService
import uk.gov.hmrc.apiplatform.modules.submissions.services.SubmissionService
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.views.html.CheckUrlsPage
import scala.concurrent.Future.successful
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.Standard
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.services.SubmissionReviewService
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.SubmissionReview

object CheckUrlsController {
  case class ViewModel(appName: String, applicationId: ApplicationId, organisationUrl: Option[String], privacyPolicyUrl: Option[String], termsAndConditionsUrl: Option[String]) {
    lazy val hasOrganisationUrl: Boolean = organisationUrl.isDefined

    lazy val privacyPolicyInDesktop = privacyPolicyUrl.exists(_ == "desktop")
    lazy val hasPrivacyPolicyUrl    = privacyPolicyUrl.exists(_ != "desktop")

    lazy val termsAndConditionsInDesktop = termsAndConditionsUrl.exists(_ == "desktop")
    lazy val hasTermsAndConditionsUrl    = termsAndConditionsUrl.exists(_ != "desktop")
  }
}

@Singleton
class CheckUrlsController @Inject()(
  strideAuthConfig: StrideAuthConfig,
  authConnector: AuthConnector,
  forbiddenHandler: ForbiddenHandler,
  mcc: MessagesControllerComponents,
  submissionReviewService: SubmissionReviewService,
  errorHandler: ErrorHandler,
  checkUrlsPage: CheckUrlsPage,
  val applicationActionService: ApplicationActionService,
  val submissionService: SubmissionService
)(implicit override val ec: ExecutionContext) extends AbstractCheckController(strideAuthConfig, authConnector, forbiddenHandler, mcc, errorHandler, submissionReviewService) {
  def checkUrlsPage(applicationId: ApplicationId): Action[AnyContent] = loggedInWithApplicationAndSubmission(applicationId) { implicit request =>
    request.application.access match {
      // Should only be uplifting and checking Standard apps
      case std: Standard => 
        successful(
          Ok(
            checkUrlsPage(
              CheckUrlsController.ViewModel(
                request.application.name, applicationId,
                std.organisationUrl, 
                std.privacyPolicyUrl, 
                std.termsAndConditionsUrl
              )
            )
          )
        )
      case _ => successful(BadRequest(errorHandler.badRequestTemplate))
    }
  }

  def checkUrlsAction(applicationId: ApplicationId): Action[AnyContent] = 
    updateActionStatus(SubmissionReview.Action.CheckUrls)(applicationId)
}
