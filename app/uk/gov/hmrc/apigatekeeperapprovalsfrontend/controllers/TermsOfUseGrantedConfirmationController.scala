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
import uk.gov.hmrc.play.bootstrap.controller.WithUrlEncodedOnlyFormBinding

import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.ApplicationName
import uk.gov.hmrc.apiplatform.modules.common.domain.models.ApplicationId
import uk.gov.hmrc.apiplatform.modules.gkauth.services.StrideAuthorisationService
import uk.gov.hmrc.apiplatform.modules.submissions.services.SubmissionService

import uk.gov.hmrc.apigatekeeperapprovalsfrontend.config.ErrorHandler
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.services.ApplicationActionService
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.views.html.TermsOfUseGrantedConfirmationPage

object TermsOfUseGrantedConfirmationController {
  case class ViewModel(applicationId: ApplicationId, appName: ApplicationName)
}

@Singleton
class TermsOfUseGrantedConfirmationController @Inject() (
    strideAuthorisationService: StrideAuthorisationService,
    mcc: MessagesControllerComponents,
    errorHandler: ErrorHandler,
    val applicationActionService: ApplicationActionService,
    val submissionService: SubmissionService,
    termsOfUseGrantedConfirmationPage: TermsOfUseGrantedConfirmationPage
  )(implicit override val ec: ExecutionContext
  ) extends AbstractApplicationController(strideAuthorisationService, mcc, errorHandler) with WithUrlEncodedOnlyFormBinding {

  import TermsOfUseGrantedConfirmationController._

  def page(applicationId: ApplicationId) = strideAdvancedUserWithApplicationAndSubmission(applicationId) { implicit request =>
    successful(Ok(termsOfUseGrantedConfirmationPage(ViewModel(applicationId, request.application.name))))
  }
}
