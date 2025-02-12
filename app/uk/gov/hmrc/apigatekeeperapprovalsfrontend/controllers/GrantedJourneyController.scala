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

import cats.data.{EitherT, NonEmptyList}

import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.play.bootstrap.controller.WithUnsafeDefaultFormBinding

import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models._
import uk.gov.hmrc.apiplatform.modules.commands.applications.domain.models.CommandFailures
import uk.gov.hmrc.apiplatform.modules.commands.applications.domain.models.CommandFailures.GenericFailure
import uk.gov.hmrc.apiplatform.modules.common.domain.models.ApplicationId
import uk.gov.hmrc.apiplatform.modules.gkauth.services.StrideAuthorisationService
import uk.gov.hmrc.apiplatform.modules.submissions.services.SubmissionService

import uk.gov.hmrc.apigatekeeperapprovalsfrontend.config.ErrorHandler
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.services.{ApplicationActionService, SubmissionReviewService}
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.views.html.{ApplicationApprovedPage, ProvideEscalatedToForGrantingPage, ProvideWarningsForGrantingPage}

object GrantedJourneyController {
  case class ViewModel(appName: ApplicationName, applicationId: ApplicationId)

  case class ProvideWarningsForm(warnings: String)

  val provideWarningsForm: Form[ProvideWarningsForm] = Form(
    mapping(
      "warnings" -> nonEmptyText
    )(ProvideWarningsForm.apply)(ProvideWarningsForm.unapply)
  )

  case class ProvideEscalatedToForm(firstName: String, lastName: String)

  val provideEscalatedToForm: Form[ProvideEscalatedToForm] = Form(
    mapping(
      "first-name" -> nonEmptyText,
      "last-name"  -> nonEmptyText
    )(ProvideEscalatedToForm.apply)(ProvideEscalatedToForm.unapply)
  )
}

@Singleton
class GrantedJourneyController @Inject() (
    strideAuthorisationService: StrideAuthorisationService,
    mcc: MessagesControllerComponents,
    errorHandler: ErrorHandler,
    val applicationActionService: ApplicationActionService,
    val submissionService: SubmissionService,
    submissionReviewService: SubmissionReviewService,
    provideWarningsForGrantingPage: ProvideWarningsForGrantingPage,
    provideEscalatedToForGrantingPage: ProvideEscalatedToForGrantingPage,
    applicationApprovedPage: ApplicationApprovedPage
  )(implicit override val ec: ExecutionContext
  ) extends AbstractApplicationController(strideAuthorisationService, mcc, errorHandler) with WithUnsafeDefaultFormBinding {
  import GrantedJourneyController._

  def provideWarningsPage(applicationId: ApplicationId) = loggedInThruStrideWithApplicationAndSubmission(applicationId) { implicit request =>
    successful(Ok(provideWarningsForGrantingPage(provideWarningsForm, ViewModel(request.application.name, applicationId))))
  }

  def provideWarningsAction(applicationId: ApplicationId) = loggedInThruStrideWithApplicationAndSubmission(applicationId) { implicit request =>
    def handleValidForm(form: ProvideWarningsForm) = {
      (
        for {
          review <- EitherT.fromOptionF(
                      submissionReviewService.updateGrantWarnings(form.warnings)(request.submission.id, request.submission.latestInstance.index),
                      NonEmptyList.one(GenericFailure("There was a problem updating the grant warnings on the submission review"))
                    )
          _      <- EitherT(submissionService.grantWithWarnings(applicationId, request.name.get, form.warnings, review.escalatedTo))
        } yield Redirect(uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.routes.GrantedJourneyController.grantedPage(applicationId).url)
      )
        .value
        .flatMap {
          case Right(value)   => successful(value)
          case Left(failures) => {
            val errString = failures.toList.map(error => CommandFailures.describe(error)).mkString(", ")
            logger.warn(s"Error granting access for application $applicationId: $errString")
            errorHandler.internalServerErrorTemplate.map(InternalServerError(_))
          }
        }
    }

    def handleInvalidForm(form: Form[ProvideWarningsForm]) = {
      successful(BadRequest(provideWarningsForGrantingPage(form, ViewModel(request.application.name, applicationId))))
    }

    GrantedJourneyController.provideWarningsForm.bindFromRequest().fold(handleInvalidForm, handleValidForm)
  }

  def provideEscalatedToPage(applicationId: ApplicationId) = loggedInThruStrideWithApplicationAndSubmission(applicationId) { implicit request =>
    successful(Ok(provideEscalatedToForGrantingPage(provideEscalatedToForm, ViewModel(request.application.name, applicationId))))
  }

  def provideEscalatedToAction(applicationId: ApplicationId) = loggedInThruStrideWithApplicationAndSubmission(applicationId) { implicit request =>
    def handleValidForm(form: ProvideEscalatedToForm) = {
      submissionReviewService.updateEscalatedTo(form.firstName + " " + form.lastName)(request.submission.id, request.submission.latestInstance.index)
        .flatMap {
          case Some(value) => successful(Redirect(uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.routes.GrantedJourneyController.provideWarningsPage(applicationId).url))
          case None        => {
            logger.warn(s"Error updating submission review for application $applicationId: There was a problem updating the escalated to on the submission review")
            errorHandler.internalServerErrorTemplate.map(InternalServerError(_))
          }
        }
    }

    def handleInvalidForm(form: Form[ProvideEscalatedToForm]) = {
      successful(BadRequest(provideEscalatedToForGrantingPage(form, ViewModel(request.application.name, applicationId))))
    }

    GrantedJourneyController.provideEscalatedToForm.bindFromRequest().fold(handleInvalidForm, handleValidForm)
  }

  def grantedPage(applicationId: ApplicationId) = loggedInThruStrideWithApplicationAndSubmission(applicationId) { implicit request =>
    successful(Ok(applicationApprovedPage(GrantedJourneyController.ViewModel(request.application.name, applicationId))))
  }
}
