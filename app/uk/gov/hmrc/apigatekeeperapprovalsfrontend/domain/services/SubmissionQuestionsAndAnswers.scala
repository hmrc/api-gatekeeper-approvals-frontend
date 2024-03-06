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

import uk.gov.hmrc.apiplatform.modules.submissions.domain.models._
import uk.gov.hmrc.apiplatform.modules.submissions.domain.services.ActualAnswersAsText

object SubmissionQuestionsAndAnswers {
  case class QuestionAndAnswer(question: String, answer: String)

  case class QuestionAndAnswerGroup(heading: String, questionsAndAnswers: List[QuestionAndAnswer]) {
    lazy val isEmpty = questionsAndAnswers.isEmpty
  }

  def isDisplayable(answer: ActualAnswer) = answer match {
    case ActualAnswer.NoAnswer | ActualAnswer.AcknowledgedAnswer => false
    case _                                                       => true
  }

  def apply(submission: Submission, instance: Submission.Instance): List[QuestionAndAnswerGroup] = {
    def questionItemToQuestionAndAnswer(questionItem: QuestionItem): Option[QuestionAndAnswer] = {
      val question = questionItem.question.wording.value
      instance.answersToQuestions.get(questionItem.question.id).filter(isDisplayable(_)).map(answer => QuestionAndAnswer(question, ActualAnswersAsText(answer)))
    }

    def questionnaireToQuestionAndAnswerGroup(questionnaire: Questionnaire) = {
      val questionsAndAnswers = questionnaire.questions.map(questionItemToQuestionAndAnswer).toList.flatten
      QuestionAndAnswerGroup(questionnaire.label.value, questionsAndAnswers)
    }

    submission.groups.flatMap(_.links).map(questionnaireToQuestionAndAnswerGroup(_)).toList.filterNot(_.isEmpty)
  }
}
