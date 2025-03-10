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
import scala.concurrent.ExecutionContext.Implicits.global

import cats.data.NonEmptyList
import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

import play.api.test.Helpers._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}

import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.{ApplicationState, ApplicationWithCollaborators, Collaborators, CoreApplicationData, State}
import uk.gov.hmrc.apiplatform.modules.commands.applications.domain.models.{ApplicationCommands, CommandFailure, CommandFailures, DispatchRequest, DispatchSuccessResult}
import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress.StringSyntax
import uk.gov.hmrc.apiplatform.modules.common.domain.models.{Actors, UserId, _}
import uk.gov.hmrc.apiplatform.modules.common.domain.services.NonEmptyListFormatters._
import uk.gov.hmrc.apiplatform.modules.common.utils.FixedClock

import uk.gov.hmrc.apigatekeeperapprovalsfrontend.utils.{AsyncHmrcSpec, WireMockSugar}

class ApplicationCommandConnectorSpec
    extends AsyncHmrcSpec
    with WireMockSugar
    with GuiceOneAppPerSuite
    with FixedClock {

  def anApplicationResponse(
      createdOn: Instant = instant,
      lastAccess: Instant = instant,
      state: ApplicationState = ApplicationState(State.TESTING, None, None, None, instant)
    ): ApplicationWithCollaborators = {
    ApplicationWithCollaborators(
      details = CoreApplicationData.Standard.one.copy(
        createdOn = createdOn,
        lastAccess = Some(lastAccess),
        state = state
      ),
      collaborators = Set.empty
    )
  }

  val apiVersion1   = ApiVersionNbr.random
  val applicationId = ApplicationId.random
  val administrator = Collaborators.Administrator(UserId.random, "sample@example.com".toLaxEmail)
  val developer     = Collaborators.Developer(UserId.random, "someone@example.com".toLaxEmail)

  val authToken                  = "Bearer Token"
  implicit val hc: HeaderCarrier = HeaderCarrier().withExtraHeaders(("Authorization", authToken))

  val emailAddressToRemove = "toRemove@example.com".toLaxEmail
  val gatekeeperUserName   = "maxpower"
  val collaborator         = Collaborators.Administrator(UserId.random, emailAddressToRemove)
  val command              = ApplicationCommands.RemoveCollaborator(Actors.GatekeeperUser(gatekeeperUserName), collaborator, instant)

  val adminsToEmail = Set("admin1@example.com", "admin2@example.com").map(_.toLaxEmail)
  val url           = s"/applications/${applicationId}/dispatch"

  class Setup(proxyEnabled: Boolean = false) {

    val httpClient = app.injector.instanceOf[HttpClientV2]

    val config    = ApmConnector.Config(wireMockUrl)
    val connector = new ApplicationCommandConnector(httpClient, config) {}
  }

  "dispatch" should {

    "send a correct command" in new Setup {
      stubFor(
        patch(urlPathEqualTo(url))
          .withJsonRequestBody(DispatchRequest(command, adminsToEmail))
          .willReturn(
            aResponse()
              .withJsonBody(DispatchSuccessResult(anApplicationResponse()))
              .withStatus(OK)
          )
      )
      await(connector.dispatch(applicationId, command, adminsToEmail)).isRight shouldBe true
    }

    "send a correct command and handle command failure" in new Setup {
      val failures = NonEmptyList.one[CommandFailure](CommandFailures.ApplicationNotFound)
      stubFor(
        patch(urlPathEqualTo(url))
          .withJsonRequestBody(DispatchRequest(command, adminsToEmail))
          .willReturn(
            aResponse()
              .withJsonBody(failures)
              .withStatus(BAD_REQUEST)
          )
      )
      await(connector.dispatch(applicationId, command, adminsToEmail)).left.value shouldBe failures
    }

    "send a correct command and handle general failure" in new Setup {
      stubFor(
        patch(urlPathEqualTo(url))
          .withJsonRequestBody(DispatchRequest(command, adminsToEmail))
          .willReturn(
            aResponse()
              .withStatus(IM_A_TEAPOT)
          )
      )
      intercept[InternalServerException] {
        await(connector.dispatch(applicationId, command, adminsToEmail))
      }.message should include(IM_A_TEAPOT.toString)
    }
  }
}
