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

package uk.gov.hmrc.apigatekeeperapprovalsfrontend.connectors

import scala.collection.immutable.Seq
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future.successful

import org.scalatestplus.play.guice.GuiceOneAppPerSuite

import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.{Application, Mode}
import uk.gov.hmrc.apiplatform.modules.apis.domain.models.MappedApiDefinitions
import uk.gov.hmrc.apiplatform.modules.common.domain.models.ApplicationId
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import uk.gov.hmrc.apigatekeeperapprovalsfrontend.utils.AsyncHmrcSpec

class ApmConnectorSpec extends AsyncHmrcSpec with GuiceOneAppPerSuite {

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .overrides(bind[ConnectorMetrics].to[NoopConnectorMetrics])
      .in(Mode.Test)
      .configure(
        "metrics.jvm"     -> false,
        "metrics.enabled" -> false
      )
      .build()

  trait Setup {
    implicit val hc: HeaderCarrier = HeaderCarrier()
    val httpClient                 = mock[HttpClient]
    val urlBase                    = "http://example.com"
    val appId                      = ApplicationId.random

    val connector = new ApmConnector(httpClient, ApmConnector.Config(urlBase), new NoopConnectorMetrics())

    def assertHttpClientWasCalledWithUrl(expectedUrl: String, expectedParams: Seq[(String, String)] = Seq()) =
      verify(httpClient).GET(eqTo(expectedUrl), eqTo(expectedParams), *[Seq[(String, String)]])(*, *, *)
  }

  "the Apm Connector" should {
    "call the correct endpoint for fetchLinkedSubordinateApplicationById" in new Setup {
      connector.fetchLinkedSubordinateApplicationById(appId)
      assertHttpClientWasCalledWithUrl(s"$urlBase/applications/${appId.value}/linked-subordinate")
    }

    "call the correct endpoint for fetchSubscribableApisForApplication" in new Setup {
      when(httpClient.GET[MappedApiDefinitions](*, *, *)(*, *, *)).thenReturn(successful(MappedApiDefinitions(Map())))
      connector.fetchSubscribableApisForApplication(appId)
      assertHttpClientWasCalledWithUrl(s"$urlBase/api-definitions", Seq("applicationId" -> appId.toString()))
    }

    "call the correct endpoint for fetchApplicationWithSubscriptionData" in new Setup {
      connector.fetchApplicationWithSubscriptionData(appId)
      assertHttpClientWasCalledWithUrl(s"$urlBase/applications/${appId.value}")
    }
  }

}
