/*
 * Copyright 2021 HM Revenue & Customs
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

import uk.gov.hmrc.apigatekeeperapprovalsfrontend.utils.HmrcSpec
import java.util.UUID

class MarkedSubmissionSpec extends HmrcSpec {

  private def buildMarkedSubmissionWithMarks(marks: String*): MarkedSubmission = {
    val submission = mock[Submission]
    val markedQuestions = marks.toList.map(m => UUID.randomUUID.toString -> Map(("markAnswer" -> m))).toMap
    MarkedSubmission(submission, markedQuestions)
  }

  trait Setup {
    val submissionWithAllPasses = buildMarkedSubmissionWithMarks("pass", "pass", "pass")
    val submissionWithOneFail = buildMarkedSubmissionWithMarks("pass", "fail", "pass")
    val submissionWithThreeWarnings = buildMarkedSubmissionWithMarks("pass", "warn", "warn", "pass", "warn")
    val submissionWithFourWarnings = buildMarkedSubmissionWithMarks("warn", "warn", "warn", "pass", "warn")
    val submissionWithNoQuestions = buildMarkedSubmissionWithMarks()
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
      submissionWithAllPasses.hasWarnings shouldBe false
    }
    "be true if 1 warning" in new Setup {
      submissionWithThreeWarnings.hasWarnings shouldBe true
    }
    "be true if 2 warnings" in new Setup {
      submissionWithFourWarnings.hasWarnings shouldBe true
    }
    "be false if 1 fail" in new Setup {
      submissionWithOneFail.hasWarnings shouldBe false
    }
    "be false if no questions" in new Setup {
      submissionWithNoQuestions.hasWarnings shouldBe false
    }
  }

}