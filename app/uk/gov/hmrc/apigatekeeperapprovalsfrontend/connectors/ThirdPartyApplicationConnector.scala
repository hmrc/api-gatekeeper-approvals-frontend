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

import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps, UpstreamErrorResponse}
import uk.gov.hmrc.play.http.metrics.common.API

import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.ApplicationWithCollaborators
import uk.gov.hmrc.apiplatform.modules.common.domain.models.ApplicationId

object ThirdPartyApplicationConnector {
  type ErrorOrUnit = Either[UpstreamErrorResponse, Unit]
  case class Config(serviceBaseUrl: String)
}

@Singleton
class ThirdPartyApplicationConnector @Inject() (
    httpClient: HttpClientV2,
    config: ThirdPartyApplicationConnector.Config,
    val metrics: ConnectorMetrics
  )(implicit val ec: ExecutionContext
  ) extends CommonResponseHandlers {

  val serviceBaseUrl = config.serviceBaseUrl
  val api            = API("third-party-application")

  def fetchApplicationById(id: ApplicationId)(implicit hc: HeaderCarrier): Future[Option[ApplicationWithCollaborators]] = {
    import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.ApplicationWithCollaborators._

    metrics.record(api) {
      httpClient.get(url"$serviceBaseUrl/application/$id").execute[Option[ApplicationWithCollaborators]]
    }
  }
}
