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

package uk.gov.hmrc.apigatekeeperapprovalsfrontend.services

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future.successful
import scala.concurrent.{ExecutionContext, Future}

import cats.data.OptionT

import uk.gov.hmrc.apiplatform.modules.apis.domain.models.ApiDefinition
import uk.gov.hmrc.apiplatform.modules.common.domain.models.ApplicationId
import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.apigatekeeperapprovalsfrontend.connectors.ApmConnector

@Singleton
class SubscriptionService @Inject() (
    apmConnector: ApmConnector
  )(implicit val ec: ExecutionContext
  ) {

  def fetchSubscriptionsByApplicationId(applicationId: ApplicationId)(implicit hc: HeaderCarrier): Future[Set[ApiDefinition]] = {
    (
      for {
        applicationWithSubscriptions <- OptionT(apmConnector.fetchApplicationWithSubscriptionData(applicationId))
        subscribableApis             <- OptionT.liftF(apmConnector.fetchSubscribableApisForApplication(applicationId))
        applicationSubscriptions      = applicationWithSubscriptions.subscriptions.flatMap(apiDefinition => subscribableApis.find(_.context == apiDefinition.context))
      } yield applicationSubscriptions
    ).getOrElseF(successful(Set.empty))
  }
}
