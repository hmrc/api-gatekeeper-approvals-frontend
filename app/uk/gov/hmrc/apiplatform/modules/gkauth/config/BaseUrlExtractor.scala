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

package uk.gov.hmrc.apiplatform.modules.gkauth.config

import com.typesafe.config.Config


trait BaseUrlExtractor {
  def config: Config

  protected lazy val rootServices = "microservice.services"

  def extractBaseUrl(serviceName: String) = {
    val protocol = config.getString(s"${rootServices}.$serviceName.protocol")
    val host     = config.getString(s"${rootServices}.$serviceName.host")
    val port     = config.getString(s"${rootServices}.$serviceName.port")
    s"$protocol://$host:$port"
  }
}