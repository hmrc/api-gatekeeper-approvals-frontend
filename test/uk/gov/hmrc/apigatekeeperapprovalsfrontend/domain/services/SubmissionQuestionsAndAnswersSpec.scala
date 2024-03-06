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

package uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.services

import uk.gov.hmrc.apiplatform.modules.common.utils.HmrcSpec
import uk.gov.hmrc.apiplatform.modules.submissions.SubmissionsTestData
import uk.gov.hmrc.apiplatform.modules.submissions.domain.models.{ActualAnswer, Submission}

class SubmissionQuestionsAndAnswersSpec extends HmrcSpec {

  trait Setup extends SubmissionsTestData {
    val submissionWithAnswers = Submission.updateLatestAnswersTo(completeAnswersToQuestions)(aSubmission)

    val submissionWithAnswersExceptForOrgDetails = Submission.updateLatestAnswersTo(
      completeAnswersToQuestions - OrganisationDetails.question1.id - OrganisationDetails.questionRI1.id - OrganisationDetails.questionRI2.id - OrganisationDetails.questionRI3.id - OrganisationDetails.question2.id - OrganisationDetails.question2c.id
    )(aSubmission)

    val submissionWithNonDisplayableDevPracticesAnswers = Submission.updateLatestAnswersTo(completeAnswersToQuestions +
      (DevelopmentPractices.question1.id -> ActualAnswer.NoAnswer) +
      (DevelopmentPractices.question2.id -> ActualAnswer.AcknowledgedAnswer) +
      (DevelopmentPractices.question3.id -> ActualAnswer.NoAnswer))(aSubmission)
  }

  "SubmissionQuestionsAndAnswers" should {
    "extract questions and answers correctly" in new Setup {
      val result = SubmissionQuestionsAndAnswers(submissionWithAnswers, submissionWithAnswers.latestInstance)

      result.length shouldBe 3
      result.find(_.heading == "Customers authorising your software").value.questionsAndAnswers.length shouldBe 5
      result.find(_.heading == "Organisation details").value.questionsAndAnswers.length shouldBe 6
      result.find(_.heading == "Development practices").value.questionsAndAnswers.length shouldBe 3
    }

    "extract questions and answers omitting groups that have no questions" in new Setup {
      val result = SubmissionQuestionsAndAnswers(submissionWithAnswersExceptForOrgDetails, submissionWithAnswersExceptForOrgDetails.latestInstance)

      result.map(_.heading) should contain.only("Customers authorising your software", "Development practices")
    }

    "extract questions and answers omitting groups that have only non-displayable answers" in new Setup {
      val result = SubmissionQuestionsAndAnswers(submissionWithNonDisplayableDevPracticesAnswers, submissionWithNonDisplayableDevPracticesAnswers.latestInstance)

      result.map(_.heading) should contain.only("Customers authorising your software", "Organisation details")
    }

  }
}
