/*
 * Copyright 2026 HM Revenue & Customs
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

import play.api.libs.json.*

import uk.gov.hmrc.apiplatform.modules.applications.submissions.domain.models.SubmissionId
import uk.gov.hmrc.apiplatform.modules.common.utils.BaseJsonFormattersSpec

trait SubmissionReviewData {
  val submissionIdOne     = SubmissionId.random
  val submissionIdOneText = submissionIdOne.value.toString()
  val submissionReviewOne = SubmissionReview(submissionIdOne, 0, false, true, true, true)
}

class SubmissionReviewSpec extends BaseJsonFormattersSpec with SubmissionReviewData {
  import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.services.SubmissionReviewJsonFormatters.given_Format_SubmissionReview

  "SubmissionReview" should {
    "read from Json" in {
      testFromJson[SubmissionReview](s"""{"submissionId":"$submissionIdOneText","instanceIndex":0,"declineReasons":"","grantWarnings":"","requiredActions":{"CheckCompanyRegistration":{"Review.StatusType":"notstarted"},"CheckPassedAnswers":{"Review.StatusType":"notstarted"},"CheckApplicationName":{"Review.StatusType":"notstarted"},"CheckUrls":{"Review.StatusType":"notstarted"},"CheckFailsAndWarnings":{"Review.StatusType":"notstarted"},"ArrangedDemo":{"Review.StatusType":"notstarted"},"CheckFraudPreventionData":{"Review.StatusType":"notstarted"},"CheckSandboxTesting":{"Review.StatusType":"notstarted"}}}""")(
        submissionReviewOne
      )
    }

    "write to Json" in {
      Json.fromJson(Json.toJson(submissionReviewOne)) shouldBe JsSuccess(submissionReviewOne)
    }
  }
}
