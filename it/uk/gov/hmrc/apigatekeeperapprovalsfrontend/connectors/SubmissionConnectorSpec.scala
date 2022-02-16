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
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.utils.{ApplicationTestData, WireMockExtensions}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import uk.gov.hmrc.http.HeaderCarrier
import play.api.Mode
import uk.gov.hmrc.apiplatform.modules.submissions.connectors.SubmissionsConnector
import uk.gov.hmrc.apiplatform.modules.submissions.connectors.SubmissionsConnector.GrantedRequest
import uk.gov.hmrc.apiplatform.modules.submissions.connectors.SubmissionsConnector.DeclinedRequest
import uk.gov.hmrc.apiplatform.modules.submissions.MarkedSubmissionsTestData
import uk.gov.hmrc.apiplatform.modules.submissions.domain.services.SubmissionsFrontendJsonFormatters

class SubmissionConnectorSpec extends BaseConnectorIntegrationSpec with GuiceOneAppPerSuite with WireMockExtensions with MarkedSubmissionsTestData with ApplicationTestData{
  private val appConfig = Configuration(
    "microservice.services.third-party-application.port" -> stubPort,
    "microservice.services.third-party-application.use-proxy" -> false,
    "microservice.services.third-party-application.api-key" -> "",
    "metrics.jvm"     -> false,
    "metrics.enabled" -> false
  )

  override def fakeApplication(): PlayApplication =
    GuiceApplicationBuilder()
      .configure(appConfig)
      .in(Mode.Test)
      .build()

  trait Setup extends SubmissionsFrontendJsonFormatters {
    val connector = app.injector.instanceOf[SubmissionsConnector]

    val extSubmission = extendedSubmission
    val markSubmission = markedSubmission
    val requestedBy = "bob@blockbusters.com"
    val reason = "reason"
    implicit val hc = HeaderCarrier()
  }
  
  "fetch latest submission by id" should {
    val url = s"/submissions/application/${applicationId.value}"

    "return a submission" in new Setup {
      stubFor(
        get(urlEqualTo(url))
        .willReturn(
            aResponse()
            .withStatus(OK)
            .withJsonBody(extSubmission)
        )
      )
      
      val result = await(connector.fetchLatestSubmission(applicationId))

      result shouldBe defined
      result.get.submission.id shouldBe extSubmission.submission.id
    }

    "return None if the submission cannot be found" in new Setup {
      stubFor(
        get(urlEqualTo(url))
        .willReturn(
            aResponse()
            .withStatus(NOT_FOUND)
        )
      )

      val result = await(connector.fetchLatestSubmission(applicationId))

      result shouldBe empty
    }
  }

  "fetch latest marked submission by id" should {
    val url = s"/submissions/marked/application/${applicationId.value}"

    "return a marked submission" in new Setup {
      stubFor(
        get(urlEqualTo(url))
        .willReturn(
            aResponse()
            .withStatus(OK)
            .withJsonBody(markSubmission)
        )
      )
      
      val result = await(connector.fetchLatestMarkedSubmission(applicationId))

      result shouldBe defined
      result.get.submission.id shouldBe extSubmission.submission.id
    }

    "return None if the submission cannot be found" in new Setup {
      stubFor(
        get(urlEqualTo(url))
        .willReturn(
            aResponse()
            .withStatus(NOT_FOUND)
        )
      )

      val result = await(connector.fetchLatestMarkedSubmission(applicationId))

      result shouldBe empty
    }
  }

  "grant application" should {
    val url = s"/approvals/application/${applicationId.value}/grant"

    "return an application on success" in new Setup {
      stubFor(
        post(urlEqualTo(url))
        .withJsonRequestBody(GrantedRequest(requestedBy))
        .willReturn(
            aResponse()
            .withStatus(OK)
            .withJsonBody(anApplication(id = applicationId))
        )
      )
      
      val result = await(connector.grant(applicationId, requestedBy))

      result shouldBe 'Right
      result.right.get.id shouldBe applicationId
    }
  }

  "decline application" should {
    val url = s"/approvals/application/${applicationId.value}/decline"

    "return an application on success" in new Setup {
      stubFor(
        post(urlEqualTo(url))
        .withJsonRequestBody(DeclinedRequest(requestedBy, reason))
        .willReturn(
            aResponse()
            .withStatus(OK)
            .withJsonBody(anApplication(id = applicationId))
        )
      )
      
      val result = await(connector.decline(applicationId, requestedBy, reason))

      result shouldBe 'Right
      result.right.get.id shouldBe applicationId
    }
  }
}
