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
import uk.gov.hmrc.apiplatform.modules.gkauth.services.StrideAuthorisationService
import uk.gov.hmrc.apiplatform.modules.submissions.services.SubmissionService

import uk.gov.hmrc.apigatekeeperapprovalsfrontend.config.{ErrorHandler, GatekeeperConfig}
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.{ApplicationId, Standard}
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
    request.application.access match {
      // Should only be uplifting and checking Standard apps
      case std: Standard =>
        val gatekeeperApplicationUrl = s"${config.applicationsPageUri}/${applicationId.value}"
        successful(
          Ok(
            sendNewTermsOfUseConfirmPage(
              SendNewTermsOfUseController.ViewModel(
                request.application.name,
                applicationId,
                gatekeeperApplicationUrl
              )
            )
          )
        )
      case _             => successful(BadRequest(errorHandler.badRequestTemplate))
    }
  }

  def action(applicationId: ApplicationId): Action[AnyContent] = loggedInThruStrideWithApplication(applicationId) { implicit request =>
    val gatekeeperApplicationUrl = s"${config.applicationsPageUri}/${applicationId.value}"
    request.body.asFormUrlEncoded.getOrElse(Map.empty).get("invite-admins").flatMap(_.headOption) match {
      case Some("yes") => successful(Ok(sendNewTermsOfUseRequestedPage(
              SendNewTermsOfUseController.ViewModel(
                request.application.name,
                applicationId,
                gatekeeperApplicationUrl
              ))))
      case _           => successful(Redirect(gatekeeperApplicationUrl))
    }
  }
}
