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

import cats.data.NonEmptyList

import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.play.bootstrap.controller.WithUrlEncodedOnlyFormBinding

import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.ApplicationName
import uk.gov.hmrc.apiplatform.modules.commands.applications.domain.models.{CommandFailure, CommandFailures}
import uk.gov.hmrc.apiplatform.modules.common.domain.models.ApplicationId
import uk.gov.hmrc.apiplatform.modules.common.services.EitherTHelper
import uk.gov.hmrc.apiplatform.modules.gkauth.services.StrideAuthorisationService
import uk.gov.hmrc.apiplatform.modules.submissions.services.SubmissionService

import uk.gov.hmrc.apigatekeeperapprovalsfrontend.config.ErrorHandler
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.services.ApplicationActionService
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.views.html.TermsOfUseNotesPage

object TermsOfUseNotesController {

  case class ViewModel(applicationId: ApplicationId, appName: ApplicationName)

  case class ProvideNotesForm(notes: String)

  val provideNotesForm: Form[ProvideNotesForm] = Form(
    mapping(
      "notes" -> nonEmptyText
    )(ProvideNotesForm.apply)(ProvideNotesForm.unapply)
  )
}

@Singleton
class TermsOfUseNotesController @Inject() (
    strideAuthorisationService: StrideAuthorisationService,
    mcc: MessagesControllerComponents,
    errorHandler: ErrorHandler,
    val applicationActionService: ApplicationActionService,
    val submissionService: SubmissionService,
    termsOfUseNotesPage: TermsOfUseNotesPage
  )(implicit override val ec: ExecutionContext
  ) extends AbstractApplicationController(strideAuthorisationService, mcc, errorHandler) with WithUrlEncodedOnlyFormBinding {

  import TermsOfUseNotesController._

  def page(applicationId: ApplicationId) = loggedInThruStrideWithApplicationAndSubmission(applicationId) { implicit request =>
    successful(Ok(termsOfUseNotesPage(provideNotesForm, ViewModel(applicationId, request.application.name))))
  }

  def action(applicationId: ApplicationId) = loggedInThruStrideWithApplicationAndSubmission(applicationId) { implicit request =>
    def handleValidForm(form: ProvideNotesForm) = {
      def failure(errs: NonEmptyList[CommandFailure]) =
        errorHandler.standardErrorTemplate(
          "Terms of use grant",
          "Error granting terms of use",
          errs.toList.map(error => CommandFailures.describe(error)).mkString(", ")
        ).map(BadRequest(_))

      lazy val success = Redirect(uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.routes.TermsOfUseGrantedConfirmationController.page(applicationId))

      val E = EitherTHelper.make[NonEmptyList[CommandFailure]]

      E.fromEitherF(submissionService.grantForTouUplift(applicationId, request.name.get, form.notes, None))
        .map(_ => success)
        .leftSemiflatMap(err => failure(err))
        .merge
    }

    def handleInvalidForm(form: Form[ProvideNotesForm]) = {
      successful(BadRequest(termsOfUseNotesPage(form, ViewModel(applicationId, request.application.name))))
    }

    TermsOfUseNotesController.provideNotesForm.bindFromRequest().fold(handleInvalidForm, handleValidForm)
  }
}
