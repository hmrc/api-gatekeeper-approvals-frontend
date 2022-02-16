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

package uk.gov.hmrc.apigatekeeperapprovalsfrontend.services

import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.{ApiDefinition, ApiIdentifier, ApplicationId}
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.utils.AsyncHmrcSpec
import uk.gov.hmrc.apiplatform.modules.stride.connectors.mocks.ApmConnectorMockModule
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global

class SubscriptionServiceSpec extends AsyncHmrcSpec {

  trait Setup extends ApmConnectorMockModule {
    implicit val hc: HeaderCarrier = HeaderCarrier()
    val applicationId = ApplicationId.random
    val service = new SubscriptionService(ApmConnectorMock.aMock)
  }

  "fetchSubscriptionsByApplicationId" should {
    "return correct apis for application" in new Setup {
      val apiDefinition1 = ApiDefinition("serviceName1", "name1")
      val apiDefinition2 = ApiDefinition("serviceName2", "name2")
      ApmConnectorMock.FetchApplicationWithSubscriptionData.thenReturn(
        ApiIdentifier("context1", "v1"), ApiIdentifier("context2", "v2"), ApiIdentifier("context3", "v3")
      )
      ApmConnectorMock.FetchSubscribableApisForApplication.thenReturn(Map(
        ("context1" -> apiDefinition1),
        ("context2" -> apiDefinition2),
        ("context4" -> ApiDefinition("serviceName4", "name4"))
      ))
      val result = await(service.fetchSubscriptionsByApplicationId(applicationId))
      result shouldBe Set(apiDefinition1, apiDefinition2)
    }

    "return empty set if no application is found" in new Setup {
      ApmConnectorMock.FetchApplicationWithSubscriptionData.thenReturnNothing
      val result = await(service.fetchSubscriptionsByApplicationId(applicationId))
      result shouldBe Set()
    }

    "return empty set if no subscribable apis are found" in new Setup {
      ApmConnectorMock.FetchApplicationWithSubscriptionData.thenReturn(
        ApiIdentifier("context1", "v1"), ApiIdentifier("context2", "v2"), ApiIdentifier("context3", "v3")
      )
      ApmConnectorMock.FetchSubscribableApisForApplication.thenReturnNothing
      val result = await(service.fetchSubscriptionsByApplicationId(applicationId))
      result shouldBe Set()
    }
  }
}
