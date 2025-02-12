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

package uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers

import java.time.format.DateTimeFormatter
import java.time.{Instant, ZoneId}

import org.scalatestplus.play.guice.GuiceOneAppPerSuite

import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.MessagesControllerComponents
import play.api.test.FakeRequest

import uk.gov.hmrc.apiplatform.modules.applications.access.domain.models.SellResellOrDistribute
import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.{ApplicationWithCollaboratorsFixtures, Collaborator}
import uk.gov.hmrc.apiplatform.modules.common.domain.models.{LaxEmailAddress, UserId}
import uk.gov.hmrc.apiplatform.modules.gkauth.config.StrideAuthConfig
import uk.gov.hmrc.apiplatform.modules.gkauth.services.ApplicationActionServiceMockModule
import uk.gov.hmrc.apiplatform.modules.submissions.SubmissionsTestData
import uk.gov.hmrc.apiplatform.modules.submissions.services.{SubmissionReviewServiceMockModule, SubmissionServiceMockModule}

import uk.gov.hmrc.apigatekeeperapprovalsfrontend.config.{ErrorHandler, GatekeeperConfig}
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.utils.{AsyncHmrcSpec, WithCSRFAddToken}

class AbstractControllerSpec
    extends AsyncHmrcSpec
    with GuiceOneAppPerSuite
    with WithCSRFAddToken
    with ApplicationWithCollaboratorsFixtures
    with SubmissionsTestData {

  override def fakeApplication() =
    new GuiceApplicationBuilder()
      .configure(
        "metrics.jvm"     -> false,
        "metrics.enabled" -> false
      )
      .build()

  trait AbstractSetup
      extends ApplicationActionServiceMockModule
      with SubmissionServiceMockModule
      with SubmissionReviewServiceMockModule {

    val config           = app.injector.instanceOf[GatekeeperConfig]
    val strideAuthConfig = app.injector.instanceOf[StrideAuthConfig]
    val forbiddenHandler = app.injector.instanceOf[HandleForbiddenWithView]
    val mcc              = app.injector.instanceOf[MessagesControllerComponents]
    val errorHandler     = app.injector.instanceOf[ErrorHandler]

    val application = standardApp.withCollaborators(Collaborator(LaxEmailAddress("pete@example.com"), Collaborator.Roles.ADMINISTRATOR, UserId.random))

    val inHouseApplication = application.modifyStdAccess(_.copy(sellResellOrDistribute = Some(SellResellOrDistribute("No"))))
    val fakeRequest        = FakeRequest().withCSRFToken

    val fakeSubmitCheckedRequest       = fakeRequest.withFormUrlEncodedBody("submit-action" -> "checked")
    val fakeSubmitComebackLaterRequest = fakeRequest.withFormUrlEncodedBody("submit-action" -> "come-back-later")
    val brokenRequest                  = fakeRequest.withFormUrlEncodedBody("submit-action" -> "bobbins")

    def formatDMY(i: Instant): String = DateTimeFormatter.ofPattern("dd MMMM yyyy").withZone(ZoneId.systemDefault()).format(i)
  }
}
