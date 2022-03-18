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

package uk.gov.hmrc.apiplatform.modules.submissions.domain.models

import uk.gov.hmrc.apigatekeeperapprovalsfrontend.utils.AsyncHmrcSpec
import uk.gov.hmrc.apiplatform.modules.submissions.SubmissionsTestData

class QuestionnaireSpec extends AsyncHmrcSpec {
  trait Setup extends SubmissionsTestData {
  }

  "Questionnaire" should {
    "find question by id when given a question id that exists in the questionnire" in new Setup {
      DevelopmentPractices.questionnaire.hasQuestion(DevelopmentPractices.question1.id) shouldBe true
    }

    "fail to find question by id when given a question id that does not exist in the questionnire" in new Setup {
      DevelopmentPractices.questionnaire.hasQuestion(Question.Id("bobbins")) shouldBe false
    }
  }
}
