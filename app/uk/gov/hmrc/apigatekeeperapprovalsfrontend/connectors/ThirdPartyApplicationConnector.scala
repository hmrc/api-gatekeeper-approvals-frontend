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

package uk.gov.hmrc.apigatekeeperapprovalsfrontend.connectors

import uk.gov.hmrc.apigatekeeperapprovalsfrontend.connectors.ThirdPartyApplicationConnector.ErrorOrUnit
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.connectors.AddTermsOfUseAcceptanceRequest

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, UpstreamErrorResponse}
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.{Application, ApplicationId}
import uk.gov.hmrc.play.http.metrics.common.API

object ThirdPartyApplicationConnector {
  type ErrorOrUnit = Either[UpstreamErrorResponse, Unit]
  case class Config(serviceBaseUrl: String)
}

@Singleton
class ThirdPartyApplicationConnector @Inject()(
  httpClient: HttpClient,
  config: ThirdPartyApplicationConnector.Config,
  val metrics: ConnectorMetrics
)(implicit val ec: ExecutionContext) {

  val serviceBaseUrl = config.serviceBaseUrl
  val api = API("third-party-application")


  def fetchApplicationById(id: ApplicationId)(implicit hc: HeaderCarrier): Future[Option[Application]] = {
    import uk.gov.hmrc.http.HttpReads.Implicits._
    import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.Application._

    metrics.record(api) {
      httpClient.GET[Option[Application]](s"$serviceBaseUrl/application/${id.value}")
    }
  }

  def addTermsOfUseAcceptance(id: ApplicationId, addTermsOfUseAcceptanceRequest: AddTermsOfUseAcceptanceRequest)(implicit hc: HeaderCarrier) = {
    import uk.gov.hmrc.http.HttpReads.Implicits._

    metrics.record(api) {
      httpClient.POST[AddTermsOfUseAcceptanceRequest, ErrorOrUnit](s"$serviceBaseUrl/application/${id.value}/terms-of-use-acceptance", addTermsOfUseAcceptanceRequest)
    }
  }
}
