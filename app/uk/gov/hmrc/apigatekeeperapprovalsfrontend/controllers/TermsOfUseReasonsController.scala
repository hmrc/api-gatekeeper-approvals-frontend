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

import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.apiplatform.modules.common.domain.models.ApplicationId
import uk.gov.hmrc.apiplatform.modules.gkauth.services.StrideAuthorisationService
import uk.gov.hmrc.apiplatform.modules.submissions.domain.models.Mark
import uk.gov.hmrc.apiplatform.modules.submissions.services.SubmissionService
import uk.gov.hmrc.play.bootstrap.controller.WithUnsafeDefaultFormBinding

import uk.gov.hmrc.apigatekeeperapprovalsfrontend.config.ErrorHandler
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.services.{ApplicationActionService, SubmissionReviewService}
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.views.html.TermsOfUseReasonsPage

object TermsOfUseReasonsController {

  case class ViewModel(applicationId: ApplicationId, appName: String, hasFails: Boolean, hasWarns: Boolean) {

    lazy val messageKey: String = if (hasFails) { if (hasWarns) "failsandwarns" else "failsonly" }
    else "warnsonly"
  }

  case class ProvideReasonsForm(reasons: String)

  val provideReasonsForm: Form[ProvideReasonsForm] = Form(
    mapping(
      "reasons" -> nonEmptyText
    )(ProvideReasonsForm.apply)(ProvideReasonsForm.unapply)
  )
}

@Singleton
class TermsOfUseReasonsController @Inject() (
    strideAuthorisationService: StrideAuthorisationService,
    mcc: MessagesControllerComponents,
    errorHandler: ErrorHandler,
    val applicationActionService: ApplicationActionService,
    val submissionService: SubmissionService,
    termsOfUseReasonsPage: TermsOfUseReasonsPage,
    submissionReviewService: SubmissionReviewService
  )(implicit override val ec: ExecutionContext
  ) extends AbstractApplicationController(strideAuthorisationService, mcc, errorHandler) with WithUnsafeDefaultFormBinding {

  import TermsOfUseReasonsController._

  def provideReasonsPage(applicationId: ApplicationId) = loggedInThruStrideWithApplicationAndSubmission(applicationId) { implicit request =>
    val hasFails    = request.markedSubmission.markedAnswers.values.toList.contains(Mark.Fail)
    val hasWarnings = request.markedSubmission.isWarn
    successful(Ok(termsOfUseReasonsPage(provideReasonsForm, ViewModel(applicationId, request.application.name, hasFails, hasWarnings))))
  }

  def provideReasonsAction(applicationId: ApplicationId) = loggedInThruStrideWithApplicationAndSubmission(applicationId) { implicit request =>
    def handleValidForm(form: ProvideReasonsForm) = {
      submissionReviewService.updateGrantWarnings(form.reasons)(request.submission.id, request.submission.latestInstance.index).flatMap {
        case Some(value) => successful(Redirect(uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.routes.TermsOfUseFailedJourneyController.emailAddressesPage(applicationId)))
        case None        => {
          logger.warn("Persisting reasons failed")
          errorHandler.badRequestTemplate.map(BadRequest(_))
        }
      }
    }

    def handleInvalidForm(form: Form[ProvideReasonsForm]) = {
      val hasFails    = request.markedSubmission.markedAnswers.values.toList.contains(Mark.Fail)
      val hasWarnings = request.markedSubmission.isWarn
      successful(BadRequest(termsOfUseReasonsPage(form, ViewModel(applicationId, request.application.name, hasFails, hasWarnings))))
    }

    TermsOfUseReasonsController.provideReasonsForm.bindFromRequest().fold(handleInvalidForm, handleValidForm)
  }
}
