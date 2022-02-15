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

package uk.gov.hmrc.apiplatform.modules.submissions.domain.services

import uk.gov.hmrc.apiplatform.modules.submissions.domain.models.Submission
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.CompanyRegistrationDetails
import uk.gov.hmrc.apiplatform.modules.submissions.domain.models.SingleChoiceAnswer
import uk.gov.hmrc.apiplatform.modules.submissions.domain.models.ActualAnswer
import uk.gov.hmrc.apiplatform.modules.submissions.domain.models.TextAnswer
import uk.gov.hmrc.apiplatform.modules.submissions.domain.models.Questionnaire
import uk.gov.hmrc.apiplatform.modules.submissions.domain.models.AskWhen
import uk.gov.hmrc.apiplatform.modules.submissions.domain.models.QuestionId
import uk.gov.hmrc.apiplatform.modules.submissions.domain.models.Submission.AnswersToQuestions

object CompanyDetailsExtractor {

  object DeriveContext {
    object Keys {
      val VAT_OR_ITSA = "VAT_OR_ITSA"
      val IN_HOUSE_SOFTWARE = "IN_HOUSE_SOFTWARE" // Stored on Application
    }
  }

  def apply(submission: Submission): CompanyRegistrationDetails = {

    def extractSingleChoiceAnswer(a: ActualAnswer): Option[String] = a match {
      case SingleChoiceAnswer(ta) => Some(ta)
      case _ => None
    }

    def extractTextAnswer(a: ActualAnswer): Option[String] = a match {
      case TextAnswer(ta) => Some(ta)
      case _ => None
    }

    def questionsToAsk(questionnaire: Questionnaire, context: AskWhen.Context, answersToQuestions: AnswersToQuestions): List[QuestionId] = {
      questionnaire.questions.collect {
        case (qi) if AskWhen.shouldAsk(context, answersToQuestions)(qi.askWhen) => qi.question.id
      }
    }

    // TODO new questionIdsOfInterest for Identify your organisation
    // and some way of getting the right answer depending on this answer
    val registrationTypeQuestionId = QuestionId("cbdf264f-be39-4638-92ff-6ecd2259c662")
    val registrationType = submission.latestInstance.answersToQuestions.get(registrationTypeQuestionId) flatMap extractSingleChoiceAnswer

    val registrationQuestionnaire = submission.findQuestionnaireContaining(registrationTypeQuestionId)
    val simpleContext = Map(DeriveContext.Keys.IN_HOUSE_SOFTWARE -> "Yes", DeriveContext.Keys.VAT_OR_ITSA -> "No")
    val registrationQuestions = questionsToAsk(registrationQuestionnaire.get, simpleContext, submission.latestInstance.answersToQuestions)
    val registrationValueQuestionId = registrationQuestions.dropWhile(_ != registrationTypeQuestionId).tail.headOption
    val registrationValue = submission.latestInstance.answersToQuestions.get(registrationValueQuestionId.get) flatMap extractTextAnswer

    CompanyRegistrationDetails(registrationType, registrationValue)
  }
}
