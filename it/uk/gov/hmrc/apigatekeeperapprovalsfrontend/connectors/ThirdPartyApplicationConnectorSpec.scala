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

import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.http.Status._
import play.api.Configuration
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.{Application => PlayApplication}

import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.ApplicationId
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.utils.WireMockExtensions
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.Application
import uk.gov.hmrc.http.HeaderCarrier

class ThirdPartyApplicationConnectorSpec extends BaseConnectorIntegrationSpec with GuiceOneAppPerSuite with WireMockExtensions {
  private val appConfig = Configuration(
    "microservice.services.third-party-application.port" -> stubPort,
    "microservice.services.third-party-application.use-proxy" -> false,
    "microservice.services.third-party-application.api-key" -> "",
  )

  override def fakeApplication(): PlayApplication =
    GuiceApplicationBuilder()
      .configure(appConfig)
      .build()

  private val applicationId = ApplicationId("applicationId")

  trait Setup {
    val connector = app.injector.instanceOf[ThirdPartyApplicationConnector]

    def applicationResponse(appId: ApplicationId, appName: String = "My Application") = new Application(
      appId,
      appName
    )

    implicit val hc = HeaderCarrier()
  }
  
  "fetch application by id" should {
    val url = s"/application/${applicationId.value}"
    val appName = "app name"

    "return an application" in new Setup {
      stubFor(
        get(urlEqualTo(url))
        .willReturn(
            aResponse()
            .withStatus(OK)
            .withJsonBody(applicationResponse(applicationId, appName))
        )
      )
      
      val result = await(connector.fetchApplicationById(applicationId))

      result shouldBe defined
      result.get.id shouldBe applicationId
      result.get.name shouldBe appName
    }

    "return None if the application cannot be found" in new Setup {
      stubFor(
        get(urlEqualTo(url))
        .willReturn(
            aResponse()
            .withStatus(NOT_FOUND)
        )
      )

      val result = await(connector.fetchApplicationById(applicationId))

      result shouldBe empty
    }
  }
}
