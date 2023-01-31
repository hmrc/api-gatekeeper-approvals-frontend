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

package uk.gov.hmrc.apiplatform.modules.submissions.services

import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future.successful

import uk.gov.hmrc.apiplatform.modules.submissions.MarkedSubmissionsTestData
import uk.gov.hmrc.apiplatform.modules.submissions.connectors.SubmissionsConnector
import uk.gov.hmrc.apiplatform.modules.submissions.domain.models.{TermsOfUseInvitation, TermsOfUseInvitationSuccessful}
import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.ApplicationId
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.utils.{ApplicationTestData, AsyncHmrcSpec}

class SubmissionServiceSpec extends AsyncHmrcSpec with MarkedSubmissionsTestData with ApplicationTestData {

  trait Setup {
    implicit val hc                                    = HeaderCarrier()
    val applicationId                                  = ApplicationId.random
    val mockSubmissionsConnector: SubmissionsConnector = mock[SubmissionsConnector]
    val requestedBy                                    = "bob@example.com"
    val app                                            = anApplication(applicationId)

    val underTest = new SubmissionService(mockSubmissionsConnector)
  }

  "fetchLatestSubmission" should {
    "successfully fetch latest submission" in new Setup {
      when(mockSubmissionsConnector.fetchLatestSubmission(*[ApplicationId])(*)).thenReturn(successful(Some(aSubmission)))
      val result = await(underTest.fetchLatestSubmission(applicationId))
      result shouldBe Some(aSubmission)
    }
  }

  "fetchLatestMarkedSubmission" should {
    "successfully fetch latest marked submission" in new Setup {
      when(mockSubmissionsConnector.fetchLatestMarkedSubmission(*[ApplicationId])(*)).thenReturn(successful(Some(markedSubmission)))
      val result = await(underTest.fetchLatestMarkedSubmission(applicationId))
      result shouldBe Some(markedSubmission)
    }
  }

  "grant" should {
    "call submission connector correctly" in new Setup {
      when(mockSubmissionsConnector.grant(eqTo(applicationId), eqTo(requestedBy))(*)).thenReturn(successful(Right(app)))
      val result = await(underTest.grant(applicationId, requestedBy))
      result shouldBe Right(app)
    }
  }

  "grantWithWarnings" should {
    "call submission connector correctly" in new Setup {
      val warnings = "warn"
      val manager  = "manager"
      when(mockSubmissionsConnector.grantWithWarnings(eqTo(applicationId), eqTo(requestedBy), eqTo(warnings), eqTo(Some(manager)))(*)).thenReturn(successful(Right(app)))
      val result   = await(underTest.grantWithWarnings(applicationId, requestedBy, warnings, Some(manager)))
      result shouldBe Right(app)
    }
  }

  "termsOfUseInvite" should {
    "call submission connector correctly" in new Setup {
      when(mockSubmissionsConnector.termsOfUseInvite(eqTo(applicationId))(*)).thenReturn(successful(Right(TermsOfUseInvitationSuccessful)))
      val result = await(underTest.termsOfUseInvite(applicationId))
      result shouldBe Right(TermsOfUseInvitationSuccessful)
    }
  }

  "fetchTermsOfUseInvitation" should {
    "call submission connector correctly" in new Setup {
      val invite = TermsOfUseInvitation(applicationId, Instant.now, Instant.now, Instant.now, None)
      when(mockSubmissionsConnector.fetchTermsOfUseInvitation(eqTo(applicationId))(*)).thenReturn(successful(Some(invite)))
      val result = await(underTest.fetchTermsOfUseInvitation(applicationId))
      result shouldBe Some(invite)
    }
  }
}
