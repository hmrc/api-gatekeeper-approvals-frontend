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

package uk.gov.hmrc.apiplatform.modules.submissions.domain.services

import uk.gov.hmrc.apiplatform.modules.submissions.domain.models.Submission.AnswersToQuestions
import uk.gov.hmrc.apiplatform.modules.submissions.domain.models._

import uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.CompanyRegistrationDetails

object CompanyDetailsExtractor {

  def apply(submission: Submission): Option[CompanyRegistrationDetails] = {

    def extractSingleChoiceAnswer(a: ActualAnswer): Option[String] = a match {
      case ActualAnswer.SingleChoiceAnswer(ta) => Some(ta)
      case _                                   => None
    }

    def extractTextAnswer(a: ActualAnswer): Option[String] = a match {
      case ActualAnswer.TextAnswer(ta) => Some(ta)
      case _                           => None
    }

    def questionsToAsk(questionnaire: Questionnaire, context: AskWhen.Context, answersToQuestions: AnswersToQuestions): List[Question.Id] = {
      questionnaire.questions.collect {
        case (qi) if AskWhen.shouldAsk(context, answersToQuestions)(qi.askWhen) => qi.question.id
      }
    }

    def getRegistrationValue(registrationTypeQuestionId: Question.Id): Option[String] = {
      // Get the answer to the next question for the value of the VAT number, UTR, etc.
      // Note that the question is dependent upon your previous answer (i.e. you'll be asked for a
      // VAT number if you answered the 'Identify your Organisation' question 'VAT registration number')
      val registrationQuestionnaire   = submission.findQuestionnaireContaining(registrationTypeQuestionId).get
      val simpleContext               = Map(AskWhen.Context.Keys.IN_HOUSE_SOFTWARE -> "Yes", AskWhen.Context.Keys.VAT_OR_ITSA -> "No")
      val registrationQuestions       = questionsToAsk(registrationQuestionnaire, simpleContext, submission.latestInstance.answersToQuestions)
      val registrationValueQuestionId = registrationQuestions.dropWhile(_ != registrationTypeQuestionId).tail.headOption
      submission.latestInstance.answersToQuestions.get(registrationValueQuestionId.get) flatMap extractTextAnswer
    }

    // Get the answer to the 'Identify your Organisation' question as the registration type
    val registrationTypeQuestionId = submission.questionIdsOfInterest.identifyYourOrganisationId
    val registrationType           = submission.latestInstance.answersToQuestions.get(registrationTypeQuestionId) flatMap extractSingleChoiceAnswer

    if (registrationType.isDefined) {
      Some(CompanyRegistrationDetails(registrationType.get, getRegistrationValue(registrationTypeQuestionId)))
    } else {
      None
    }
  }
}
