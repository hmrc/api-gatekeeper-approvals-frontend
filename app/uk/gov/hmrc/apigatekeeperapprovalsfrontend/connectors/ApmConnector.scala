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

import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.{ApiDefinition, Application, ApplicationId, ApplicationWithSubscriptionData}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}
import uk.gov.hmrc.play.http.metrics.common.API

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

object ApmConnector {
  case class Config(serviceBaseUrl: String)
}

@Singleton
class ApmConnector @Inject() (
    httpClient: HttpClient,
    config: ApmConnector.Config,
    val metrics: ConnectorMetrics
  )(implicit val ec: ExecutionContext
  ) {

  val serviceBaseUrl = config.serviceBaseUrl

  val api = API("api-platform-microservice")

  def fetchLinkedSubordinateApplicationById(id: ApplicationId)(implicit hc: HeaderCarrier): Future[Option[Application]] = {
    import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.Application._
    import uk.gov.hmrc.http.HttpReads.Implicits._

    metrics.record(api) {
      httpClient.GET[Option[Application]](s"$serviceBaseUrl/applications/${id.value}/linked-subordinate")
    }
  }

  def fetchSubscribableApisForApplication(id: ApplicationId)(implicit hc: HeaderCarrier): Future[Map[String, ApiDefinition]] = {
    import uk.gov.hmrc.http.HttpReads.Implicits._

    metrics.record(api) {
      httpClient.GET[Map[String, ApiDefinition]](s"$serviceBaseUrl/api-definitions", Seq("applicationId" -> id.value))
    }
  }

  def fetchApplicationWithSubscriptionData(id: ApplicationId)(implicit hc: HeaderCarrier): Future[Option[ApplicationWithSubscriptionData]] = {
    import uk.gov.hmrc.http.HttpReads.Implicits._

    metrics.record(api) {
      httpClient.GET[Option[ApplicationWithSubscriptionData]](s"$serviceBaseUrl/applications/${id.value}")
    }
  }
}
