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

import uk.gov.hmrc.apiplatform.modules.gkauth.services.StrideAuthorisationService
import scala.concurrent.ExecutionContext
import uk.gov.hmrc.apiplatform.modules.gkauth.controllers.GatekeeperBaseController
import play.api.mvc._
import uk.gov.hmrc.apiplatform.modules.common.services.ApplicationLogger
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.config.ErrorHandler
import uk.gov.hmrc.apiplatform.modules.common.services.EitherTHelper
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.actions.GatekeeperStrideRoleWithApplicationActions
import uk.gov.hmrc.apiplatform.modules.gkauth.services.StrideAuthorisationService
import org.joda.time.DateTime


abstract class AbstractApplicationController(
  strideAuthorisationService: StrideAuthorisationService,
  mcc: MessagesControllerComponents,
  val errorHandler: ErrorHandler
)(implicit override val ec: ExecutionContext)
    extends GatekeeperBaseController(strideAuthorisationService, mcc)
    with GatekeeperStrideRoleWithApplicationActions
    with EitherTHelper[Result]
    with ApplicationLogger {

  implicit class TimestampSyntax(datetime: DateTime) {
    def asText = datetime.toString("dd MMMM yyyy")
  }

}