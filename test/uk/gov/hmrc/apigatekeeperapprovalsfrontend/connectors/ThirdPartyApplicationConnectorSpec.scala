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

package uk.gov.hmrc.apigatekeeperapprovalsfrontend.connectors

import org.scalatestplus.play.guice.GuiceOneAppPerSuite

import scala.concurrent.ExecutionContext.Implicits.global
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.ApplicationId
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.utils.AsyncHmrcSpec
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}
import scala.collection.Seq
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.Application
import play.api.Mode

class ThirdPartyApplicationConnectorSpec extends AsyncHmrcSpec with GuiceOneAppPerSuite {

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .overrides(bind[ConnectorMetrics].to[NoopConnectorMetrics])
      .in(Mode.Test)
      .build()


  trait Setup {
    implicit val hc: HeaderCarrier = HeaderCarrier()
    val httpClient = mock[HttpClient]
    val urlBase = "http://example.com"
    val appId = ApplicationId.random

    val connector = new ThirdPartyApplicationConnector(httpClient, ThirdPartyApplicationConnector.Config(urlBase))

    def assertHttpClientWasCalledWithUrl(expectedUrl: String) = 
      verify(httpClient).GET(eqTo(expectedUrl), *[Seq[(String, String)]], *[Seq[(String, String)]])(*,*,*)
  }

  "fetchApplicationById" should {
    "call the correct endpoint" in new Setup {
      connector.fetchApplicationById(appId)

      assertHttpClientWasCalledWithUrl(s"$urlBase/application/${appId.value}")      
    }
  }

  // "fetchLatestMarkedSubmission" should {
  //   "call the correct endpoint" in new Setup {
  //     connector.fetchLatestMarkedSubmission(appId)

  //     assertHttpClientWasCalledWithUrl(s"$urlBase/submissions/marked/application/${appId.value}")      
  //   }
  // }
}
