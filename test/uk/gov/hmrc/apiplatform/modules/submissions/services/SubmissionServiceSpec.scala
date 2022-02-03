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

package uk.gov.hmrc.apiplatform.modules.submissions.services

import uk.gov.hmrc.apigatekeeperapprovalsfrontend.utils.AsyncHmrcSpec
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.apiplatform.modules.submissions.connectors.SubmissionsConnector
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.ApplicationId
import uk.gov.hmrc.apiplatform.modules.submissions.MarkedSubmissionsTestData
import scala.concurrent.Future.successful
import scala.concurrent.ExecutionContext.Implicits.global

class SubmissionServiceSpec extends AsyncHmrcSpec with MarkedSubmissionsTestData {
  
  trait Setup {
    implicit val hc = HeaderCarrier()
    val applicationId = ApplicationId.random
    val mockSubmissionsConnector: SubmissionsConnector = mock[SubmissionsConnector]

    val underTest = new SubmissionService(mockSubmissionsConnector)
  }

  "fetchLatestSubmission" should {
    "successfully fetch latest submission" in new Setup {
      when(mockSubmissionsConnector.fetchLatestSubmission(*[ApplicationId])(*)).thenReturn(successful(Some(extendedSubmission)))
      val result = await(underTest.fetchLatestSubmission(applicationId))
      result shouldBe Some(extendedSubmission)
    }
  }

  "fetchLatestMarkedSubmission" should {
    "successfully fetch latest marked submission" in new Setup {
      when(mockSubmissionsConnector.fetchLatestMarkedSubmission(*[ApplicationId])(*)).thenReturn(successful(Some(markedSubmission)))
      val result = await(underTest.fetchLatestMarkedSubmission(applicationId))
      result shouldBe Some(markedSubmission)
    }
  }
}
