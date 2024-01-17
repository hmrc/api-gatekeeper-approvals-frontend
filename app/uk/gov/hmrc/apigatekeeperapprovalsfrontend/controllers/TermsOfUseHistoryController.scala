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
import java.time.{Instant, ZoneId}
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.apiplatform.modules.common.domain.models.ApplicationId
import uk.gov.hmrc.apiplatform.modules.common.services.ApplicationLogger
import uk.gov.hmrc.apiplatform.modules.gkauth.services.{LdapAuthorisationService, StrideAuthorisationService}
import uk.gov.hmrc.apiplatform.modules.submissions.domain.models.Submission.Status._
import uk.gov.hmrc.apiplatform.modules.submissions.domain.models.{AskWhen, Submission, TermsOfUseInvitation, TermsOfUseInvitationState}
import uk.gov.hmrc.apiplatform.modules.submissions.services.SubmissionService

import uk.gov.hmrc.apigatekeeperapprovalsfrontend.config.{ErrorHandler, GatekeeperConfig}
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.actions.GatekeeperRoleWithApplicationActions
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.Application
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.services.{ApplicationActionService, ApplicationService}
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.views.html.TermsOfUseHistoryPage

object TermsOfUseHistoryController {

  case class ViewModel(
      applicationId: ApplicationId,
      applicationName: String,
      historyEntries: List[TermsOfUseHistory],
      applicationDetailsUrl: String,
      isInHouseSoftware: Boolean,
      dueDate: String
    )

  case class TermsOfUseHistory(
      date: String,
      status: String,
      description: String,
      details: Option[String],
      escalatedTo: Option[String],
      submissionStatus: Option[Submission.Status],
      dateAsString: String
    )
}

@Singleton
class TermsOfUseHistoryController @Inject() (
    config: GatekeeperConfig,
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

  def page(applicationId: ApplicationId) = loggedInWithApplication(applicationId) { implicit request =>
    val gatekeeperApplicationUrl = s"${config.applicationsPageUri}/${applicationId.value}"

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

    def deriveInvitationStatusDisplayName(status: TermsOfUseInvitationState): String = {
      status match {
        case TermsOfUseInvitationState.REMINDER_EMAIL_SENT => "Reminder email sent"
        case TermsOfUseInvitationState.OVERDUE             => "Overdue"
        case _                                             => "Email sent"
      }
    }

    def deriveSubmissionStatusDescription(status: Submission.Status): String = {
      status match {
        case s: Answering                    => "The submission was started"
        case s: Created                      => "The submission was started"
        case s: Declined                     => s"${s.name} declined that the application complies with the terms of use V2"
        case s: Failed                       => s"${s.name} submitted the terms of use checklist.  The application did not comply with version 2 of the terms of use."
        case s: Granted                      => s"${s.name} accepted that the application complies with the terms of use V2"
        case s: GrantedWithWarnings          => s"${s.name} has contacted the application admins to action the warnings generated by the submission."
        case s: PendingResponsibleIndividual => "The submission is waiting for the responsible individual to accept the terms of use"
        case s: Submitted                    => "The submission was submitted"
        case s: Warnings                     => s"${s.name} submitted the terms of use checklist.  The application did not comply with version 2 of the terms of use."
      }
    }

    def deriveInvitationStatusDescription(status: TermsOfUseInvitationState): String = {
      status match {
        case TermsOfUseInvitationState.REMINDER_EMAIL_SENT => "We sent a reminder email to the admins of the application."
        case TermsOfUseInvitationState.OVERDUE             => "The terms of use have not been completed by the due date."
        case _                                             => "We invited admins of the application to agree to version 2 of the terms of use."
      }
    }

    def deriveSubmissionStatusDetail(status: Submission.Status): Option[String] = {
      status match {
        case s: Answering                    => None
        case s: Created                      => None
        case s: Declined                     => Some(s.reasons)
        case s: Failed                       => None
        case s: Granted                      => s.comments
        case s: GrantedWithWarnings          => Some(s.warnings)
        case s: PendingResponsibleIndividual => None
        case s: Submitted                    => None
        case s: Warnings                     => None
      }
    }

    def deriveSubmissionEscalatedTo(status: Submission.Status): Option[String] = {
      status match {
        case s: Answering                    => None
        case s: Created                      => None
        case s: Declined                     => None
        case s: Failed                       => None
        case s: Granted                      => s.escalatedTo
        case s: GrantedWithWarnings          => s.escalatedTo
        case s: PendingResponsibleIndividual => None
        case s: Submitted                    => None
        case s: Warnings                     => None
      }
    }

    val fmter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneId.of("UTC"))

    def buildModelFromSubmissionStatus(status: Submission.Status): TermsOfUseHistory = {
      TermsOfUseHistory(
        status.timestamp.asText,
        deriveSubmissionStatusDisplayName(status),
        deriveSubmissionStatusDescription(status),
        deriveSubmissionStatusDetail(status),
        deriveSubmissionEscalatedTo(status),
        Some(status),
        fmter.format(status.timestamp)
      )
    }

    def buildModelFromInvitationStateAndDate(status: TermsOfUseInvitationState, date: Option[Instant]): TermsOfUseHistory = {
      TermsOfUseHistory(
        date.fold("Unknown")(d => DateTimeFormatter.ofPattern("dd MMMM yyyy").withZone(ZoneId.systemDefault()).format(d)),
        deriveInvitationStatusDisplayName(status),
        deriveInvitationStatusDescription(status),
        None,
        None,
        None,
        date.fold("0")(d => DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneId.of("UTC")).format(d))
      )
    }

    def buildHistoryFromInvitation(invite: TermsOfUseInvitation): List[TermsOfUseHistory] = {
      invite.status match {
        case TermsOfUseInvitationState.OVERDUE             => List[TermsOfUseHistory](
            buildModelFromInvitationStateAndDate(TermsOfUseInvitationState.OVERDUE, Some(invite.dueBy)),
            buildModelFromInvitationStateAndDate(TermsOfUseInvitationState.REMINDER_EMAIL_SENT, invite.reminderSent),
            buildModelFromInvitationStateAndDate(TermsOfUseInvitationState.EMAIL_SENT, Some(invite.createdOn))
          )
        case TermsOfUseInvitationState.REMINDER_EMAIL_SENT => List[TermsOfUseHistory](
            buildModelFromInvitationStateAndDate(TermsOfUseInvitationState.REMINDER_EMAIL_SENT, invite.reminderSent),
            buildModelFromInvitationStateAndDate(TermsOfUseInvitationState.EMAIL_SENT, Some(invite.createdOn))
          )
        case _                                             => List[TermsOfUseHistory](
            buildModelFromInvitationStateAndDate(TermsOfUseInvitationState.EMAIL_SENT, Some(invite.createdOn))
          )
      }
    }

    def filterHistoryStatus(status: Option[Submission.Status]) = {
      status match {
        case Some(Answering(_, _))                    => true
        case Some(Submission.Status.Created(_, _))    => false
        case Some(Declined(_, _, _))                  => true
        case Some(Failed(_, _))                       => true
        case Some(Granted(_, _, _, _))                => true
        case Some(GrantedWithWarnings(_, _, _, _))    => true
        case Some(PendingResponsibleIndividual(_, _)) => true
        case Some(Submitted(_, _))                    => false
        case Some(Warnings(_, _))                     => true
        case None                                     => true
      }
    }

    def buildHistoryFromSubmissionInstance(instance: Submission.Instance): List[TermsOfUseHistory] = {
      instance.statusHistory
        .map(history => buildModelFromSubmissionStatus(history))
        .toList
    }

    def buildHistoryFromSubmission(submission: Submission): List[TermsOfUseHistory] = {
      submission.instances
        .map(instance => buildHistoryFromSubmissionInstance(instance))
        .toList
        .flatten
        .filter(history => filterHistoryStatus(history.submissionStatus))
    }

    def buildHistoryFromSubmissionAndInvitation(submission: Submission, invite: TermsOfUseInvitation): List[TermsOfUseHistory] = {
      val history = buildHistoryFromSubmission(submission) ++ buildHistoryFromInvitation(invite)
      history
    }

    def isInHouseSoftware(submission: Submission): Boolean = {
      submission.context.get(AskWhen.Context.Keys.IN_HOUSE_SOFTWARE).contains("Yes")
    }

    def buildViewModel(invite: TermsOfUseInvitation, application: Application, submission: Option[Submission]): ViewModel = {
      submission match {
        case Some(sub) => {
          ViewModel(
            application.id,
            application.name,
            buildHistoryFromSubmissionAndInvitation(sub, invite).sortBy(_.dateAsString).reverse,
            gatekeeperApplicationUrl,
            isInHouseSoftware(sub),
            DateTimeFormatter.ofPattern("dd MMMM yyyy").withZone(ZoneId.systemDefault()).format(invite.dueBy)
          )
        }
        case None      => {
          ViewModel(
            application.id,
            application.name,
            buildHistoryFromInvitation(invite).sortBy(_.dateAsString).reverse,
            gatekeeperApplicationUrl,
            false,
            DateTimeFormatter.ofPattern("dd MMMM yyyy").withZone(ZoneId.systemDefault()).format(invite.dueBy)
          )
        }
      }
    }

    (
      for {
        invite     <- fromOptionF(submissionService.fetchTermsOfUseInvitation(applicationId), BadRequest("Unable to find terms of use invitation"))
        submission <- liftF(submissionService.fetchLatestSubmission(applicationId))
        viewModel   = buildViewModel(invite, request.application, submission)
      } yield Ok(termsOfUseHistoryPage(viewModel))
    ).fold(identity(_), identity(_))
  }
}
