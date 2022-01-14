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

package uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.models

import uk.gov.hmrc.modules.submissions.domain.models.MarkedSubmission
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.actions.HasApplication

class MarkedSubmissionApplicationRequest[A](val markedSubmission: MarkedSubmission, applicationRequest: ApplicationRequest[A]) extends ApplicationRequest[A](applicationRequest.application, applicationRequest.loggedInRequest) with HasApplication {
  lazy val submission = markedSubmission.submission
  lazy val answersToQuestions = submission.answersToQuestions
  lazy val markedAnswers = markedSubmission.markedAnswers
}