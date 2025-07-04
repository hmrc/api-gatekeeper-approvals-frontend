/*
 * Copyright 2025 HM Revenue & Customs
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

import uk.gov.hmrc.apiplatform.modules.common.utils.FixedClock
import uk.gov.hmrc.apiplatform.modules.submissions.SubmissionsTestData
import uk.gov.hmrc.apiplatform.modules.submissions.domain.models.{ActualAnswer, Submission}

import uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.CompanyRegistrationDetails
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.utils.AsyncHmrcSpec

class CompanyDetailsExtractorSpec extends AsyncHmrcSpec with SubmissionsTestData with FixedClock {

  "CompanyDetailsExtractor" should {
    "extract details from submission with answers" in {
      val maybeCompanyDetails: Option[CompanyRegistrationDetails] = CompanyDetailsExtractor(submittedSubmission)

      maybeCompanyDetails.isDefined shouldBe true
      maybeCompanyDetails.get.registrationType shouldBe "VAT registration number"
      maybeCompanyDetails.get.registrationValue.isDefined shouldBe true
      maybeCompanyDetails.get.registrationValue.get shouldBe "123456789"
    }

    "extract details from submission with incorrect answer type for identify your organisation" in {
      val registrationTypeQuestionId = submittedSubmission.questionIdsOfInterest.identifyYourOrganisationId
      val incorrectRegTypeOrg        = Submission.updateLatestAnswersTo(
        submittedSubmission.latestInstance.answersToQuestions
          + (registrationTypeQuestionId -> ActualAnswer.TextAnswer("Should be a single choice"))
      )(submittedSubmission)

      val maybeCompanyDetails: Option[CompanyRegistrationDetails] = CompanyDetailsExtractor(incorrectRegTypeOrg)

      maybeCompanyDetails.isDefined shouldBe false
    }

    "extract details from submission with incorrect answer type for VAT number" in {
      val vatNumberQuestionId = OrganisationDetails.question2c.id
      val noVatNumberOrg      = Submission.updateLatestAnswersTo(
        submittedSubmission.latestInstance.answersToQuestions
          + (vatNumberQuestionId -> ActualAnswer.SingleChoiceAnswer("Should be a text answer"))
      )(submittedSubmission)

      val maybeCompanyDetails: Option[CompanyRegistrationDetails] = CompanyDetailsExtractor(noVatNumberOrg)

      maybeCompanyDetails.isDefined shouldBe true
      maybeCompanyDetails.get.registrationType shouldBe "VAT registration number"
      maybeCompanyDetails.get.registrationValue.isDefined shouldBe false
    }

    "fail to extract details from submission with no answers" in {
      val maybeCompanyDetails: Option[CompanyRegistrationDetails] = CompanyDetailsExtractor(aSubmission)

      maybeCompanyDetails.isDefined shouldBe false
    }
  }
}
