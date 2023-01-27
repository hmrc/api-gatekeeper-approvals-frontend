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

package uk.gov.hmrc.apiplatform.modules.submissions.domain.models

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import uk.gov.hmrc.apiplatform.modules.submissions.SubmissionsTestData

class SubmissionSpec extends AnyWordSpec with Matchers with SubmissionsTestData {

  "submission questionIdsOfInterest app name" in {
    Submission.updateLatestAnswersTo(completeAnswersToQuestions)(aSubmission).latestInstance.answersToQuestions(
      aSubmission.questionIdsOfInterest.applicationNameId
    ) shouldBe TextAnswer("name of software")
  }

  "submission instance state history" in {
    aSubmission.latestInstance.statusHistory.head.isOpenToAnswers shouldBe true
    aSubmission.latestInstance.isOpenToAnswers shouldBe true
    aSubmission.status.isOpenToAnswers shouldBe true
  }

  "submission instance is in progress" in {
    aSubmission.latestInstance.isOpenToAnswers shouldBe true
  }

  "submission is in progress" in {
    aSubmission.status.isOpenToAnswers shouldBe true
  }

  "submission findQuestionnaireContaining" in {
    aSubmission.findQuestionnaireContaining(aSubmission.questionIdsOfInterest.applicationNameId) shouldBe Some(CustomersAuthorisingYourSoftware.questionnaire)
  }

  "submission setLatestAnswers" in {
    val newAnswersToQuestions = Map(
      (OrganisationDetails.question1.id -> TextAnswer("new web site"))
    )

    Submission.updateLatestAnswersTo(newAnswersToQuestions)(aSubmission).latestInstance.answersToQuestions(OrganisationDetails.question1.id) shouldBe TextAnswer("new web site")
  }
}
