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

import java.time.Clock
import java.time.LocalDateTime
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

import uk.gov.hmrc.apiplatform.modules.common.domain.models.ApplicationId
import uk.gov.hmrc.apiplatform.modules.commands.applications.domain.models.ApplicationCommands
import uk.gov.hmrc.apiplatform.modules.commands.applications.domain.models.{DispatchSuccessResult, _}
import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.apigatekeeperapprovalsfrontend.connectors.{ApmConnector, ApplicationCommandConnector, ThirdPartyApplicationConnector}
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.Application
import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress

@Singleton
class ApplicationService @Inject() (
    thirdPartyApplicationConnector: ThirdPartyApplicationConnector,
    apmConnector: ApmConnector,
    applicationCommandConnector: ApplicationCommandConnector
  )(implicit val ec: ExecutionContext
  ) extends CommandHandlerTypes[DispatchSuccessResult] {

  def fetchByApplicationId(applicationId: ApplicationId)(implicit hc: HeaderCarrier): Future[Option[Application]] = {
    thirdPartyApplicationConnector.fetchApplicationById(applicationId)
  }

  def fetchLinkedSubordinateApplicationByApplicationId(applicationId: ApplicationId)(implicit hc: HeaderCarrier): Future[Option[Application]] = {
    apmConnector.fetchLinkedSubordinateApplicationById(applicationId)
  }

  def declineApplicationApprovalRequest(applicationId: ApplicationId, requestedBy: String, reasons: String, adminsToEmail: Set[LaxEmailAddress])(implicit hc: HeaderCarrier): AppCmdResult = {
    val request = ApplicationCommands.DeclineApplicationApprovalRequest(requestedBy, reasons, LocalDateTime.now)
    applicationCommandConnector.dispatch(applicationId, request, adminsToEmail)
  }
}
