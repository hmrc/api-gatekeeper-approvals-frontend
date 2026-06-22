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

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext
import scala.concurrent.Future.successful

import play.api.mvc.*

import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.*
import uk.gov.hmrc.apiplatform.modules.common.domain.models.ApplicationId
import uk.gov.hmrc.apiplatform.modules.gkauth.services.StrideAuthorisationService
import uk.gov.hmrc.apiplatform.modules.submissions.domain.models.Submission
import uk.gov.hmrc.apiplatform.modules.submissions.services.*

import uk.gov.hmrc.apigatekeeperapprovalsfrontend.config.ErrorHandler
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.*
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
  import ChecklistController.*
  import Extensions.*

  type RequiredActions = Map[SubmissionReview.Action, SubmissionReview.Status]

  enum AutomaticChecksResult {
    case PASSED_WITHOUT_WARNINGS, PASSED_WITH_WARNINGS, FAILED
  }

  object AutomaticChecksResult {

    def apply(isSuccessful: Boolean, hasWarnings: Boolean): AutomaticChecksResult = {
      (isSuccessful, hasWarnings) match {
        case (true, false) => PASSED_WITHOUT_WARNINGS
        case (true, true)  => PASSED_WITH_WARNINGS
        case (false, _)    => FAILED
      }
    }
  }

  import AutomaticChecksResult.*

  private def setupSubmissionReview(submission: Submission, isSuccessful: Boolean, hasWarnings: Boolean) = {
    val requiresFraudCheck = SubmissionRequiresFraudCheck(submission)
    val requiresDemo       = SubmissionRequiresDemo(submission)
    submissionReviewService.findOrCreateReview(submission.id, submission.latestInstance.index, isSuccessful, hasWarnings, requiresFraudCheck, requiresDemo)
  }

  def checklistPage(rawApplicationId: UUID): Action[AnyContent] = loggedInThruStrideWithApplicationAndSubmission(rawApplicationId) { implicit request =>
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
      sections = buildChecklistSections(rawApplicationId, review.requiredActions, automaticChecksResult)
      _        = logger.info("Section Item Statuses " + sections.flatMap(_.items).flatMap(i => s"${i.labelMsgId} : ${i.status}"))
    } yield Ok(checklistPage(ViewModel(request.application.id, appName, topMsgId, sections, isInHouseSoftware, isDeleted)))
  }

  def declineRequest(rawApplicationId: UUID): Action[AnyContent] = loggedInThruStrideWithApplicationAndSubmission(rawApplicationId) { implicit request =>
    val isSuccessful = !request.markedSubmission.isFail
    val hasWarnings  = request.markedSubmission.isWarn
    for {
      _ <- setupSubmissionReview(request.submission, isSuccessful, hasWarnings)
    } yield Redirect(uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.routes.DeclinedJourneyController.provideReasonsPage(rawApplicationId))
  }

  def checklistAction(rawApplicationId: UUID): Action[AnyContent] = loggedInThruStrideWithApplicationAndSubmission(rawApplicationId) { implicit request =>
    request.body.asFormUrlEncoded.getOrElse(Map.empty).get("submit-action").flatMap(_.headOption) match {
      case Some("checked")         => successful(Redirect(uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.routes.ConfirmYourDecisionController.page(rawApplicationId)))
      case Some("come-back-later") => successful(Redirect(uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.routes.ApplicationSubmissionsController.page(rawApplicationId)))
      case _                       => successful(BadRequest("Invalid submit-action found in request"))
    }
  }

  private def buildChecklistSections(rawApplicationId: UUID, requiredActions: RequiredActions, automaticChecksResult: AutomaticChecksResult): List[ChecklistSection] = {
    List(
      buildFailsAndWarnsSection(rawApplicationId, requiredActions, automaticChecksResult),
      buildCheckApplicationSection(rawApplicationId, requiredActions),
      buildAnswersThatPassedSection(rawApplicationId, requiredActions)
    ).filter(!_.isEmpty)
  }

  private def buildFailsAndWarnsSection(rawApplicationId: UUID, requiredActions: RequiredActions, automaticChecksResult: AutomaticChecksResult): ChecklistSection = {
    val (titleMsgId, checkListItems) = automaticChecksResult match {
      case PASSED_WITH_WARNINGS => ("checklist.checkwarnings.heading", buildCheckWarningsItem(rawApplicationId, requiredActions))
      case FAILED               => ("checklist.checkfailed.heading", buildCheckFailuresAndWarningsItem(rawApplicationId, requiredActions))
      case _                    => ("", None)
    }

    ChecklistSection(titleMsgId, checkListItems.toList)
  }

  private def buildCheckApplicationSection(rawApplicationId: UUID, requiredActions: RequiredActions): ChecklistSection = {
    val checkApplicationNameItem     = buildCheckApplicationNameItem(rawApplicationId, requiredActions)
    val checkCompanyRegistrationItem = buildCheckCompanyRegistrationItem(rawApplicationId, requiredActions)
    val checkUrlsItem                = buildCheckUrlsItem(rawApplicationId, requiredActions)
    val checkSandboxItem             = buildCheckSandboxTestingItem(rawApplicationId, requiredActions)
    val checkFraudItem               = buildCheckFraudItem(rawApplicationId, requiredActions)
    val arrangeDemoItem              = buildArrangeDemoItem(rawApplicationId, requiredActions)

    val checklistItems = checkApplicationNameItem ++ checkCompanyRegistrationItem ++ checkUrlsItem ++ checkSandboxItem ++ checkFraudItem ++ arrangeDemoItem
    ChecklistSection("checklist.checkapplication.heading", checklistItems.toList)
  }

  private def buildAnswersThatPassedSection(rawApplicationId: UUID, requiredActions: RequiredActions): ChecklistSection = {
    ChecklistSection("checklist.checkpassed.heading", buildAnswersThatPassedItem(rawApplicationId, requiredActions).toList)
  }

  private def buildChecklistItemIfActionIsRequired(
      labelMsgId: String,
      urlFn: UUID => String,
      uid: String,
      action: SubmissionReview.Action
    )(
      rawApplicationId: UUID,
      requiredActions: RequiredActions
    ): Option[ChecklistItem] = {
    requiredActions.get(action).map(ChecklistItem(labelMsgId, urlFn(rawApplicationId), uid, _))
  }

  private def buildCheckWarningsItem = buildChecklistItemIfActionIsRequired(
    "checklist.checkwarnings.linktext",
    uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.routes.CheckAnswersThatFailedController.page(_).url,
    "checkwarnings",
    SubmissionReview.Action.CheckFailsAndWarnings
  )

  private def buildCheckFailuresAndWarningsItem = buildChecklistItemIfActionIsRequired(
    "checklist.checkfailed.linktext",
    uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.routes.CheckAnswersThatFailedController.page(_).url,
    "checkfailed",
    SubmissionReview.Action.CheckFailsAndWarnings
  )

  private def buildCheckApplicationNameItem = buildChecklistItemIfActionIsRequired(
    "checklist.checkapplication.linktext.name",
    uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.routes.CheckApplicationNameController.page(_).url,
    "checkname",
    SubmissionReview.Action.CheckApplicationName
  )

  private def buildCheckCompanyRegistrationItem = buildChecklistItemIfActionIsRequired(
    "checklist.checkapplication.linktext.company",
    uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.routes.CheckCompanyRegistrationController.page(_).url,
    "checkcompany",
    SubmissionReview.Action.CheckCompanyRegistration
  )

  private def buildCheckUrlsItem = buildChecklistItemIfActionIsRequired(
    "checklist.checkapplication.linktext.urls",
    uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.routes.CheckUrlsController.checkUrlsPage(_).url,
    "checkurls",
    SubmissionReview.Action.CheckUrls
  )

  private def buildCheckSandboxTestingItem = buildChecklistItemIfActionIsRequired(
    "checklist.checkapplication.linktext.sandbox",
    uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.routes.CheckSandboxController.checkSandboxPage(_).url,
    "checksandbox",
    SubmissionReview.Action.CheckSandboxTesting
  )

  private def buildCheckFraudItem = buildChecklistItemIfActionIsRequired(
    "checklist.checkapplication.linktext.fraud",
    uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.routes.CheckFraudController.checkFraudPage(_).url,
    "checkfraud",
    SubmissionReview.Action.CheckFraudPreventionData
  )

  private def buildArrangeDemoItem = buildChecklistItemIfActionIsRequired(
    "checklist.checkapplication.linktext.demo",
    uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.routes.ArrangeDemoController.page(_).url,
    "arrangedemo",
    SubmissionReview.Action.ArrangedDemo
  )

  private def buildAnswersThatPassedItem = buildChecklistItemIfActionIsRequired(
    "checklist.checkpassed.linktext",
    uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.routes.CheckAnswersThatPassedController.checkAnswersThatPassedPage(_).url,
    "checkpassed",
    SubmissionReview.Action.CheckPassedAnswers
  )

}
