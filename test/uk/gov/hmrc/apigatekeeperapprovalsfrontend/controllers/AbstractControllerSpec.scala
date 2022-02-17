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

package uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers

import org.scalatestplus.play.guice.GuiceOneAppPerSuite

import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.apiplatform.modules.stride.config.StrideAuthConfig
import uk.gov.hmrc.apiplatform.modules.stride.connectors.mocks.{ApplicationActionServiceMockModule, AuthConnectorMockModule}
import uk.gov.hmrc.apiplatform.modules.submissions.services.{SubmissionReviewServiceMockModule, SubmissionServiceMockModule}

import uk.gov.hmrc.apigatekeeperapprovalsfrontend.config.{ErrorHandler, GatekeeperConfig}
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.utils.{AsyncHmrcSpec, WithCSRFAddToken}
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.ApplicationId
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.Application
import play.api.test.FakeRequest
import uk.gov.hmrc.apiplatform.modules.submissions.SubmissionsTestData

class AbstractControllerSpec extends AsyncHmrcSpec with GuiceOneAppPerSuite with WithCSRFAddToken {
  override def fakeApplication() =
    new GuiceApplicationBuilder()
      .configure(
        "metrics.jvm"     -> false,
        "metrics.enabled" -> false
      )
      .build()

  trait AbstractSetup 
      extends AuthConnectorMockModule
      with ApplicationActionServiceMockModule 
      with SubmissionServiceMockModule
      with SubmissionReviewServiceMockModule {

    val config = app.injector.instanceOf[GatekeeperConfig]
    val strideAuthConfig = app.injector.instanceOf[StrideAuthConfig]
    val forbiddenHandler = app.injector.instanceOf[HandleForbiddenWithView]
    val mcc = app.injector.instanceOf[MessagesControllerComponents]
    val errorHandler = app.injector.instanceOf[ErrorHandler]

    val appId = ApplicationId.random
    lazy val application = Application(appId, "app name")
    
    val fakeRequest = FakeRequest().withCSRFToken

    val fakeSubmitCheckedRequest = FakeRequest().withCSRFToken.withFormUrlEncodedBody("submit-action" -> "checked")
    val fakeSubmitComebackLaterRequest = FakeRequest().withCSRFToken.withFormUrlEncodedBody("submit-action" -> "come-back-later")
    val brokenRequest = FakeRequest().withCSRFToken.withFormUrlEncodedBody("submit-action" -> "bobbins")
  }
}