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

import java.time.Instant

import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

import play.api.http.Status._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.{Application => PlayApplication, Configuration, Mode}
import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.ApplicationWithCollaborators
import uk.gov.hmrc.apiplatform.modules.submissions.connectors.SubmissionsConnector
import uk.gov.hmrc.apiplatform.modules.submissions.connectors.SubmissionsConnector.TouUpliftRequest
import uk.gov.hmrc.apiplatform.modules.submissions.domain.models.TermsOfUseInvitationState.EMAIL_SENT
import uk.gov.hmrc.apiplatform.modules.submissions.domain.models.{Submission, TermsOfUseInvitation, TermsOfUseInvitationSuccessful, TermsOfUseInvitationWithApplication}
import uk.gov.hmrc.apiplatform.modules.submissions.{MarkedSubmissionsTestData, ProgressTestDataHelper}

import uk.gov.hmrc.apigatekeeperapprovalsfrontend.utils.WireMockExtensions

class SubmissionConnectorISpec extends BaseConnectorIntegrationISpec with GuiceOneAppPerSuite with WireMockExtensions with MarkedSubmissionsTestData {

  import Submission._

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

  trait Setup extends ProgressTestDataHelper {
    val connector = app.injector.instanceOf[SubmissionsConnector]

    val extSubmission              = aSubmission.withIncompleteProgress()
    val markSubmission             = markedSubmission
    val requestedBy                = "bob@blockbusters.com"
    val reason                     = "reason"
    val escalatedTo                = "Mr Edmund Blackadder"
    val invitation                 = TermsOfUseInvitation(applicationId, Instant.now, Instant.now, Instant.now, None, EMAIL_SENT)
    val invitationWithApp          = TermsOfUseInvitationWithApplication(applicationId, Instant.now, Instant.now, Instant.now, None, EMAIL_SENT, "Petes app")
    implicit val hc: HeaderCarrier = HeaderCarrier()
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

  "fetch terms of use invitations" should {
    val url = "/terms-of-use"

    "return an invitation" in new Setup {
      stubFor(
        get(urlEqualTo(url))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withJsonBody(List(invitation))
          )
      )

      val result = await(connector.fetchTermsOfUseInvitations())

      result.size shouldBe 1
      result.head.applicationId shouldBe invitation.applicationId
    }

    "return empty list if no invitations found" in new Setup {
      stubFor(
        get(urlEqualTo(url))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withJsonBody(List[TermsOfUseInvitation]())
          )
      )

      val result = await(connector.fetchTermsOfUseInvitations())

      result.size shouldBe 0
    }
  }

  "search terms of use invitations" should {
    val url = "/terms-of-use/search?status=EMAIL_SENT"

    "return an invitation" in new Setup {
      stubFor(
        get(urlEqualTo(url))
          .withQueryParam("status", equalTo("EMAIL_SENT"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withJsonBody(List(invitationWithApp))
          )
      )

      val result = await(connector.searchTermsOfUseInvitations(Seq("status" -> "EMAIL_SENT")))

      result.size shouldBe 1
      result.head.applicationId shouldBe invitation.applicationId
    }

    "return empty list if no invitations found" in new Setup {
      stubFor(
        get(urlEqualTo(url))
          .withQueryParam("status", equalTo("EMAIL_SENT"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withJsonBody(List[TermsOfUseInvitationWithApplication]())
          )
      )

      val result = await(connector.searchTermsOfUseInvitations(Seq("status" -> "EMAIL_SENT")))

      result.size shouldBe 0
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
              .withJsonBody(standardApp.withState(appStateTesting))
          )
      )

      val result = await(connector.grantWithWarningsForTouUplift(applicationId, requestedBy, reason))

      result match {
        case Right(app: ApplicationWithCollaborators) => app.id shouldBe applicationId
        case _                                        => fail("Expected an Application, got something else.")
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
              .withJsonBody(standardApp.withState(appStateTesting))
          )
      )

      val result = await(connector.declineForTouUplift(applicationId, requestedBy, reason))

      result match {
        case Right(app: ApplicationWithCollaborators) => app.id shouldBe applicationId
        case _                                        => fail("Expected an Application, got something else.")
      }
    }
  }

  "reset application for ToU" should {
    val url = s"/approvals/application/${applicationId.value}/reset-tou"

    "return an application on success" in new Setup {
      stubFor(
        post(urlEqualTo(url))
          .withJsonRequestBody(TouUpliftRequest(requestedBy, reason))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withJsonBody(standardApp.withState(appStateTesting))
          )
      )

      val result = await(connector.resetForTouUplift(applicationId, requestedBy, reason))

      result match {
        case Right(app: ApplicationWithCollaborators) => app.id shouldBe applicationId
        case _                                        => fail("Expected an Application, got something else.")
      }
    }
  }

  "delete submission for ToU" should {
    val url = s"/approvals/application/${applicationId.value}/delete-tou"

    "return an application on success" in new Setup {
      stubFor(
        post(urlEqualTo(url))
          .withJsonRequestBody(SubmissionsConnector.TouDeleteRequest(requestedBy))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withJsonBody(standardApp.withState(appStateTesting))
          )
      )

      val result = await(connector.deleteTouUplift(applicationId, requestedBy))

      result match {
        case Right(app: ApplicationWithCollaborators) => app.id shouldBe applicationId
        case _                                        => fail("Expected an Application, got something else.")
      }
    }
  }
}
