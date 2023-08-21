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

import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.http.Status._
import play.api.Configuration
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.{Application => PlayApplication}
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.utils.{ApplicationTestData, WireMockExtensions}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import uk.gov.hmrc.http.HeaderCarrier
import play.api.Mode
import java.time.Instant
import uk.gov.hmrc.apiplatform.modules.submissions.connectors.SubmissionsConnector
import uk.gov.hmrc.apiplatform.modules.submissions.connectors.SubmissionsConnector.{GrantedRequest, TouGrantedRequest, TouUpliftRequest}
import uk.gov.hmrc.apiplatform.modules.submissions.MarkedSubmissionsTestData
import uk.gov.hmrc.apiplatform.modules.submissions.domain.services.SubmissionsJsonFormatters
import uk.gov.hmrc.apiplatform.modules.submissions.ProgressTestDataHelper
import uk.gov.hmrc.apiplatform.modules.submissions.domain.models.{TermsOfUseInvitation, TermsOfUseInvitationSuccessful}
import uk.gov.hmrc.apiplatform.modules.submissions.domain.models.TermsOfUseInvitationState.EMAIL_SENT
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.Application

class SubmissionConnectorISpec extends BaseConnectorIntegrationISpec with GuiceOneAppPerSuite with WireMockExtensions with MarkedSubmissionsTestData with ApplicationTestData {

  private val appConfig = Configuration(
    "microservice.services.third-party-application.port"      -> stubPort,
    "microservice.services.third-party-application.use-proxy" -> false,
    "microservice.services.third-party-application.api-key"   -> "",
    "metrics.jvm"                                             -> false,
    "metrics.enabled"                                         -> false
  )

  override def fakeApplication(): PlayApplication =
    GuiceApplicationBuilder()
      .configure(appConfig)
      .in(Mode.Test)
      .build()

  trait Setup extends SubmissionsJsonFormatters with ProgressTestDataHelper {
    val connector = app.injector.instanceOf[SubmissionsConnector]

    val extSubmission  = aSubmission.withIncompleteProgress()
    val markSubmission = markedSubmission
    val requestedBy    = "bob@blockbusters.com"
    val reason         = "reason"
    val escalatedTo    = "Mr Edmund Blackadder"
    val invitation     = TermsOfUseInvitation(applicationId, Instant.now, Instant.now, Instant.now, None, EMAIL_SENT)
    implicit val hc    = HeaderCarrier()
  }

  "fetch latest submission by id" should {
    val url = s"/submissions/application/${applicationId.value}"

    "return a submission" in new Setup {
      stubFor(
        get(urlEqualTo(url))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withJsonBody(aSubmission)
          )
      )

      val result = await(connector.fetchLatestSubmission(applicationId))

      result shouldBe defined
      result.get.id shouldBe extSubmission.submission.id
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
          .withJsonRequestBody(GrantedRequest(requestedBy, None))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withJsonBody(anApplication(id = applicationId))
          )
      )

      await(connector.grant(applicationId, requestedBy)) match {
        case Right(app: Application) => app.id shouldBe applicationId
        case _                       => fail()
      }

    }
  }

  "invite application for terms of use" should {
    val url = s"/terms-of-use/application/${applicationId.value}"

    "return TermsOfUseInvitationSuccessful on success" in new Setup {
      stubFor(
        post(urlEqualTo(url))
          .willReturn(
            aResponse()
              .withStatus(OK)
          )
      )

      await(connector.termsOfUseInvite(applicationId)) match {
        case Right(_: TermsOfUseInvitationSuccessful) => succeed
        case _                                        => fail()
      }

    }
  }

  "fetch terms of use invitation by app id" should {
    val url = s"/terms-of-use/application/${applicationId.value}"

    "return an invitation" in new Setup {
      stubFor(
        get(urlEqualTo(url))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withJsonBody(invitation)
          )
      )

      val result = await(connector.fetchTermsOfUseInvitation(applicationId))

      result shouldBe defined
      result.get.applicationId shouldBe invitation.applicationId
    }

    "return None if the invitation cannot be found" in new Setup {
      stubFor(
        get(urlEqualTo(url))
          .willReturn(
            aResponse()
              .withStatus(NOT_FOUND)
          )
      )

      val result = await(connector.fetchTermsOfUseInvitation(applicationId))

      result shouldBe empty
    }
  }

  "grant application for ToU" should {
    val url = s"/approvals/application/${applicationId.value}/grant-tou"

    "return an application on success" in new Setup {
      stubFor(
        post(urlEqualTo(url))
          .withJsonRequestBody(TouGrantedRequest(requestedBy, reason, Some(escalatedTo)))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withJsonBody(anApplication(id = applicationId))
          )
      )

      await(connector.grantForTouUplift(applicationId, requestedBy, reason, Some(escalatedTo))) match {
        case Right(app: Application) => app.id shouldBe applicationId
        case _                       => fail()
      }

    }
  }

  "grant with warnings application for ToU" should {
    val url = s"/approvals/application/${applicationId.value}/grant-with-warn-tou"

    "return an application on success" in new Setup {
      stubFor(
        post(urlEqualTo(url))
          .withJsonRequestBody(TouUpliftRequest(requestedBy, reason))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withJsonBody(anApplication(id = applicationId))
          )
      )

      val result = await(connector.grantWithWarningsForTouUplift(applicationId, requestedBy, reason))

      result match {
        case Right(app: Application) => app.id shouldBe applicationId
        case _                       => fail("Expected an Application, got something else.")
      }
    }
  }

  "decline application for ToU" should {
    val url = s"/approvals/application/${applicationId.value}/decline-tou"

    "return an application on success" in new Setup {
      stubFor(
        post(urlEqualTo(url))
          .withJsonRequestBody(TouUpliftRequest(requestedBy, reason))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withJsonBody(anApplication(id = applicationId))
          )
      )

      val result = await(connector.declineForTouUplift(applicationId, requestedBy, reason))

      result match {
        case Right(app: Application) => app.id shouldBe applicationId
        case _                       => fail("Expected an Application, got something else.")
      }
    }
  }
}
