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

import scala.concurrent.ExecutionContext.Implicits.global

import org.scalatestplus.play.guice.GuiceOneAppPerSuite

import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.{Application, Mode}
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}

import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.ApplicationWithCollaboratorsFixtures

import uk.gov.hmrc.apigatekeeperapprovalsfrontend.utils.AsyncHmrcSpec

class ThirdPartyApplicationConnectorSpec extends AsyncHmrcSpec with GuiceOneAppPerSuite with ApplicationWithCollaboratorsFixtures {

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .overrides(bind[ConnectorMetrics].to[NoopConnectorMetrics])
      .in(Mode.Test)
      .configure(
        "metrics.jvm"     -> false,
        "metrics.enabled" -> false
      )
      .build()

  trait Setup extends HttpClientMockModule {
    implicit val hc: HeaderCarrier = HeaderCarrier()

    val urlBase = "http://example.com"
    val appId   = applicationIdOne
    val app     = standardApp.withState(appStateTesting)

    val connector = new ThirdPartyApplicationConnector(HttpClientMock.aMock, ThirdPartyApplicationConnector.Config(urlBase), new NoopConnectorMetrics())
  }

  "fetchApplicationById" should {
    "call the correct endpoint and return the application" in new Setup {
      HttpClientMock.Get.thenReturn(Some(app))

      val result = await(connector.fetchApplicationById(appId))

      result shouldBe Some(app)
      HttpClientMock.Get.verifyUrl(url"$urlBase/application/$appId")
    }

    "return None if the application was not found" in new Setup {
      HttpClientMock.Get.thenReturn(None)

      val result = await(connector.fetchApplicationById(appId))

      result shouldBe None
    }
  }
}
