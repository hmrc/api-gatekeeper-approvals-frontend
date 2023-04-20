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

import java.time.format.DateTimeFormatter
import java.time.ZoneId
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.apiplatform.modules.common.services.ApplicationLogger
import uk.gov.hmrc.apiplatform.modules.gkauth.services.{LdapAuthorisationService, StrideAuthorisationService}
import uk.gov.hmrc.apiplatform.modules.submissions.domain.models.Submission.Status._
import uk.gov.hmrc.apiplatform.modules.submissions.domain.models.{Submission, TermsOfUseInvitation}
import uk.gov.hmrc.apiplatform.modules.submissions.services.SubmissionService

import uk.gov.hmrc.apigatekeeperapprovalsfrontend.config.ErrorHandler
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.actions.GatekeeperRoleWithApplicationActions
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.{Application, ApplicationId}
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.services.{ApplicationActionService, ApplicationService}
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.views.html.TermsOfUseHistoryPage

object TermsOfUseHistoryController {
  case class ViewModel(applicationId: ApplicationId, applicationName: String, currentState: TermsOfUseHistory, historyEntries: List[TermsOfUseHistory])
  case class TermsOfUseHistory(date: String, status: String, description: String, details: Option[String])
}

@Singleton
class TermsOfUseHistoryController @Inject() (
    strideAuthorisationService: StrideAuthorisationService,
    mcc: MessagesControllerComponents,
    errorHandler: ErrorHandler,
    val applicationActionService: ApplicationActionService,
    val submissionService: SubmissionService,
    val ldapAuthorisationService: LdapAuthorisationService,
    termsOfUseHistoryPage: TermsOfUseHistoryPage,
    applicationService: ApplicationService
  )(implicit override val ec: ExecutionContext
  ) extends AbstractApplicationController(strideAuthorisationService, mcc, errorHandler) with GatekeeperRoleWithApplicationActions with ApplicationLogger {
  import TermsOfUseHistoryController._

  def page(applicationId: ApplicationId) = loggedInThruStrideWithApplication(applicationId) { implicit request =>
    def deriveSubmissionStatusDisplayName(status: Submission.Status): String = {
      status match {
        case s: Answering                    => "In progress"
        case s: Created                      => "In progress"
        case s: Declined                     => "Declined"
        case s: Failed                       => "Failed"
        case s: Granted                      => "Terms of use V2"
        case s: GrantedWithWarnings          => "Terms of use V2 with warnings"
        case s: PendingResponsibleIndividual => "Pending responsible individual"
        case s: Submitted                    => "Submitted"
        case s: Warnings                     => "Warnings"
      }
    }

    def deriveSubmissionStatusDescription(status: Submission.Status): String = {
      status match {
        case s: Answering                    => "The submission was started"
        case s: Created                      => "The submission was created"
        case s: Declined                     => s"${s.name} declined that the application complies with the terms of use V2"
        case s: Failed                       => s"${s.name} submitted the terms of use checklist.  The application did not comply with version 2 of the terms of use."
        case s: Granted                      => s"${s.name} accepted that the application complies with the terms of use V2"
        case s: GrantedWithWarnings          => s"${s.name} accepted that the application complies with the terms of use V2 with warnings"
        case s: PendingResponsibleIndividual => "The submission is waiting for the responsible individual to accept the terms of use"
        case s: Submitted                    => "The submission was submitted"
        case s: Warnings                     => s"${s.name} submitted the terms of use checklist.  The application did not comply with version 2 of the terms of use."
      }
    }

    def deriveSubmissionStatusDetail(status: Submission.Status): Option[String] = {
      status match {
        case s: Answering                    => None
        case s: Created                      => None
        case s: Declined                     => Some(s.reasons)
        case s: Failed                       => None
        case s: Granted                      => None
        case s: GrantedWithWarnings          => Some(s.warnings)
        case s: PendingResponsibleIndividual => None
        case s: Submitted                    => None
        case s: Warnings                     => None
      }
    }

    def buildHistoryModelFromStatusHistory(invite: TermsOfUseInvitation, application: Application, history: Submission.Status): TermsOfUseHistory = {
      TermsOfUseHistory(
        history.timestamp.asText,
        deriveSubmissionStatusDisplayName(history),
        deriveSubmissionStatusDescription(history),
        deriveSubmissionStatusDetail(history)
      )
    }

    def filterSubmissionStatus(status: Submission.Status) = {
      status match {
        case s: Answering                    => true
        case s: Created                      => false
        case s: Declined                     => true
        case s: Failed                       => true
        case s: Granted                      => true
        case s: GrantedWithWarnings          => true
        case s: PendingResponsibleIndividual => true
        case s: Submitted                    => false
        case s: Warnings                     => true
      }
    }

    def buildHistoryFromSubmissionInstance(invite: TermsOfUseInvitation, application: Application, instance: Submission.Instance): List[TermsOfUseHistory] = {
      instance.statusHistory
        .filter(history => filterSubmissionStatus(history))
        .map(history => buildHistoryModelFromStatusHistory(invite, application, history))
        .toList
    }

    def buildHistoryFromSubmission(invite: TermsOfUseInvitation, application: Application, submission: Submission): List[TermsOfUseHistory] = {
      submission.instances.map(instance => buildHistoryFromSubmissionInstance(invite, application, instance)).toList.flatten.tail
    }

    def buildViewModel(invite: TermsOfUseInvitation, application: Application, submission: Option[Submission]): ViewModel = {
      submission match {
        case Some(sub) => {
          ViewModel(
            application.id,
            application.name,
            buildHistoryModelFromStatusHistory(invite, application, sub.status),
            buildHistoryFromSubmission(invite, application, sub)
          )
        }
        case None      => {
          ViewModel(
            application.id, 
            application.name, 
            TermsOfUseHistory(
              DateTimeFormatter.ofPattern("dd MMMM yyyy").withZone(ZoneId.systemDefault()).format(invite.createdOn), 
              "Email sent",
              "We invited admins of the application to agree to version 2 of the terms of use.",
              None
            ),
            List.empty
          )
        }
      }
    }

    (
      for {
        invite        <- fromOptionF(submissionService.fetchTermsOfUseInvitation(applicationId), BadRequest("Unable to find terms of use invitation"))
        submission    <- liftF(submissionService.fetchLatestSubmission(applicationId))
        viewModel      = buildViewModel(invite, request.application, submission)
      } yield Ok(termsOfUseHistoryPage(viewModel))
    ).fold(identity(_), identity(_))
  }
}
