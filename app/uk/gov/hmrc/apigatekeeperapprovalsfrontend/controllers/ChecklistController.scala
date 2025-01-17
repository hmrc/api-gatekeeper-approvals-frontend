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

import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models._
import uk.gov.hmrc.apiplatform.modules.common.domain.models.ApplicationId
import uk.gov.hmrc.apiplatform.modules.gkauth.services.StrideAuthorisationService
import uk.gov.hmrc.apiplatform.modules.submissions.domain.models.Submission
import uk.gov.hmrc.apiplatform.modules.submissions.services._

import uk.gov.hmrc.apigatekeeperapprovalsfrontend.config.ErrorHandler
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models._
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.services.{SubmissionRequiresDemo, SubmissionRequiresFraudCheck}
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.services.{ApplicationActionService, SubmissionReviewService}
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.views.html.ChecklistPage

object ChecklistController {

  case class ChecklistSection(titleMsgId: String, items: List[ChecklistItem]) {
    lazy val isEmpty = items.isEmpty
  }
  case class ChecklistItem(labelMsgId: String, url: String, uid: String, status: SubmissionReview.Status)
  case class ViewModel(applicationId: ApplicationId, appName: ApplicationName, topMsgId: String, sections: List[ChecklistSection], isInHouseSoftware: Boolean, isDeleted: Boolean)
}

@Singleton
class ChecklistController @Inject() (
    strideAuthorisationService: StrideAuthorisationService,
    mcc: MessagesControllerComponents,
    submissionReviewService: SubmissionReviewService,
    errorHandler: ErrorHandler,
    checklistPage: ChecklistPage,
    val applicationActionService: ApplicationActionService,
    val submissionService: SubmissionService
  )(implicit override val ec: ExecutionContext
  ) extends AbstractApplicationController(strideAuthorisationService, mcc, errorHandler) {
  import ChecklistController._
  import Implicits._

  type RequiredActions = Map[SubmissionReview.Action, SubmissionReview.Status]

  object AutomaticChecksResult extends Enumeration {
    type AutomaticChecksResult = Value
    val PASSED_WITHOUT_WARNINGS, PASSED_WITH_WARNINGS, FAILED = Value

    def apply(isSuccessful: Boolean, hasWarnings: Boolean): AutomaticChecksResult = {
      (isSuccessful, hasWarnings) match {
        case (true, false) => PASSED_WITHOUT_WARNINGS
        case (true, true)  => PASSED_WITH_WARNINGS
        case (false, _)    => FAILED
      }
    }
  }
  import AutomaticChecksResult._

  private def setupSubmissionReview(submission: Submission, isSuccessful: Boolean, hasWarnings: Boolean) = {
    val requiresFraudCheck = SubmissionRequiresFraudCheck(submission)
    val requiresDemo       = SubmissionRequiresDemo(submission)
    submissionReviewService.findOrCreateReview(submission.id, submission.latestInstance.index, isSuccessful, hasWarnings, requiresFraudCheck, requiresDemo)
  }

  def checklistPage(applicationId: ApplicationId): Action[AnyContent] = loggedInThruStrideWithApplicationAndSubmission(applicationId) { implicit request =>
    val appName                    = request.application.name
    val isSuccessful               = !request.markedSubmission.isFail
    val hasWarnings                = request.markedSubmission.isWarn
    val automaticChecksResult      = AutomaticChecksResult(isSuccessful, hasWarnings)
    val topMsgId                   = automaticChecksResult match {
      case PASSED_WITHOUT_WARNINGS => "checklist.requestpassed"
      case PASSED_WITH_WARNINGS    => "checklist.requestpassedwithwarnings"
      case FAILED                  => "checklist.requestfailed"
    }
    val isInHouseSoftware: Boolean = request.application.isInHouseSoftware
    val isDeleted                  = request.application.state.isDeleted

    for {
      review  <- setupSubmissionReview(request.submission, isSuccessful, hasWarnings)
      sections = buildChecklistSections(applicationId, review.requiredActions, automaticChecksResult)
    } yield Ok(checklistPage(ViewModel(applicationId, appName, topMsgId, sections, isInHouseSoftware, isDeleted)))
  }

  def declineRequest(applicationId: ApplicationId): Action[AnyContent] = loggedInThruStrideWithApplicationAndSubmission(applicationId) { implicit request =>
    val isSuccessful = !request.markedSubmission.isFail
    val hasWarnings  = request.markedSubmission.isWarn
    for {
      review <- setupSubmissionReview(request.submission, isSuccessful, hasWarnings)
    } yield Redirect(uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.routes.DeclinedJourneyController.provideReasonsPage(applicationId))
  }

  def checklistAction(applicationId: ApplicationId): Action[AnyContent] = loggedInThruStrideWithApplicationAndSubmission(applicationId) { implicit request =>
    request.body.asFormUrlEncoded.getOrElse(Map.empty).get("submit-action").flatMap(_.headOption) match {
      case Some("checked")         => successful(Redirect(uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.routes.ConfirmYourDecisionController.page(applicationId)))
      case Some("come-back-later") => successful(Redirect(uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.routes.ApplicationSubmissionsController.page(applicationId)))
      case _                       => successful(BadRequest("Invalid submit-action found in request"))
    }
  }

  private def buildChecklistSections(applicationId: ApplicationId, requiredActions: RequiredActions, automaticChecksResult: AutomaticChecksResult): List[ChecklistSection] = {
    List(
      buildFailsAndWarnsSection(applicationId, requiredActions, automaticChecksResult),
      buildCheckApplicationSection(applicationId, requiredActions),
      buildAnswersThatPassedSection(applicationId, requiredActions)
    ).filter(!_.isEmpty)
  }

  private def buildFailsAndWarnsSection(applicationId: ApplicationId, requiredActions: RequiredActions, automaticChecksResult: AutomaticChecksResult): ChecklistSection = {
    val (titleMsgId, checkListItems) = automaticChecksResult match {
      case PASSED_WITH_WARNINGS => ("checklist.checkwarnings.heading", buildCheckWarningsItem(applicationId, requiredActions))
      case FAILED               => ("checklist.checkfailed.heading", buildCheckFailuresAndWarningsItem(applicationId, requiredActions))
      case _                    => ("", None)
    }

    ChecklistSection(titleMsgId, checkListItems.toList)
  }

  private def buildCheckApplicationSection(applicationId: ApplicationId, requiredActions: RequiredActions): ChecklistSection = {
    val checkApplicationNameItem     = buildCheckApplicationNameItem(applicationId, requiredActions)
    val checkCompanyRegistrationItem = buildCheckCompanyRegistrationItem(applicationId, requiredActions)
    val checkUrlsItem                = buildCheckUrlsItem(applicationId, requiredActions)
    val checkSandboxItem             = buildCheckSandboxTestingItem(applicationId, requiredActions)
    val checkFraudItem               = buildCheckFraudItem(applicationId, requiredActions)
    val arrangeDemoItem              = buildArrangeDemoItem(applicationId, requiredActions)

    val checklistItems = checkApplicationNameItem ++ checkCompanyRegistrationItem ++ checkUrlsItem ++ checkSandboxItem ++ checkFraudItem ++ arrangeDemoItem
    ChecklistSection("checklist.checkapplication.heading", checklistItems.toList)
  }

  private def buildAnswersThatPassedSection(applicationId: ApplicationId, requiredActions: RequiredActions): ChecklistSection = {
    ChecklistSection("checklist.checkpassed.heading", buildAnswersThatPassedItem(applicationId, requiredActions).toList)
  }

  private def buildChecklistItemIfActionIsRequired(
      labelMsgId: String,
      urlFn: ApplicationId => String,
      uid: String,
      action: SubmissionReview.Action
    )(
      applicationId: ApplicationId,
      requiredActions: RequiredActions
    ): Option[ChecklistItem] = {
    requiredActions.get(action).map(ChecklistItem(labelMsgId, urlFn(applicationId), uid, _))
  }

  private def buildCheckWarningsItem = buildChecklistItemIfActionIsRequired(
    "checklist.checkwarnings.linktext",
    uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.routes.CheckAnswersThatFailedController.page(_).url,
    "checkwarnings",
    SubmissionReview.Action.CheckFailsAndWarnings
  ) _

  private def buildCheckFailuresAndWarningsItem = buildChecklistItemIfActionIsRequired(
    "checklist.checkfailed.linktext",
    uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.routes.CheckAnswersThatFailedController.page(_).url,
    "checkfailed",
    SubmissionReview.Action.CheckFailsAndWarnings
  ) _

  private def buildCheckApplicationNameItem = buildChecklistItemIfActionIsRequired(
    "checklist.checkapplication.linktext.name",
    uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.routes.CheckApplicationNameController.page(_).url,
    "checkname",
    SubmissionReview.Action.CheckApplicationName
  ) _

  private def buildCheckCompanyRegistrationItem = buildChecklistItemIfActionIsRequired(
    "checklist.checkapplication.linktext.company",
    uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.routes.CheckCompanyRegistrationController.page(_).url,
    "checkcompany",
    SubmissionReview.Action.CheckCompanyRegistration
  ) _

  private def buildCheckUrlsItem = buildChecklistItemIfActionIsRequired(
    "checklist.checkapplication.linktext.urls",
    uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.routes.CheckUrlsController.checkUrlsPage(_).url,
    "checkurls",
    SubmissionReview.Action.CheckUrls
  ) _

  private def buildCheckSandboxTestingItem = buildChecklistItemIfActionIsRequired(
    "checklist.checkapplication.linktext.sandbox",
    uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.routes.CheckSandboxController.checkSandboxPage(_).url,
    "checksandbox",
    SubmissionReview.Action.CheckSandboxTesting
  ) _

  private def buildCheckFraudItem = buildChecklistItemIfActionIsRequired(
    "checklist.checkapplication.linktext.fraud",
    uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.routes.CheckFraudController.checkFraudPage(_).url,
    "checkfraud",
    SubmissionReview.Action.CheckFraudPreventionData
  ) _

  private def buildArrangeDemoItem = buildChecklistItemIfActionIsRequired(
    "checklist.checkapplication.linktext.demo",
    uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.routes.ArrangeDemoController.page(_).url,
    "arrangedemo",
    SubmissionReview.Action.ArrangedDemo
  ) _

  private def buildAnswersThatPassedItem = buildChecklistItemIfActionIsRequired(
    "checklist.checkpassed.linktext",
    uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.routes.CheckAnswersThatPassedController.checkAnswersThatPassedPage(_).url,
    "checkpassed",
    SubmissionReview.Action.CheckPassedAnswers
  ) _

}
