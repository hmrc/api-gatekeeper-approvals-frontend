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

import play.api.i18n.MessagesApi
import play.api.mvc.MessagesRequest
import play.api.test.FakeRequest
import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.ApplicationWithCollaboratorsFixtures
import uk.gov.hmrc.apiplatform.modules.gkauth.domain.models.{GatekeeperRoles, LoggedInRequest}
import uk.gov.hmrc.apiplatform.modules.gkauth.services.ApplicationServiceMockModule

import uk.gov.hmrc.apigatekeeperapprovalsfrontend.utils.AsyncHmrcSpec

class ApplicationActionServiceSpec extends AsyncHmrcSpec with ApplicationWithCollaboratorsFixtures {

  trait Setup extends ApplicationServiceMockModule {
    implicit val hc: HeaderCarrier = HeaderCarrier()
    val applicationId              = applicationIdOne
    val service                    = new ApplicationActionService(ApplicationServiceMock.aMock)
    val loggedInRequest            = new LoggedInRequest(Some("name"), GatekeeperRoles.READ_ONLY, new MessagesRequest(FakeRequest("GET", "/"), mock[MessagesApi]))
  }

  "process" should {
    "return an ApplicationRequest if the application exists" in new Setup {
      ApplicationServiceMock.FetchByApplicationId.thenReturn(applicationId)
      val result = await(service.process(applicationId, loggedInRequest).value)
      result.value.application.id shouldBe applicationId
    }
    "return nothing" in new Setup {
      ApplicationServiceMock.FetchByApplicationId.thenNotFound()
      val result = await(service.process(applicationId, loggedInRequest).value)
      result shouldBe None
    }
  }

}
