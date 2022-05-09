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

package uk.gov.hmrc.apiplatform.modules.stride.connectors.mocks

import org.mockito.MockitoSugar
import org.mockito.ArgumentMatchersSugar

import scala.concurrent.Future.successful
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.services.ApplicationService
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.ApplicationId
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.utils.ApplicationTestData

trait ApplicationServiceMockModule extends MockitoSugar with ArgumentMatchersSugar with ApplicationTestData {
  trait BaseApplicationServiceMock {
    def aMock: ApplicationService

    object FetchByApplicationId {
      def thenReturn(applicationId: ApplicationId) = {
        val response = Some(anApplication(id = applicationId))
        when(aMock.fetchByApplicationId(eqTo(applicationId))(*)).thenReturn(successful(response))
      }
      def thenNotFound() = {
        when(aMock.fetchByApplicationId(*[ApplicationId])(*)).thenReturn(successful(None))
      }
    }

    object FetchLinkedSubordinateApplicationByApplicationId {
      def thenReturn(subordinateApplicationId: ApplicationId) = {
        when(aMock.fetchLinkedSubordinateApplicationByApplicationId(*[ApplicationId])(*))
          .thenReturn(successful(Some(anApplication(id = subordinateApplicationId))))
      }
    }

  }

  object ApplicationServiceMock extends BaseApplicationServiceMock {
    val aMock = mock[ApplicationService](withSettings.lenient())
  }
}
