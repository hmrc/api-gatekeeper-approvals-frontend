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

package uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.services

import uk.gov.hmrc.apigatekeeperapprovalsfrontend.utils.HmrcSpec
import uk.gov.hmrc.apiplatform.modules.submissions.SubmissionsTestData
import uk.gov.hmrc.apiplatform.modules.submissions.domain.models.NoAnswer
import uk.gov.hmrc.apiplatform.modules.submissions.domain.models.AcknowledgedAnswer

class SubmissionQuestionsAndAnswersSpec extends HmrcSpec {
  trait Setup extends SubmissionsTestData {
    val submissionWithAnswers = submission.setLatestAnswers(sampleAnswersToQuestions)
    val submissionWithAnswersExceptForOrgDetails = submission.setLatestAnswers(sampleAnswersToQuestions - OrganisationDetails.question1.id)
    val submissionWithNonDisplayableDevPracticesAnswers = submission.setLatestAnswers(sampleAnswersToQuestions + 
      (DevelopmentPractices.question1.id -> NoAnswer) + 
      (DevelopmentPractices.question2.id -> AcknowledgedAnswer) + 
      (DevelopmentPractices.question3.id -> NoAnswer)
    )
  }

  "SubmissionQuestionsAndAnswers" should {
    "extract questions and answers correctly" in new Setup {      
      val result = SubmissionQuestionsAndAnswers(submissionWithAnswers, 0)
      
      result.length shouldBe 3
      result.find(_.heading == "Customers authorising your software").get.questionsAndAnswers.length shouldBe 3
      result.find(_.heading == "Organisation details").get.questionsAndAnswers.length shouldBe 1
      result.find(_.heading == "Development practices").get.questionsAndAnswers.length shouldBe 3
    }

    "extract questions and answers omitting groups that have no questions" in new Setup {      
      val result = SubmissionQuestionsAndAnswers(submissionWithAnswersExceptForOrgDetails, 0)
      
      result.map(_.heading) should contain only("Customers authorising your software", "Development practices")
    }

    "extract questions and answers omitting groups that have only non-displayable answers" in new Setup {      
      val result = SubmissionQuestionsAndAnswers(submissionWithNonDisplayableDevPracticesAnswers, 0)
      
      result.map(_.heading) should contain only("Customers authorising your software", "Organisation details")
    }

  }
}