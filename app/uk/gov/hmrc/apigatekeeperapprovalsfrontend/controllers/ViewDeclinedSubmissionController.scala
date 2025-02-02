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

import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.ApplicationName
import uk.gov.hmrc.apiplatform.modules.common.domain.models.ApplicationId
import uk.gov.hmrc.apiplatform.modules.gkauth.services.StrideAuthorisationService
import uk.gov.hmrc.apiplatform.modules.submissions.domain.models.Submission.Status.Declined
import uk.gov.hmrc.apiplatform.modules.submissions.domain.models._
import uk.gov.hmrc.apiplatform.modules.submissions.services.SubmissionService

import uk.gov.hmrc.apigatekeeperapprovalsfrontend.config.ErrorHandler
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.services.{ApplicationActionService, SubmissionReviewService}
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.views.html.ViewDeclinedSubmissionPage

object ViewDeclinedSubmissionController {

  case class ViewModel(
      appName: ApplicationName,
      applicationId: ApplicationId,
      submitterEmail: String,
      submittedDate: String,
      declinedName: String,
      declinedDate: String,
      reasons: String,
      index: Int
    )
}

@Singleton
class ViewDeclinedSubmissionController @Inject() (
    strideAuthorisationService: StrideAuthorisationService,
    mcc: MessagesControllerComponents,
    viewDeclinedSubmissionPage: ViewDeclinedSubmissionPage,
    errorHandler: ErrorHandler,
    submissionReviewService: SubmissionReviewService,
    val applicationActionService: ApplicationActionService,
    val submissionService: SubmissionService
  )(implicit override val ec: ExecutionContext
  ) extends AbstractApplicationController(strideAuthorisationService, mcc, errorHandler) {

  import ViewDeclinedSubmissionController._

  def page(applicationId: ApplicationId, index: Int) = loggedInThruStrideWithApplicationAndSubmission(applicationId) { implicit request =>
    val appName = request.application.name

    request.markedSubmission.submission.instances.find(i => i.index == index && i.isDeclined).fold(
      errorHandler.badRequestTemplate(request).map(BadRequest(_))
    )(instance => {
      (instance.statusHistory.head, instance.statusHistory.find(_.isSubmitted)) match {
        case (Declined(declinedTimestamp, declinedName, reasons), Some(Submission.Status.Submitted(submittedTimestamp, requestedBy))) =>
          successful(Ok(viewDeclinedSubmissionPage(ViewModel(
            appName,
            applicationId,
            requestedBy,
            submittedTimestamp.asText,
            declinedName,
            declinedTimestamp.asText,
            reasons,
            instance.index
          ))))
        case _                                                                                                                        =>
          logger.warn("Unexpectedly could not find a submitted status for an instance with a declined status")
          errorHandler.badRequestTemplate(request).map(BadRequest(_))
      }
    })

  }
}
