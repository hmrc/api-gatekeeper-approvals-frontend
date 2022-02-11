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

object CompanyDetailsExtractor {
  def apply(submission: Submission): CompanyRegistrationDetails = {

    def extractSingleChoiceAnswer(a: ActualAnswer): Option[String] = a match {
      case SingleChoiceAnswer(ta) => Some(ta)
      case _ => None
    }

    def extractTextAnswer(a: ActualAnswer): Option[String] = a match {
      case TextAnswer(ta) => Some(ta)
      case _ => None
    }

    // TODO new questionIdsOfInterest for Identify your organisation
    // and some way of getting the right answer depending on this answer
    val registrationType = 
      submission.latestInstance.answersToQuestions.get(submission.questionIdsOfInterest.responsibleIndividualNameId) flatMap extractSingleChoiceAnswer

    val registrationValue = submission.latestInstance.answersToQuestions.get(submission.questionIdsOfInterest.responsibleIndividualEmailId) flatMap extractTextAnswer

    CompanyRegistrationDetails(registrationType, registrationValue)
  }
}
