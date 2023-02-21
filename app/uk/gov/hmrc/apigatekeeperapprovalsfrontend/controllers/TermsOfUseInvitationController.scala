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

import uk.gov.hmrc.apiplatform.modules.gkauth.services.StrideAuthorisationService
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.config.ErrorHandler
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.services.ApplicationActionService
import uk.gov.hmrc.apiplatform.modules.submissions.services.SubmissionService
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.actions.GatekeeperRoleWithApplicationActions
import uk.gov.hmrc.apiplatform.modules.gkauth.services.LdapAuthorisationService
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.views.html.TermsOfUsePage
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.ApplicationId
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.services.ApplicationService
import scala.concurrent.Future
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.Application
import uk.gov.hmrc.apiplatform.modules.submissions.domain.models.Submission
import uk.gov.hmrc.apiplatform.modules.common.services.ApplicationLogger
import java.time.Instant
import uk.gov.hmrc.apiplatform.modules.submissions.domain.models.TermsOfUseInvitation
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import uk.gov.hmrc.apiplatform.modules.submissions.domain.models.Submission.Status._

object TermsOfUseInvitationController {
  case class ViewModel(applicationId: ApplicationId, applicationName: String, lastUpdated: String, status: String)
}

@Singleton
class TermsOfUseInvitationController @Inject()(
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
    def getApplication(applicationId: ApplicationId): Future[Option[Application]] = {
      applicationService.fetchByApplicationId(applicationId)
    }

    def getSubmission(applicationId: ApplicationId): Future[Option[Submission]] = {
      submissionService.fetchLatestSubmission(applicationId)
    }

    def deriveSubmissionStatusDisplayName(status: Submission.Status) = {
      status match {
        case s: Answering => "Answering"
        case s: Created => "Created"
        case s: Declined => "Declined"
        case s: Failed => "Failed"
        case s: Granted => "Granted"
        case s: GrantedWithWarnings => "Granted with warnings"
        case s: PendingResponsibleIndividual => "Pending responsible individual"
        case s: Submitted => "Submitted"
        case s: Warnings => "Warnings"
      }
    }

    def buildViewModel(invite: TermsOfUseInvitation, application: Option[Application], submission: Option[Submission]): Option[ViewModel] = {
      (application, submission) match {
        case (Some(app), Some(sub)) => {
          logger.info(s"Found both application and submission for application with id ${invite.applicationId.value} when building terms of use invitation view model")
          Some(ViewModel(app.id, app.name, DateTimeFormatter.ofPattern("dd MMMM yyyy").withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(sub.status.timestamp.getMillis())), deriveSubmissionStatusDisplayName(sub.status)))
        }
        case (Some(app), None) => {
          logger.info(s"Found only application but no submission for application with id ${invite.applicationId.value} when building terms of use invitation view model")
          Some(ViewModel(app.id, app.name, DateTimeFormatter.ofPattern("dd MMMM yyyy").withZone(ZoneId.systemDefault()).format(invite.createdOn), "Email sent"))
        }
        case (None, Some(sub)) => {
          logger.info(s"Found only submission but no application for application with id ${invite.applicationId.value} when building terms of use invitation view model")
          None
        }
        case (None, None) => {
          logger.info(s"Found neither application nor submission for application with id ${invite.applicationId.value} when building terms of use invitation view model")
          None
        }
      }
    }

    for {
      invites <- submissionService.fetchTermsOfUseInvitations()
      applications <- Future.sequence(invites.map(invite => getApplication(invite.applicationId))).map(_.flatten)
      applicationMap = applications.map(app => (app.id -> app)).toMap
      submissions <- Future.sequence(invites.map(invite => getSubmission(invite.applicationId))).map(_.flatten)
      submissionMap = submissions.map(sub => (sub.applicationId -> sub)).toMap
      viewModels = invites.map(invite => buildViewModel(invite, applicationMap.get(invite.applicationId), submissionMap.get(invite.applicationId))).flatten
     } yield Ok(termsOfUsePage(viewModels))
  }
}
