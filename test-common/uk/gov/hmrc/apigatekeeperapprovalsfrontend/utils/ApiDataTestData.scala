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

package uk.gov.hmrc.apigatekeeperapprovalsfrontend.utils

import uk.gov.hmrc.apiplatform.modules.apis.domain.models.*
import uk.gov.hmrc.apiplatform.modules.apis.domain.models.ApiDefinition.ServiceBaseUrl
import uk.gov.hmrc.apiplatform.modules.common.domain.models.{ApiContext, ApiVersionNbr}

trait ApiDataTestData {

  def anApiData(
      serviceName: String,
      name: String,
      context: String
    ) = ApiDefinition(
    ServiceName(serviceName),
    serviceBaseUrl = ServiceBaseUrl(s"https://$serviceName.protected.mdtp"),
    ApiDefinition.Name(name),
    description = ApiDefinition.Description(name),
    ApiContext(context),
    Map(
      ApiVersionNbr("1.0") ->
        ApiVersion(
          ApiVersionNbr("1.0"),
          ApiStatus.Stable,
          ApiAccessType.Public,
          List(Endpoint(Endpoint.UriPattern("/sa/{utr}/status"), Endpoint.Name(s"Get $serviceName"), HttpMethod.Get, AuthType.User, scope = None, queryParameters = List.empty)),
          endpointsEnabled = true,
          None,
          ApiVersionSource.OAS
        )
    ),
    isTestSupport = false,
    lastPublishedAt = None,
    List(
      ApiCategory.SelfAssessment
    )
  )
}
