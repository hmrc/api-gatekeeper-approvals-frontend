/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers

import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.inject.guice.GuiceApplicationBuilder

import scala.concurrent.ExecutionContext.Implicits.global
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.config.AppConfig
import uk.gov.hmrc.modules.stride.config.StrideAuthConfig
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.modules.stride.connectors.mocks.ApplicationActionServiceMockModule
import uk.gov.hmrc.modules.stride.connectors.mocks.AuthConnectorMockModule
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.config.ErrorHandler
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.Application
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.ApplicationId
import uk.gov.hmrc.utils.AsyncHmrcSpec

class ApplicationControllerSpec extends AsyncHmrcSpec with GuiceOneAppPerSuite {
  override def fakeApplication() =
    new GuiceApplicationBuilder()
      .configure(
        "metrics.jvm"     -> false,
        "metrics.enabled" -> false
      )
      .build()

  private val fakeRequest = FakeRequest("GET", "/")

  trait Setup extends AuthConnectorMockModule with ApplicationActionServiceMockModule {
    implicit val appConfig = app.injector.instanceOf[AppConfig]

    val strideAuthConfig = app.injector.instanceOf[StrideAuthConfig]
    val forbiddenHandler = app.injector.instanceOf[HandleForbiddenWithView]
    val mcc = app.injector.instanceOf[MessagesControllerComponents]
    val errorHandler = app.injector.instanceOf[ErrorHandler]

    val controller = new ApplicationController(
      strideAuthConfig,
      AuthConnectorMock.aMock,
      forbiddenHandler,
      mcc,
      errorHandler,
      ApplicationActionServiceMock.aMock
    )
  }

  "GET /" should {
    "return 200" in new Setup {
      val appId = ApplicationId.random
      val application = Application(appId, "app name")

      val fakeRequest = FakeRequest()

      AuthConnectorMock.Authorise.thenReturn()
      ApplicationActionServiceMock.Process.thenReturn(application)
      val result = controller.getApplication(appId)(fakeRequest)
      status(result) shouldBe Status.OK
    }

    "return 403 for InsufficientEnrolments" in new Setup {
      AuthConnectorMock.Authorise.thenReturnInsufficientEnrolments()
      val appId = ApplicationId.random
      val result = controller.getApplication(appId)(fakeRequest)
      status(result) shouldBe Status.FORBIDDEN
    }
    
    "return 303 for SessionRecordNotFound" in new Setup {
      AuthConnectorMock.Authorise.thenReturnSessionRecordNotFound()
      val appId = ApplicationId.random
      val result = controller.getApplication(appId)(fakeRequest)
      status(result) shouldBe Status.SEE_OTHER
    }  
  }
}
