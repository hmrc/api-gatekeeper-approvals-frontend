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

import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import play.api.mvc.MessagesControllerComponents
import play.api.mvc.MessagesRequest
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.views.html.ForbiddenView
import uk.gov.hmrc.modules.stride.config.StrideAuthConfig
import uk.gov.hmrc.modules.stride.connectors.AuthConnector
import uk.gov.hmrc.modules.stride.controllers.actions.GatekeeperAuthorisationActions

abstract class BaseController(
  val forbiddenView: ForbiddenView,
  val strideAuthConfig: StrideAuthConfig,
  val authConnector: AuthConnector,
  mcc: MessagesControllerComponents
) extends FrontendController(mcc) with GatekeeperAuthorisationActions  {
  
  def forbiddenResult(implicit request: MessagesRequest[_]) = Forbidden(forbiddenView())
}
