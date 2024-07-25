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

package uk.gov.hmrc.apigatekeeperapprovalsfrontend.services

import scala.concurrent.ExecutionContext.Implicits.global

import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.apiplatform.modules.gkauth.connectors.ApmConnectorMockModule

import uk.gov.hmrc.apigatekeeperapprovalsfrontend.utils.{ApiDataTestData, AsyncHmrcSpec}

class SubscriptionServiceSpec extends AsyncHmrcSpec {

  trait Setup extends ApmConnectorMockModule with ApiDataTestData {
    implicit val hc: HeaderCarrier = HeaderCarrier()
    val applicationId              = ApplicationId.random
    val context1                   = ApiContext("context1")
    val context2                   = ApiContext("context2")
    val context3                   = ApiContext("context3")
    val context4                   = ApiContext("context4")
    val apiData1                   = anApiData("serviceName1", "name1", context1.value)
    val apiData2                   = anApiData("serviceName2", "name2", context2.value)
    val apiData3                   = anApiData("serviceName3", "name3", context3.value)

    val service = new SubscriptionService(ApmConnectorMock.aMock)
  }

  "fetchSubscriptionsByApplicationId" should {
    "return correct apis for application" in new Setup {
      ApmConnectorMock.FetchApplicationWithSubscriptionData.thenReturn(
        ApiIdentifier(context1, ApiVersionNbr("1.0")),
        ApiIdentifier(context2, ApiVersionNbr("2.0")),
        ApiIdentifier(context3, ApiVersionNbr("3.0"))
      )
      ApmConnectorMock.FetchSubscribableApisForApplication.thenReturn(List(
        apiData1,
        apiData2,
        anApiData("serviceName4", "name4", "context4")
      ))
      val result = await(service.fetchSubscriptionsByApplicationId(applicationId))
      result shouldBe Set(apiData1, apiData2)
    }

    "return empty set if no application is found" in new Setup {
      ApmConnectorMock.FetchApplicationWithSubscriptionData.thenReturnNothing
      val result = await(service.fetchSubscriptionsByApplicationId(applicationId))
      result shouldBe Set()
    }

    "return empty set if no subscribable apis are found" in new Setup {
      ApmConnectorMock.FetchApplicationWithSubscriptionData.thenReturn(
        ApiIdentifier(context1, ApiVersionNbr("1.0")),
        ApiIdentifier(context2, ApiVersionNbr("2.0")),
        ApiIdentifier(context3, ApiVersionNbr("3.0"))
      )
      ApmConnectorMock.FetchSubscribableApisForApplication.thenReturnNothing
      val result = await(service.fetchSubscriptionsByApplicationId(applicationId))
      result shouldBe Set()
    }
  }

}
