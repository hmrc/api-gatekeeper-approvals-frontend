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
import uk.gov.hmrc.apiplatform.modules.common.services.EitherTHelper
import uk.gov.hmrc.apiplatform.modules.gkauth.services.StrideAuthorisationService
import uk.gov.hmrc.apiplatform.modules.submissions.services.SubmissionService

import uk.gov.hmrc.apigatekeeperapprovalsfrontend.config.ErrorHandler
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.services.{ApplicationActionService, SubmissionReviewService}
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.views.html._

object TermsOfUseDeleteController {

  case class ViewModel(applicationId: ApplicationId, appName: ApplicationName)

}

@Singleton
class TermsOfUseDeleteController @Inject() (
    strideAuthorisationService: StrideAuthorisationService,
    mcc: MessagesControllerComponents,
    errorHandler: ErrorHandler,
    submissionReviewService: SubmissionReviewService,
    termsOfUseDeletePage: TermsOfUseDeletePage,
    termsOfUseConfirmPage: TermsOfUseDeleteConfirmationPage,
    val applicationActionService: ApplicationActionService,
    val submissionService: SubmissionService
  )(implicit override val ec: ExecutionContext
  ) extends AbstractCheckController(strideAuthorisationService, mcc, errorHandler, submissionReviewService) {

  import TermsOfUseDeleteController._

  def page(applicationId: ApplicationId) = loggedInThruStrideWithApplication(applicationId) { implicit request =>
    successful(Ok(termsOfUseDeletePage(ViewModel(applicationId, request.application.name))))
  }

  def action(applicationId: ApplicationId) = loggedInThruStrideWithApplication(applicationId) { implicit request =>
    def deleteSubmission() = {
      def failure(err: String) =
        errorHandler.standardErrorTemplate(
          "Terms of use delete",
          "Error deleting terms of use",
          err
        ).map(BadRequest(_))

      lazy val success = Redirect(uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.routes.TermsOfUseDeleteController.confirmationPage(applicationId))

      val E = EitherTHelper.make[String]

      E.fromEitherF(submissionService.deleteTouUplift(applicationId, request.name.get))
        .map(_ => success)
        .leftSemiflatMap(err => failure(err))
        .merge
    }

    request.body.asFormUrlEncoded.getOrElse(Map.empty).get("tou-delete").flatMap(_.headOption) match {
      case Some("yes") => deleteSubmission()
      case _           => successful(Redirect(uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.routes.TermsOfUseInvitationController.page))
    }
  }

  def confirmationPage(applicationId: ApplicationId) = loggedInThruStrideWithApplication(applicationId) { implicit request =>
    successful(Ok(termsOfUseConfirmPage(ViewModel(applicationId, request.application.name))))
  }
}
