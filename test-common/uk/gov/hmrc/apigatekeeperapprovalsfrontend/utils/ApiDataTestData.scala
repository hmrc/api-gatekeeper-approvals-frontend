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

import uk.gov.hmrc.apiplatform.modules.apis.domain.models._
import uk.gov.hmrc.apiplatform.modules.common.domain.models.{ApiContext, ApiVersionNbr}

trait ApiDataTestData {

  def anApiData(
      serviceName: String,
      name: String
    ) = ApiData(
    ServiceName(serviceName),
    s"https://${serviceName}.protected.mdtp",
    name,
    name,
    ApiContext(serviceName),
    Map(
      ApiVersionNbr("1.0") ->
        ApiVersion(
          ApiVersionNbr("1.0"),
          ApiStatus.STABLE,
          ApiAccess.PUBLIC,
          List(Endpoint("/sa/{utr}/status", s"Get ${serviceName}", HttpMethod.GET, AuthType.USER)),
          true,
          None,
          ApiVersionSource.OAS
        )
    ),
    false,
    false,
    None,
    List(
      ApiCategory.SELF_ASSESSMENT
    )
  )
}
