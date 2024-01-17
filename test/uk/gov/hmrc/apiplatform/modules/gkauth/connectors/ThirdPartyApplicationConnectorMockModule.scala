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

package uk.gov.hmrc.apiplatform.modules.gkauth.connectors

import scala.concurrent.Future.successful

import org.mockito.{ArgumentMatchersSugar, MockitoSugar}

import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.{ApplicationState, State}
import uk.gov.hmrc.apiplatform.modules.common.domain.models.{ApplicationId, ClientId}
import uk.gov.hmrc.apiplatform.modules.common.utils.FixedClock

import uk.gov.hmrc.apigatekeeperapprovalsfrontend.connectors.ThirdPartyApplicationConnector
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.Application

trait ThirdPartyApplicationConnectorMockModule extends MockitoSugar with ArgumentMatchersSugar with FixedClock {

  trait BaseThirdPartyApplicationConnectorMock {
    def aMock: ThirdPartyApplicationConnector

    object FetchApplicationById {

      def thenReturn() = {
        when(aMock.fetchApplicationById(*[ApplicationId])(*)).thenAnswer((appId: ApplicationId) =>
          successful(Some(Application(appId, ClientId.random, "app name", Set.empty, state = ApplicationState(State.TESTING, None, None, None, instant))))
        )
      }
    }
  }

  object ThirdPartyApplicationConnectorMock extends BaseThirdPartyApplicationConnectorMock {
    val aMock = mock[ThirdPartyApplicationConnector]
  }
}
