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

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import uk.gov.hmrc.apiplatform.modules.submissions.domain.models._
import uk.gov.hmrc.apiplatform.modules.submissions.SubmissionsTestData._

class SubmissionSpec extends AnyWordSpec with Matchers {
    private def extendedSubmissionWithStates(states: QuestionnaireState*) = {
        
        val questionnaireProgress = states.zipWithIndex map {
            case(state, index) => QuestionnaireId(s"q$index") -> QuestionnaireProgress(state, List())
        } toMap

        ExtendedSubmission(submission, questionnaireProgress)
    }

    "empty questionnaire is complete" in {
        extendedSubmissionWithStates().isCompleted shouldBe true
    }

    "questionnaire with single Completed question is complete" in {
        extendedSubmissionWithStates(QuestionnaireState.Completed).isCompleted shouldBe true
    }

    "questionnaire with single NotApplicable question is complete" in {
        extendedSubmissionWithStates(QuestionnaireState.NotApplicable).isCompleted shouldBe true
    }

    "questionnaire with single InProgress question is incomplete" in {
        extendedSubmissionWithStates(QuestionnaireState.InProgress).isCompleted shouldBe false
    }

    "questionnaire with single NotStarted question is incomplete" in {
        extendedSubmissionWithStates(QuestionnaireState.NotStarted).isCompleted shouldBe false
    }

    "questionnaire with multiple Completed/NotApplicable questions is complete" in {
        extendedSubmissionWithStates(QuestionnaireState.Completed, QuestionnaireState.NotApplicable, QuestionnaireState.Completed, QuestionnaireState.NotApplicable).isCompleted shouldBe true
    }

    "questionnaire with one InProgress question among many is incomplete" in {
        extendedSubmissionWithStates(QuestionnaireState.Completed, QuestionnaireState.NotApplicable, QuestionnaireState.Completed, QuestionnaireState.InProgress, QuestionnaireState.NotApplicable).isCompleted shouldBe false
    }

    "submission questionIdsOfInterest app name" in {
        submission.setLatestAnswers(sampleAnswersToQuestions).latestInstance.answersToQuestions(submission.questionIdsOfInterest.applicationNameId) shouldBe TextAnswer("name of software")
    }

    "submission instance state history" in {
        submission.latestInstance.statusHistory.head.isInProgress shouldBe true
    }

    "submission instance is in progress" in {
        submission.latestInstance.isInProgress shouldBe true
    }
    
    "submission is in progress" in {
        submission.isInProgress shouldBe true
    }

    "submission findQuestionnaireContaining" in {
        submission.findQuestionnaireContaining(submission.questionIdsOfInterest.applicationNameId) shouldBe Some(CustomersAuthorisingYourSoftware.questionnaire)
    }

    "submission setLatestAnswers" in {
        val newAnswersToQuestions = Map(
            (OrganisationDetails.question1.id -> TextAnswer("new web site"))
        )

        submission.setLatestAnswers(newAnswersToQuestions).latestInstance.answersToQuestions(OrganisationDetails.question1.id) shouldBe TextAnswer("new web site")
    }
}