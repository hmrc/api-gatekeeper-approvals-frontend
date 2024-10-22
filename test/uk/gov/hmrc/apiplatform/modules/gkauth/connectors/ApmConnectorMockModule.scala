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

import uk.gov.hmrc.apiplatform.modules.apis.domain.models.ApiDefinition
import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.ApplicationWithCollaboratorsFixtures
import uk.gov.hmrc.apiplatform.modules.common.domain.models.{ApiIdentifier, ApplicationId}

import uk.gov.hmrc.apigatekeeperapprovalsfrontend.connectors.ApmConnector
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.ApplicationWithSubscriptionData

trait ApmConnectorMockModule extends MockitoSugar with ArgumentMatchersSugar with ApplicationWithCollaboratorsFixtures {

  trait BaseApmConnectorMock {
    def aMock: ApmConnector

    object FetchLinkedSubordinateApplicationById {

      def thenReturn(subordinateApplicationId: ApplicationId) =
        when(aMock.fetchLinkedSubordinateApplicationById(*[ApplicationId])(*)).thenReturn(successful(Some(standardApp.withId(subordinateApplicationId))))
    }

    object FetchSubscribableApisForApplication {

      def thenReturn(apiDefinitions: List[ApiDefinition]) =
        when(aMock.fetchSubscribableApisForApplication(*[ApplicationId])(*)).thenReturn(successful(apiDefinitions))
      def thenReturnNothing                               = when(aMock.fetchSubscribableApisForApplication(*[ApplicationId])(*)).thenReturn(successful(List()))
    }

    object FetchApplicationWithSubscriptionData {

      def thenReturn(apiIdentifiers: ApiIdentifier*) =
        when(aMock.fetchApplicationWithSubscriptionData(*[ApplicationId])(*)).thenReturn(
          successful(Some(ApplicationWithSubscriptionData(standardApp, apiIdentifiers.toSet)))
        )
      def thenReturnNothing                          = when(aMock.fetchApplicationWithSubscriptionData(*[ApplicationId])(*)).thenReturn(successful(None))
    }
  }

  object ApmConnectorMock extends BaseApmConnectorMock {
    val aMock = mock[ApmConnector]
  }
}
