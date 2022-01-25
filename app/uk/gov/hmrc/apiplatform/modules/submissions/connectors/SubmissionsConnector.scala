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

package uk.gov.hmrc.apiplatform.modules.submissions.connectors

import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models._
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.connectors.ConnectorMetrics
import uk.gov.hmrc.apiplatform.modules.submissions.domain.models._
import uk.gov.hmrc.apiplatform.modules.submissions.domain.services._
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.play.http.metrics.common.API


object SubmissionsConnector {
  case class Config(serviceBaseUrl: String, apiKey: String)
}

@Singleton
class SubmissionsConnector @Inject() (
    val http: HttpClient,
    val config: SubmissionsConnector.Config,
    val metrics: ConnectorMetrics
)(implicit val ec: ExecutionContext)
    extends SubmissionsFrontendJsonFormatters {

  import config._

  val api = API("third-party-application-submissions")

  def fetchLatestSubmission(applicationId: ApplicationId)(implicit hc: HeaderCarrier): Future[Option[ExtendedSubmission]] = {
    metrics.record(api) {
      http.GET[Option[ExtendedSubmission]](s"$serviceBaseUrl/submissions/application/${applicationId.value}")
    }
  }
  
  def fetchLatestMarkedSubmission(id: ApplicationId)(implicit hc: HeaderCarrier): Future[Option[MarkedSubmission]] = {
    import uk.gov.hmrc.http.HttpReads.Implicits._
    val url = s"$serviceBaseUrl/submissions/marked/application/${id.value}"
    
    metrics.record(api) {
      http.GET[Option[MarkedSubmission]](url)
    }
  }
}