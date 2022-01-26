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

object SubmissionReview {
  sealed trait Status

  object Status {
    case object ReviewNotStarted extends Status
    case object ReviewInProgress extends Status
    case object ReviewCompleted extends Status
  }
}

import SubmissionReview.Status

case class SubmissionReview(
  submissionId: Submission.Id,
  instanceIndex: Int,
  checkedFailsAndWarnings: Status = SubmissionReview.Status.ReviewNotStarted,
  emailedResponsibleIndividual: Status = SubmissionReview.Status.ReviewNotStarted,
  checkedUrls: Status = SubmissionReview.Status.ReviewNotStarted,
  checkedForSandboxTesting: Status = SubmissionReview.Status.ReviewNotStarted,
  checkedPassedAnswers: Status = SubmissionReview.Status.ReviewNotStarted
) {
  lazy val isCompleted = List(checkedFailsAndWarnings, emailedResponsibleIndividual, checkedUrls, checkedForSandboxTesting, checkedPassedAnswers).forall(s => s == Status.ReviewCompleted)
}