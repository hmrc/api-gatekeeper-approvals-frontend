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

import uk.gov.hmrc.apigatekeeperapprovalsfrontend.services.ApplicationService
import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.ApplicationId
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.modules.stride.config.StrideAuthConfig
import uk.gov.hmrc.modules.stride.connectors.AuthConnector
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.views.html.ForbiddenView
import scala.concurrent.ExecutionContext
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.config.ErrorHandler
import play.api.libs.json.Json

@Singleton
class ApplicationController @Inject()(
  strideAuthConfig: StrideAuthConfig,
  authConnector: AuthConnector,
  forbiddenView: ForbiddenView,
  mcc: MessagesControllerComponents,
  applicationService: ApplicationService,
  errorHandler: ErrorHandler
)(implicit val ec: ExecutionContext) extends BaseController(forbiddenView, strideAuthConfig, authConnector, mcc) {
  
  def getApplication(applicationId: ApplicationId): Action[AnyContent] = anyStrideUserAction { implicit request =>
    import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.Application.applicationWrites

    applicationService.fetchByApplicationId(applicationId).map(o => 
      o.fold(
        NotFound(errorHandler.notFoundTemplate)
      )(a => Ok(Json.toJson(a)))
    )
  }
}
