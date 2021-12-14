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

import uk.gov.hmrc.apigatekeeperapprovalsfrontend.views.html.HelloWorldPage
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import javax.inject.{Inject, Singleton}
import scala.concurrent.Future
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.utils.GatekeeperAuthWrapper
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.model.GatekeeperRole
import scala.concurrent.ExecutionContext
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.config.AppConfig
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.connectors.AuthConnector
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.views.html.ForbiddenView

@Singleton
class HelloWorldController @Inject()(
  val authConnector: AuthConnector,
  val forbiddenView: ForbiddenView,
  mcc: MessagesControllerComponents,
  helloWorldPage: HelloWorldPage
)(implicit val appConfig: AppConfig, val ec: ExecutionContext) extends FrontendController(mcc) with GatekeeperAuthWrapper  {

  val helloWorld: Action[AnyContent] = requiresAtLeast(GatekeeperRole.USER) { implicit request =>
    Future.successful(Ok(helloWorldPage()))
  }

}
