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

import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.apiplatform.modules.gkauth.services.StrideAuthorisationService
import uk.gov.hmrc.apiplatform.modules.submissions.domain.models.Submission.Status._
import uk.gov.hmrc.apiplatform.modules.submissions.domain.models._
import uk.gov.hmrc.apiplatform.modules.submissions.services.SubmissionService

import uk.gov.hmrc.apigatekeeperapprovalsfrontend.config.ErrorHandler
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.ApplicationId
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.services.ApplicationActionService
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.views.html.ProductionAccessPage

object ProductionAccessController {

  case class ViewModel(
      appName: String,
      applicationId: ApplicationId,
      submitterEmail: String,
      submittedDate: String,
      grantedName: String,
      grantedDate: String,
      warnings: Option[String],
      escalatedTo: Option[String],
      index: Int
    )
}

@Singleton
class ProductionAccessController @Inject() (
    strideAuthorisationService: StrideAuthorisationService,
    mcc: MessagesControllerComponents,
    errorHandler: ErrorHandler,
    productionAccessPage: ProductionAccessPage,
    val applicationActionService: ApplicationActionService,
    val submissionService: SubmissionService
  )(implicit override val ec: ExecutionContext
  ) extends AbstractApplicationController(strideAuthorisationService, mcc, errorHandler) {

  import ProductionAccessController._

  def page(applicationId: ApplicationId) = loggedInThruStrideWithApplicationAndSubmission(applicationId) { implicit request =>
    val appName  = request.application.name
    val instance = request.markedSubmission.submission.latestInstance

    (instance.statusHistory.head, instance.statusHistory.find(_.isSubmitted)) match {
      case (Granted(grantedTimestamp, grantedName, _), Some(Submission.Status.Submitted(submittedTimestamp, requestedBy)))                                 =>
        successful(Ok(productionAccessPage(ViewModel(
          appName,
          applicationId,
          requestedBy,
          submittedTimestamp.asText,
          grantedName,
          grantedTimestamp.asText,
          None,
          None,
          instance.index
        ))))
      case (GrantedWithWarnings(grantedTimestamp, grantedName, warnings, escalatedTo), Some(Submission.Status.Submitted(submittedTimestamp, requestedBy))) =>
        successful(Ok(productionAccessPage(ViewModel(
          appName,
          applicationId,
          requestedBy,
          submittedTimestamp.asText,
          grantedName,
          grantedTimestamp.asText,
          Some(warnings),
          escalatedTo,
          instance.index
        ))))
      case _                                                                                                                                               =>
        logger.warn("Unexpectedly could not find a submitted status for an instance with a granted status")
        successful(BadRequest(errorHandler.badRequestTemplate(request)))
    }

  }
}
