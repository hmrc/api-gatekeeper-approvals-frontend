/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.modules.stride.config

import javax.inject.{Inject, Provider, Singleton}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

case class StrideAuthConfig(
  authBaseUrl: String,
  strideLoginUrl: String,
  origin: String,
  adminRole: String,
  superUserRole: String,
  userRole: String
)

@Singleton
class StrideAuthConfigProvider @Inject()(config: ServicesConfig) extends Provider[StrideAuthConfig] {
  override def get(): StrideAuthConfig = {
    val authBaseUrl = config.baseUrl("auth")
    val strideLoginUrl = s"${config.baseUrl("stride-auth-frontend")}/stride/sign-in"
    val origin = config.getString("stride.origin")
    val adminRole = config.getString("stride.roles.admin")
    val superUserRole = config.getString("stride.roles.super-user")
    val userRole = config.getString("stride.roles.user")

    StrideAuthConfig(authBaseUrl, strideLoginUrl, origin, adminRole, superUserRole, userRole)
  }
}
