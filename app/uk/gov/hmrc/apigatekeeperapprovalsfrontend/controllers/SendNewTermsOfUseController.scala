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

import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}

import uk.gov.hmrc.apiplatform.modules.applications.access.domain.models.Access
import uk.gov.hmrc.apiplatform.modules.applications.core.domain
import uk.gov.hmrc.apiplatform.modules.commands.applications.domain.models.{CommandFailure, CommandFailures}
import uk.gov.hmrc.apiplatform.modules.common.domain.models.ApplicationId
import uk.gov.hmrc.apiplatform.modules.common.services.EitherTHelper
import uk.gov.hmrc.apiplatform.modules.gkauth.services.StrideAuthorisationService
import uk.gov.hmrc.apiplatform.modules.submissions.services.SubmissionService

import uk.gov.hmrc.apigatekeeperapprovalsfrontend.config.{ErrorHandler, GatekeeperConfig}
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.services.ApplicationActionService
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.views.html.{SendNewTermsOfUseConfirmPage, SendNewTermsOfUseRequestedPage}

object SendNewTermsOfUseController {
  case class ViewModel(appName: String, applicationId: ApplicationId, applicationDetailsUrl: String)
}

@Singleton
class SendNewTermsOfUseController @Inject() (
    config: GatekeeperConfig,
    strideAuthorisationService: StrideAuthorisationService,
    mcc: MessagesControllerComponents,
    errorHandler: ErrorHandler,
    sendNewTermsOfUseConfirmPage: SendNewTermsOfUseConfirmPage,
    sendNewTermsOfUseRequestedPage: SendNewTermsOfUseRequestedPage,
    val applicationActionService: ApplicationActionService,
    val submissionService: SubmissionService
  )(implicit override val ec: ExecutionContext
  ) extends AbstractApplicationController(strideAuthorisationService, mcc, errorHandler) {

  def page(applicationId: ApplicationId): Action[AnyContent] = loggedInThruStrideWithApplication(applicationId) { implicit request =>
    val gatekeeperApplicationUrl = s"${config.applicationsPageUri}/${applicationId.value}"

    def checkNotAlreadyInvited = {
      // Check no existing submissions and not already invited
      val success = successful(Ok(
        sendNewTermsOfUseConfirmPage(
          SendNewTermsOfUseController.ViewModel(request.application.name, applicationId, gatekeeperApplicationUrl)
        )
      ))

      lazy val failed =
        errorHandler.standardErrorTemplate(
          "Application submission",
          "Application already invited",
          "The application has already been invited or has submissions"
        ).map(BadRequest(_))
      (
        for {
          existingSubmission <- submissionService.fetchLatestSubmission(applicationId)
          existingInvitation <- submissionService.fetchTermsOfUseInvitation(applicationId)
          result             <- if (existingSubmission.isEmpty && existingInvitation.isEmpty) success else failed
        } yield result
      )
    }

    request.application.access match {
      // Should only be sending new terms of use invites to Standard apps
      // with a state of Production and not already invited
      case std: Access.Standard if (request.application.state.name == domain.models.State.PRODUCTION) => checkNotAlreadyInvited
      case std: Access.Standard                                                                       =>
        errorHandler.standardErrorTemplate(
          "Application status",
          "Invalid application status",
          "The application must have Production status"
        ).map(BadRequest(_))
      case _                                                                                          =>
        errorHandler.standardErrorTemplate(
          "Application access type",
          "Invalid application access type",
          "The application must be of Standard access type"
        ).map(BadRequest(_))
    }
  }

  def action(applicationId: ApplicationId): Action[AnyContent] = loggedInThruStrideWithApplication(applicationId) { implicit request =>
    val gatekeeperApplicationUrl = s"${config.applicationsPageUri}/${applicationId.value}"

    def inviteTermsOfUse = {
      def failure(failures: NonEmptyList[CommandFailure]) = {
        val errString = failures.toList.map(error => CommandFailures.describe(error)).mkString(", ")
        errorHandler.standardErrorTemplate(
          "Terms of use invite",
          "Error inviting for terms of use",
          errString
        ).map(BadRequest(_))
      }

      lazy val success = Ok(
        sendNewTermsOfUseRequestedPage(
          SendNewTermsOfUseController.ViewModel(request.application.name, applicationId, gatekeeperApplicationUrl)
        )
      )

      val E = EitherTHelper.make[NonEmptyList[CommandFailure]]

      E.fromEitherF(submissionService.termsOfUseInvite(applicationId, request.name.get))
        .map(_ => success)
        .leftSemiflatMap(err => failure(err))
        .merge
    }

    request.body.asFormUrlEncoded.getOrElse(Map.empty).get("invite-admins").flatMap(_.headOption) match {
      case Some("yes") => inviteTermsOfUse
      case _           => successful(Redirect(gatekeeperApplicationUrl))
    }
  }
}
