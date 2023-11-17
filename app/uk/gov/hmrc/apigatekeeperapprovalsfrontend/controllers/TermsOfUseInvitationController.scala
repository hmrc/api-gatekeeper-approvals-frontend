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

import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.apiplatform.modules.common.domain.models.ApplicationId
import uk.gov.hmrc.apiplatform.modules.common.services.ApplicationLogger
import uk.gov.hmrc.apiplatform.modules.gkauth.services.{LdapAuthorisationService, StrideAuthorisationService}
import uk.gov.hmrc.apiplatform.modules.submissions.domain.models.TermsOfUseInvitationWithApplication
import uk.gov.hmrc.apiplatform.modules.submissions.services.SubmissionService

import uk.gov.hmrc.apigatekeeperapprovalsfrontend.config.ErrorHandler
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.actions.GatekeeperRoleWithApplicationActions
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.services.{ApplicationActionService, ApplicationService}
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.views.html.TermsOfUsePage

object TermsOfUseInvitationController {
  case class ViewModel(applicationId: ApplicationId, applicationName: String, lastUpdated: String, status: String)
}

@Singleton
class TermsOfUseInvitationController @Inject() (
    strideAuthorisationService: StrideAuthorisationService,
    mcc: MessagesControllerComponents,
    errorHandler: ErrorHandler,
    val applicationActionService: ApplicationActionService,
    val submissionService: SubmissionService,
    val ldapAuthorisationService: LdapAuthorisationService,
    termsOfUsePage: TermsOfUsePage,
    applicationService: ApplicationService
  )(implicit override val ec: ExecutionContext
  ) extends AbstractApplicationController(strideAuthorisationService, mcc, errorHandler) with GatekeeperRoleWithApplicationActions with ApplicationLogger {
  import TermsOfUseInvitationController.ViewModel

  def page = loggedInOnly() { implicit request =>
    def buildViewModel(invite: TermsOfUseInvitationWithApplication): ViewModel = {
      ViewModel(
        invite.applicationId,
        invite.applicationName,
        DateTimeFormatter.ofPattern("dd MMMM yyyy").withZone(ZoneId.systemDefault()).format(invite.lastUpdated),
        invite.status.toString()
      )
    }

    // TODO
    val params: Map[String, String] = Map()

    for {
      invites    <- submissionService.searchTermsOfUseInvitations(params)
      viewModels  = invites.map(invite => buildViewModel(invite))
    } yield Ok(termsOfUsePage(viewModels))
  }
}
