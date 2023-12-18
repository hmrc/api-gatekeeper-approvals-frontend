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

import uk.gov.hmrc.apiplatform.modules.common.utils.HmrcSpec

class MarkedSubmissionSpec extends HmrcSpec {

  private def buildMarkedSubmissionWithMarks(marks: Mark*): MarkedSubmission = {
    val submission      = mock[Submission]
    val markedQuestions = marks.toList.map(m => Question.Id.random -> m).toMap
    MarkedSubmission(submission, markedQuestions)
  }

  trait Setup {
    val submissionWithAllPasses     = buildMarkedSubmissionWithMarks(Pass, Pass, Pass)
    val submissionWithOneFail       = buildMarkedSubmissionWithMarks(Pass, Fail, Pass)
    val submissionWithThreeWarnings = buildMarkedSubmissionWithMarks(Pass, Warn, Warn, Pass, Warn)
    val submissionWithFourWarnings  = buildMarkedSubmissionWithMarks(Warn, Warn, Warn, Pass, Warn)
    val submissionWithNoQuestions   = buildMarkedSubmissionWithMarks()
  }

  "MarkedSubmission.isFail" should {
    "be false if all passed" in new Setup {
      submissionWithAllPasses.isFail shouldBe false
    }
    "be false if 3 warnings" in new Setup {
      submissionWithThreeWarnings.isFail shouldBe false
    }
    "be true if 4 warnings" in new Setup {
      submissionWithFourWarnings.isFail shouldBe true
    }
    "be true if 1 fail" in new Setup {
      submissionWithOneFail.isFail shouldBe true
    }
    "be false if no questions" in new Setup {
      submissionWithNoQuestions.isFail shouldBe false
    }
  }

  "MarkedSubmission.hasWarnings" should {
    "be false if all passed" in new Setup {
      submissionWithAllPasses.isWarn shouldBe false
    }
    "be true if 1 warning" in new Setup {
      submissionWithThreeWarnings.isWarn shouldBe true
    }
    "be true if 2 warnings" in new Setup {
      submissionWithFourWarnings.isWarn shouldBe true
    }
    "be false if 1 fail" in new Setup {
      submissionWithOneFail.isWarn shouldBe false
    }
    "be false if no questions" in new Setup {
      submissionWithNoQuestions.isWarn shouldBe false
    }
  }

}
