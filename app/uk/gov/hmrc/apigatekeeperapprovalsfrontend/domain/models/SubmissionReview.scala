/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models

import uk.gov.hmrc.apiplatform.modules.submissions.domain.models.Submission
import org.joda.time.DateTime

object SubmissionReview {
  sealed trait Status

  object Status {
    case object NotStarted extends Status
    case object InProgress extends Status
    case object Completed extends Status
  }

  sealed trait Action

  object Action {
    case object CheckFailsAndWarnings extends Action
    case object EmailResponsibleIndividual extends Action
    case object ConfirmResponsibleIndividualVerified extends Action
    case object CheckApplicationName extends Action
    case object CheckCompanyRegistration extends Action
    case object CheckUrls extends Action
    case object CheckSandboxTesting extends Action
    case object CheckFraudPreventionData extends Action
    case object ArrangedDemo extends Action
    case object CheckPassedAnswers extends Action

    def fromText(text: String): Option[Action] = {
      import cats.implicits._
      text match {
        case "CheckFailsAndWarnings"                => CheckFailsAndWarnings.some
        case "EmailResponsibleIndividual"           => EmailResponsibleIndividual.some
        case "ConfirmResponsibleIndividualVerified" => ConfirmResponsibleIndividualVerified.some
        case "CheckApplicationName"                 => CheckApplicationName.some
        case "CheckCompanyRegistration"             => CheckCompanyRegistration.some
        case "CheckUrls"                            => CheckUrls.some
        case "CheckSandboxTesting"                  => CheckSandboxTesting.some
        case "CheckFraudPreventionData"             => CheckFraudPreventionData.some
        case "ArrangedDemo"                         => ArrangedDemo.some
        case "CheckPassedAnswers"                   => CheckPassedAnswers.some
        case _                                      => None
      }
    }

    def toText(action: Action) = action match {
        case CheckFailsAndWarnings                => "CheckFailsAndWarnings"
        case EmailResponsibleIndividual           => "EmailResponsibleIndividual"
        case ConfirmResponsibleIndividualVerified => "ConfirmResponsibleIndividualVerified"
        case CheckApplicationName                 => "CheckApplicationName"
        case CheckCompanyRegistration             => "CheckCompanyRegistration"
        case CheckUrls                            => "CheckUrls"
        case CheckSandboxTesting                  => "CheckSandboxTesting"
        case CheckFraudPreventionData             => "CheckFraudPreventionData"
        case ArrangedDemo                         => "ArrangedDemo"
        case CheckPassedAnswers                   => "CheckPassedAnswers"
    }
  }

  private def apply(
    submissionId: Submission.Id,
    instanceIndex: Int,
    requiredActions: List[Action]
  ): SubmissionReview =
    SubmissionReview(submissionId, instanceIndex, "", "", None, None, requiredActions.map(a => (a -> Status.NotStarted)).toMap)

  def apply(submissionId: Submission.Id, instanceIndex: Int, isSuccessful: Boolean, hasWarnings: Boolean, requiresFraudCheck: Boolean, requiresDemo: Boolean): SubmissionReview = {
    val alternativeActions: List[Action] = 
      (isSuccessful, hasWarnings) match {
        case (true, false)  => List.empty
        case (_, _)         => List(Action.CheckFailsAndWarnings)
      }

    val fraudAction = if (requiresFraudCheck) List(Action.CheckFraudPreventionData) else List()
    val demoAction = if (requiresDemo) List(Action.ArrangedDemo) else List()

    val fixedActions: List[Action] =
      List(
        Action.EmailResponsibleIndividual, 
        Action.ConfirmResponsibleIndividualVerified,
        Action.CheckApplicationName,
        Action.CheckCompanyRegistration,
        Action.CheckUrls,
        Action.CheckSandboxTesting,
        Action.CheckPassedAnswers
      )

    SubmissionReview(
      submissionId,
      instanceIndex, 
      alternativeActions ++ fraudAction ++ demoAction ++ fixedActions
    )
  }

  val updateDeclineReasons: String => SubmissionReview => SubmissionReview = reasons => review => {
    review.copy(declineReasons = reasons)
  }

  val updateGrantWarnings: String => SubmissionReview => SubmissionReview = warnings => review => {
    review.copy(grantWarnings = warnings)
  }

  val updateReviewActionStatus : (Action, Status) => SubmissionReview => SubmissionReview = (action, newStatus) => review => {
    require(review.requiredActions.keySet.contains(action))
    review.copy(requiredActions = review.requiredActions + (action -> newStatus))
  }

  case class VerifiedByDetails(
    verified: Boolean,
    timestamp: Option[DateTime] = None
  )

  val updateVerifiedByDetails: SubmissionReview.VerifiedByDetails => SubmissionReview => SubmissionReview = verified => review => {
    review.copy(verifiedByDetails = Some(verified))
  }

  val updateEscalatedTo: String => SubmissionReview => SubmissionReview = escalated => review => {
    review.copy(escalatedTo = Some(escalated))
  }
}

case class SubmissionReview private(
  submissionId: Submission.Id,
  instanceIndex: Int,
  declineReasons: String,
  grantWarnings: String,
  verifiedByDetails: Option[SubmissionReview.VerifiedByDetails],
  escalatedTo: Option[String],
  requiredActions: Map[SubmissionReview.Action, SubmissionReview.Status]
) {
  lazy val isCompleted = requiredActions.values.filterNot(_ == SubmissionReview.Status.Completed).isEmpty
}