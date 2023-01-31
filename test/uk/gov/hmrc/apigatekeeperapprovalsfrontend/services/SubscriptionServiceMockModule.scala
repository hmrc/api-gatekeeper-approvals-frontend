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

import scala.concurrent.Future.successful

import org.mockito.{ArgumentMatchersSugar, MockitoSugar}

import uk.gov.hmrc.apiplatform.modules.submissions.MarkedSubmissionsTestData

import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models._

trait SubscriptionServiceMockModule extends MockitoSugar with ArgumentMatchersSugar with MarkedSubmissionsTestData {

  trait BaseSubscriptionServiceMock {
    def aMock: SubscriptionService

    object FetchSubscriptionsByApplicationId {

      def thenReturn(apiDefinitions: (String, String)*) = {
        val definitions = apiDefinitions.map(t => ApiDefinition(t._1, t._2)).toSet
        when(aMock.fetchSubscriptionsByApplicationId(*[ApplicationId])(*)).thenReturn(successful(definitions))
      }
    }
  }

  object SubscriptionServiceMock extends BaseSubscriptionServiceMock {
    val aMock = mock[SubscriptionService](withSettings.lenient())
  }
}
