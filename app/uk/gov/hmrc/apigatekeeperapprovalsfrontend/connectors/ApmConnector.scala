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

package uk.gov.hmrc.apigatekeeperapprovalsfrontend.connectors

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}
import uk.gov.hmrc.play.http.metrics.common.API

import uk.gov.hmrc.apiplatform.modules.apis.domain.models.{ApiDefinition, MappedApiDefinitions}
import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.ApplicationWithCollaborators
import uk.gov.hmrc.apiplatform.modules.common.domain.models.ApplicationId

import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.ApplicationWithSubscriptionData

object ApmConnector {
  case class Config(serviceBaseUrl: String)
}

@Singleton
class ApmConnector @Inject() (
    httpClient: HttpClientV2,
    config: ApmConnector.Config,
    val metrics: ConnectorMetrics
  )(implicit val ec: ExecutionContext
  ) {

  val serviceBaseUrl = config.serviceBaseUrl

  val api = API("api-platform-microservice")

  def fetchLinkedSubordinateApplicationById(id: ApplicationId)(implicit hc: HeaderCarrier): Future[Option[ApplicationWithCollaborators]] = {
    import uk.gov.hmrc.http.HttpReads.Implicits._

    metrics.record(api) {
      httpClient.get(url"$serviceBaseUrl/applications/$id/linked-subordinate").execute[Option[ApplicationWithCollaborators]]
    }
  }

  def fetchSubscribableApisForApplication(id: ApplicationId)(implicit hc: HeaderCarrier): Future[List[ApiDefinition]] = {
    import uk.gov.hmrc.http.HttpReads.Implicits._

    metrics.record(api) {
      httpClient.get(url"$serviceBaseUrl/api-definitions?applicationId=$id")
        .execute[MappedApiDefinitions]
        .map(_.wrapped.values.toList)
    }
  }

  def fetchApplicationWithSubscriptionData(id: ApplicationId)(implicit hc: HeaderCarrier): Future[Option[ApplicationWithSubscriptionData]] = {
    import uk.gov.hmrc.http.HttpReads.Implicits._

    metrics.record(api) {
      httpClient.get(url"$serviceBaseUrl/applications/$id").execute[Option[ApplicationWithSubscriptionData]]
    }
  }
}
