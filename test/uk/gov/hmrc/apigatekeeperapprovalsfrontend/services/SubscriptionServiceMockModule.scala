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

import org.mockito.quality.Strictness
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}

import uk.gov.hmrc.apiplatform.modules.common.domain.models.ApplicationId
import uk.gov.hmrc.apiplatform.modules.submissions.MarkedSubmissionsTestData

import uk.gov.hmrc.apigatekeeperapprovalsfrontend.utils.ApiDataTestData

trait SubscriptionServiceMockModule extends MockitoSugar with ArgumentMatchersSugar with MarkedSubmissionsTestData with ApiDataTestData {

  trait BaseSubscriptionServiceMock {
    def aMock: SubscriptionService

    object FetchSubscriptionsByApplicationId {

      def thenReturn(apiDefinitions: (String, String, String)*) = {
        val definitions = apiDefinitions.map(t => anApiData(t._1, t._2, t._3)).toSet
        when(aMock.fetchSubscriptionsByApplicationId(*[ApplicationId])(*)).thenReturn(successful(definitions))
      }
    }
  }

  object SubscriptionServiceMock extends BaseSubscriptionServiceMock {
    val aMock = mock[SubscriptionService](withSettings.strictness(Strictness.LENIENT))
  }
}
