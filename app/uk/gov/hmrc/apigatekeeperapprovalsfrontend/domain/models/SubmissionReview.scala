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

package uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models

import uk.gov.hmrc.apiplatform.modules.applications.submissions.domain.models.SubmissionId

case class SubmissionReview(
    submissionId: SubmissionId,
    instanceIndex: Int,
    declineReasons: String,
    grantWarnings: String,
    escalatedTo: Option[String],
    requiredActions: Map[ReviewAction, ReviewStatus]
  ) {
  lazy val isCompleted = requiredActions.values.filterNot(_ == ReviewStatus.Completed).isEmpty
}

object SubmissionReview {

  private def apply(
      submissionId: SubmissionId,
      instanceIndex: Int,
      requiredActions: List[ReviewAction]
    ): SubmissionReview =
    SubmissionReview(submissionId, instanceIndex, "", "", None, requiredActions.map(a => (a -> ReviewStatus.NotStarted)).toMap)

  def apply(submissionId: SubmissionId, instanceIndex: Int, isSuccessful: Boolean, hasWarnings: Boolean, requiresFraudCheck: Boolean, requiresDemo: Boolean): SubmissionReview = {
    val alternativeActions: List[ReviewAction] =
      (isSuccessful, hasWarnings) match {
        case (true, false) => List.empty
        case (_, _)        => List(ReviewAction.CheckFailsAndWarnings)
      }

    val fraudAction = if (requiresFraudCheck) List(ReviewAction.CheckFraudPreventionData) else List()
    val demoAction  = if (requiresDemo) List(ReviewAction.ArrangedDemo) else List()

    val fixedActions: List[ReviewAction] =
      List(
        ReviewAction.CheckApplicationName,
        ReviewAction.CheckCompanyRegistration,
        ReviewAction.CheckUrls,
        ReviewAction.CheckSandboxTesting,
        ReviewAction.CheckPassedAnswers
      )

    SubmissionReview(
      submissionId,
      instanceIndex,
      alternativeActions ++ fraudAction ++ demoAction ++ fixedActions
    )
  }

  val updateDeclineReasons: String => SubmissionReview => SubmissionReview = reasons =>
    review => {
      review.copy(declineReasons = reasons)
    }

  val updateGrantWarnings: String => SubmissionReview => SubmissionReview = warnings =>
    review => {
      review.copy(grantWarnings = warnings)
    }

  val updateReviewActionStatus: (ReviewAction, ReviewStatus) => SubmissionReview => SubmissionReview = (action, newStatus) =>
    review => {
      require(review.requiredActions.keySet.contains(action))
      review.copy(requiredActions = review.requiredActions + (action -> newStatus))
    }

  val updateEscalatedTo: String => SubmissionReview => SubmissionReview = escalated =>
    review => {
      review.copy(escalatedTo = Some(escalated))
    }

  import play.api.libs.json._
  import SubmissionId.given
  import ReviewAction.given
  import ReviewStatus.given

  given Format[SubmissionReview] = Json.format[SubmissionReview]
}
