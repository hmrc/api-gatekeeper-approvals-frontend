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
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.ResponsibleIndividual
import uk.gov.hmrc.apiplatform.modules.submissions.domain.models.TextAnswer
import uk.gov.hmrc.apiplatform.modules.submissions.domain.models.ActualAnswer

object ResponsibleIndividualExtractor {
  def apply(submission: Submission): Option[ResponsibleIndividual] = {
    import cats.implicits._
    import cats.Applicative

    def extractTextAnswer(a: ActualAnswer): Option[String] = a match {
      case TextAnswer(ta) => Some(ta)
      case _ => None
    }

    val name = 
      submission.latestInstance.answersToQuestions.get(submission.questionIdsOfInterest.responsibleIndividualNameId) flatMap extractTextAnswer

    val email = submission.latestInstance.answersToQuestions.get(submission.questionIdsOfInterest.responsibleIndividualEmailId) flatMap extractTextAnswer
    Applicative[Option].map2(name, email)(ResponsibleIndividual.apply)
  }
}
