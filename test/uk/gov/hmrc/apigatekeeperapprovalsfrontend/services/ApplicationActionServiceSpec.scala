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

import scala.concurrent.ExecutionContext.Implicits.global
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.ApplicationId
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.utils.AsyncHmrcSpec
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.modules.stride.connectors.mocks.ApplicationServiceMockModule
import uk.gov.hmrc.modules.stride.controllers.models.LoggedInRequest
import uk.gov.hmrc.auth.core.Enrolments
import play.api.mvc.MessagesRequest
import play.api.test.FakeRequest
import play.api.i18n.MessagesApi

class ApplicationActionServiceSpec extends AsyncHmrcSpec {

  trait Setup extends ApplicationServiceMockModule {
    implicit val hc: HeaderCarrier = HeaderCarrier()
    val applicationId = ApplicationId.random
    val service = new ApplicationActionService(ApplicationServiceMock.aMock)
    val loggedInRequest = new LoggedInRequest(Some("name"), Enrolments(Set.empty), new MessagesRequest(FakeRequest("GET", "/"), mock[MessagesApi]))
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
