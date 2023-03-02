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
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.{Application, ApplicationId}
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.services.ApplicationActionService
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.views.html.{TermsOfUseGrantedConfirmationPage, TermsOfUseGrantedPage}

object TermsOfUseGrantedController {
  case class ViewModel(appName: String, applicationId: ApplicationId, applicationDetailsUrl: String)
}

@Singleton
class TermsOfUseGrantedController @Inject() (
    config: GatekeeperConfig,
    strideAuthorisationService: StrideAuthorisationService,
    mcc: MessagesControllerComponents,
    errorHandler: ErrorHandler,
    termsOfUseGrantedConfirmationPage: TermsOfUseGrantedConfirmationPage,
    termsOfUseGrantedPage: TermsOfUseGrantedPage,
    val applicationActionService: ApplicationActionService,
    val submissionService: SubmissionService
  )(implicit override val ec: ExecutionContext
  ) extends AbstractApplicationController(strideAuthorisationService, mcc, errorHandler) {

  def page(applicationId: ApplicationId): Action[AnyContent] = loggedInThruStrideWithApplicationAndSubmission(applicationId) { implicit request =>
    val gatekeeperApplicationUrl = s"${config.applicationsPageUri}/${applicationId.value}"

      successful(Ok(
        termsOfUseGrantedPage(
          TermsOfUseGrantedController.ViewModel(request.application.name, applicationId, gatekeeperApplicationUrl)
        )
      ))
  }

  def action(applicationId: ApplicationId): Action[AnyContent] = loggedInThruStrideWithApplicationAndSubmission(applicationId) { implicit request =>
    val gatekeeperApplicationUrl = s"${config.applicationsPageUri}/${applicationId.value}"

    def grantTermsOfUse = {
      def failure(err: String) = BadRequest(
        errorHandler.standardErrorTemplate(
          "Terms of use grant",
          "Error granting terms of use",
          err
        )
      )
      val success              = Ok(
        termsOfUseGrantedConfirmationPage(
          TermsOfUseGrantedController.ViewModel(request.application.name, applicationId, gatekeeperApplicationUrl)
        )
      )

      submissionService.grantForTouUplift(applicationId, request.name.get).map((esu: Either[String, Application]) => esu.fold(err => failure(err), _ => success))
    }

    request.body.asFormUrlEncoded.getOrElse(Map.empty).get("grant").flatMap(_.headOption) match {
      case Some("yes") => grantTermsOfUse
      case _           => successful(Redirect(uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.routes.TermsOfUseInvitationController.page))
    }
  }
}
