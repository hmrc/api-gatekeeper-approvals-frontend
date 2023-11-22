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

import play.api.data.Form
import play.api.data.Forms._

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

  case class FilterForm(
    emailSentStatus: Option[String], 
    overdueStatus: Option[String], 
    reminderEmailSentStatus: Option[String],
    warningsStatus: Option[String],
    failedStatus: Option[String],
    termsOfUseV2WithWarningsStatus: Option[String],
    termsOfUseV2Status: Option[String]
  )

  val filterForm: Form[FilterForm] = Form(
    mapping(
      "emailSentStatus" -> optional(text),
      "overdueStatus" -> optional(text),
      "reminderEmailSentStatus" -> optional(text),
      "warningsStatus" -> optional(text),
      "failedStatus" -> optional(text),
      "termsOfUseV2WithWarningsStatus" -> optional(text),
      "termsOfUseV2Status" -> optional(text)
    )(FilterForm.apply)(FilterForm.unapply)
  )
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
  import TermsOfUseInvitationController._

  def page = loggedInOnly() { implicit request =>
    def buildViewModel(invite: TermsOfUseInvitationWithApplication): ViewModel = {
      ViewModel(
        invite.applicationId,
        invite.applicationName,
        DateTimeFormatter.ofPattern("dd MMMM yyyy").withZone(ZoneId.systemDefault()).format(invite.lastUpdated),
        invite.status.toString()
      )
    }

    def handleValidForm(form: FilterForm) = {
      val params: Seq[(String, String)] = getQueryParamsFromForm(form)
      val queryForm = filterForm.fill(form)

      for {
        invites    <- submissionService.searchTermsOfUseInvitations(params)
        viewModels  = invites.map(invite => buildViewModel(invite))
      } yield Ok(termsOfUsePage(queryForm, viewModels))
    }

    def handleInvalidForm(form: Form[FilterForm]) = {

      for {
        invites    <- submissionService.searchTermsOfUseInvitations(Seq.empty)
        viewModels  = invites.map(invite => buildViewModel(invite))
      } yield Ok(termsOfUsePage(form, viewModels))
    }

    TermsOfUseInvitationController.filterForm.bindFromRequest().fold(handleInvalidForm, handleValidForm)
  }

  private def getQueryParamsFromForm(form: FilterForm): Seq[(String, String)] = {
    getQueryParamFromVar("EMAIL_SENT", form.emailSentStatus) ++ 
      getQueryParamFromVar("WARNINGS", form.warningsStatus) ++
      getQueryParamFromVar("TERMS_OF_USE_V2_WITH_WARNINGS", form.termsOfUseV2WithWarningsStatus) ++
      getQueryParamFromVar("OVERDUE", form.overdueStatus) ++
      getQueryParamFromVar("FAILED", form.failedStatus) ++
      getQueryParamFromVar("TERMS_OF_USE_V2", form.termsOfUseV2Status) ++
      getQueryParamFromVar("REMINDER_EMAIL_SENT", form.reminderEmailSentStatus)

  }

  private def getQueryParamFromVar(key: String, value: Option[String]): Seq[(String, String)] = {
    if (value == Some("true")) {
      Seq("status" -> key)
    } else {
      Seq.empty
    }
  }
}
