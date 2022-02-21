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
    case object NotStarted extends Status
    case object InProgress extends Status
    case object Completed extends Status
  }
}

import SubmissionReview.Status

case class SubmissionReview(
  submissionId: Submission.Id,
  instanceIndex: Int,
  checkedFailsAndWarnings: Status = SubmissionReview.Status.NotStarted,
  emailedResponsibleIndividual: Status = SubmissionReview.Status.NotStarted,
  checkedUrls: Status = SubmissionReview.Status.NotStarted,
  checkedCompanyRegistration: Status = SubmissionReview.Status.NotStarted,
  checkedForSandboxTesting: Status = SubmissionReview.Status.NotStarted,
  checkedPassedAnswers: Status = SubmissionReview.Status.NotStarted,
  declineReasons: String = ""
) {
  lazy val isCompleted = List(checkedFailsAndWarnings, emailedResponsibleIndividual, checkedUrls, checkedCompanyRegistration, checkedForSandboxTesting, checkedPassedAnswers).forall(s => s == Status.Completed)
}