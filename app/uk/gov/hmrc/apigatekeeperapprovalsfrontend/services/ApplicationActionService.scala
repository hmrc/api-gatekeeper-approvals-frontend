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

package uk.gov.hmrc.apigatekeeperapprovalsfrontend.services

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

import cats.data.OptionT

import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.models.ApplicationRequest
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.ApplicationId
import uk.gov.hmrc.apiplatform.modules.gkauth.domain.models.LoggedInRequest

@Singleton
class ApplicationActionService @Inject()(
  applicationService: ApplicationService
)(implicit ec: ExecutionContext) {
  
  def process[A](applicationId: ApplicationId, loggedInRequest: LoggedInRequest[A])(implicit hc: HeaderCarrier): OptionT[Future, ApplicationRequest[A]] = {
    import cats.implicits._

    for {
      application <- OptionT(applicationService.fetchByApplicationId(applicationId))
    } yield new ApplicationRequest(application, loggedInRequest)
  }
}
