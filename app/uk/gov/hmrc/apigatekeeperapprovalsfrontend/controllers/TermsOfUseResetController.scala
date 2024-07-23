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
import uk.gov.hmrc.apiplatform.modules.common.services.EitherTHelper
import uk.gov.hmrc.apiplatform.modules.gkauth.services.StrideAuthorisationService
import uk.gov.hmrc.apiplatform.modules.submissions.services.SubmissionService

import uk.gov.hmrc.apigatekeeperapprovalsfrontend.config.ErrorHandler
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.services.{ApplicationActionService, SubmissionReviewService}
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.views.html._

object TermsOfUseResetController {

  case class ViewModel(applicationId: ApplicationId, appName: String)

  case class ProvideNotesForm(notes: String)

  val provideNotesForm: Form[ProvideNotesForm] = Form(
    mapping(
      "notes" -> nonEmptyText
    )(ProvideNotesForm.apply)(ProvideNotesForm.unapply)
  )
}

@Singleton
class TermsOfUseResetController @Inject() (
    strideAuthorisationService: StrideAuthorisationService,
    mcc: MessagesControllerComponents,
    errorHandler: ErrorHandler,
    submissionReviewService: SubmissionReviewService,
    termsOfUseResetPage: TermsOfUseResetPage,
    termsOfUseConfirmPage: TermsOfUseResetConfirmationPage,
    val applicationActionService: ApplicationActionService,
    val submissionService: SubmissionService
  )(implicit override val ec: ExecutionContext
  ) extends AbstractCheckController(strideAuthorisationService, mcc, errorHandler, submissionReviewService) {

  import TermsOfUseResetController._

  def page(applicationId: ApplicationId) = loggedInThruStrideWithApplicationAndSubmission(applicationId) { implicit request =>
    successful(Ok(termsOfUseResetPage(provideNotesForm, ViewModel(applicationId, request.application.name))))
  }

  def action(applicationId: ApplicationId) = loggedInThruStrideWithApplicationAndSubmission(applicationId) { implicit request =>
    def handleValidForm(form: ProvideNotesForm) = {
      def failure(err: String) =
        errorHandler.standardErrorTemplate(
          "Terms of use reset",
          "Error resetting of use",
          err
        ).map(BadRequest(_))

      lazy val success = Redirect(uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.routes.TermsOfUseResetController.confirmationPage(applicationId))

      val E = EitherTHelper.make[String]

      E.fromEitherF(submissionService.resetForTouUplift(applicationId, request.name.get, form.notes))
        .map(_ => success)
        .leftSemiflatMap(err => failure(err))
        .merge
    }

    def handleInvalidForm(form: Form[ProvideNotesForm]) = {
      successful(BadRequest(termsOfUseResetPage(form, ViewModel(applicationId, request.application.name))))
    }

    TermsOfUseResetController.provideNotesForm.bindFromRequest().fold(handleInvalidForm, handleValidForm)

  }

  def confirmationPage(applicationId: ApplicationId) = loggedInThruStrideWithApplicationAndSubmission(applicationId) { implicit request =>
    successful(Ok(termsOfUseConfirmPage(ViewModel(applicationId, request.application.name))))
  }
}
