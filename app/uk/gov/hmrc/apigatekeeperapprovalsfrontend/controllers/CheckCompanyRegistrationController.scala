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

import uk.gov.hmrc.apiplatform.modules.applications.access.domain.models.Access
import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models._
import uk.gov.hmrc.apiplatform.modules.common.domain.models.ApplicationId
import uk.gov.hmrc.apiplatform.modules.gkauth.services.StrideAuthorisationService
import uk.gov.hmrc.apiplatform.modules.submissions.domain.services.CompanyDetailsExtractor
import uk.gov.hmrc.apiplatform.modules.submissions.services.SubmissionService

import uk.gov.hmrc.apigatekeeperapprovalsfrontend.config.ErrorHandler
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.SubmissionReview
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.services.{ApplicationActionService, SubmissionReviewService}
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.views.html.CheckCompanyRegistrationPage

case class CompanyRegistrationDetails(registrationType: String, registrationValue: Option[String])

object CheckCompanyRegistrationController {

  case class ViewModel(appName: ApplicationName, applicationId: ApplicationId, registrationType: String, registrationValue: Option[String], isDeleted: Boolean) {
    lazy val hasRegistrationDetails: Boolean = registrationValue.isDefined
  }
}

@Singleton
class CheckCompanyRegistrationController @Inject() (
    strideAuthorisationService: StrideAuthorisationService,
    mcc: MessagesControllerComponents,
    checkCompanyRegistrationPage: CheckCompanyRegistrationPage,
    errorHandler: ErrorHandler,
    submissionReviewService: SubmissionReviewService,
    val applicationActionService: ApplicationActionService,
    val submissionService: SubmissionService
  )(implicit override val ec: ExecutionContext
  ) extends AbstractCheckController(strideAuthorisationService, mcc, errorHandler, submissionReviewService) {

  def page(applicationId: ApplicationId): Action[AnyContent] = loggedInThruStrideWithApplicationAndSubmission(applicationId) { implicit request =>
    val companyDetails = CompanyDetailsExtractor(request.submission)

    (request.application.access, companyDetails) match {
      // Should only be uplifting and checking Standard apps
      case (std: Access.Standard, Some(details)) if (request.submission.status.isSubmitted) =>
        val isDeleted = request.application.state.isDeleted
        successful(
          Ok(
            checkCompanyRegistrationPage(
              CheckCompanyRegistrationController.ViewModel(
                request.application.name,
                applicationId,
                details.registrationType,
                details.registrationValue,
                isDeleted
              )
            )
          )
        )
      case _                                                                                => errorHandler.badRequestTemplate.map(BadRequest(_))
    }
  }

  def action(applicationId: ApplicationId): Action[AnyContent] =
    updateActionStatus(SubmissionReview.Action.CheckCompanyRegistration)(applicationId)
}
