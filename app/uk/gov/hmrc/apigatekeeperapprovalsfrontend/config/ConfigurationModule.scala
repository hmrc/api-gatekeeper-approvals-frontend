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

package uk.gov.hmrc.apigatekeeperapprovalsfrontend.config

import com.google.inject.AbstractModule

import uk.gov.hmrc.apigatekeeperapprovalsfrontend.connectors.ThirdPartyApplicationConnector
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.HandleForbiddenWithView
import uk.gov.hmrc.modules.stride.controllers.actions.ForbiddenHandler
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.connectors.ConnectorMetrics
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.connectors.ConnectorMetricsImpl
import uk.gov.hmrc.modules.submissions.config.SubmissionsConnectorConfigProvider
import uk.gov.hmrc.modules.submissions.connectors.SubmissionsConnector

class ConfigurationModule extends AbstractModule {
  override def configure() = {
    bind(classOf[ThirdPartyApplicationConnector.Config]).toProvider(classOf[ThirdPartyApplicationConnectorConfigProvider])
    bind(classOf[SubmissionsConnector.Config]).toProvider(classOf[SubmissionsConnectorConfigProvider])
    
    bind(classOf[ForbiddenHandler]).to(classOf[HandleForbiddenWithView])
    bind(classOf[ConnectorMetrics]).to(classOf[ConnectorMetricsImpl])
  }
}
