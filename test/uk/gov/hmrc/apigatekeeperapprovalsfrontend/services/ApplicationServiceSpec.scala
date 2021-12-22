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

package uk.gov.hmrc.apigatekeeperapprovalsfrontend.services

import org.scalatestplus.play.guice.GuiceOneAppPerSuite

import scala.concurrent.ExecutionContext.Implicits.global
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.ApplicationId
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.utils.AsyncHmrcSpec
import uk.gov.hmrc.modules.stride.connectors.mocks.ThirdPartyApplicationConnectorMockModule
import uk.gov.hmrc.http.HeaderCarrier

class ApplicationServiceSpec extends AsyncHmrcSpec with GuiceOneAppPerSuite {

  trait Setup extends ThirdPartyApplicationConnectorMockModule {
    implicit val hc: HeaderCarrier = HeaderCarrier()
    val applicationId = ApplicationId.random
    val service = new ApplicationService(ThirdPartyApplicationConnectorMock.aMock)
  }

  "fetchByApplicationId" should {
    "return the correct application" in new Setup {
      ThirdPartyApplicationConnectorMock.FetchApplicationById.thenReturn()
      val result = await(service.fetchByApplicationId(applicationId))
      result.value.id shouldBe applicationId
    }
  }

  "fetchLatestMarkedSubmission" should {
    "return the correct marked submission" in new Setup {
      ThirdPartyApplicationConnectorMock.FetchLatestMarkedSubmission.thenReturn()
      val result = await(service.fetchLatestMarkedSubmission(applicationId))
      result.value.submission.applicationId shouldBe applicationId
    }
  }

}
