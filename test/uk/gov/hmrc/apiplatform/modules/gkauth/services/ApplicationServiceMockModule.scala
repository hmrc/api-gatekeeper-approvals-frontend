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

package uk.gov.hmrc.apiplatform.modules.gkauth.services

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future.successful

import org.mockito.quality.Strictness
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}

import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.ApplicationWithCollaborators
import uk.gov.hmrc.apiplatform.modules.commands.applications.domain.models._
import uk.gov.hmrc.apiplatform.modules.common.domain.models.{ApplicationId, LaxEmailAddress}

import uk.gov.hmrc.apigatekeeperapprovalsfrontend.services.ApplicationService
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.utils.ApplicationTestData

trait ApplicationServiceMockModule extends MockitoSugar with ArgumentMatchersSugar with ApplicationTestData {

  trait BaseApplicationServiceMock {
    val CHT = new CommandHandlerTypes[DispatchSuccessResult] {}

    import CHT.Implicits._

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

    object DeclineApplicationApprovalRequest {

      def thenReturnSuccess() = {
        when(aMock.declineApplicationApprovalRequest(*[ApplicationId], *, *, *[Set[LaxEmailAddress]])(*)).thenReturn(DispatchSuccessResult(
          mock[ApplicationWithCollaborators]
        ).asSuccess)
      }

      def thenReturnFailure() = {
        when(aMock.declineApplicationApprovalRequest(*[ApplicationId], *, *, *)(*)).thenThrow(new RuntimeException("Application id not found"))
      }
    }
  }

  object ApplicationServiceMock extends BaseApplicationServiceMock {
    val aMock = mock[ApplicationService](withSettings.strictness(Strictness.LENIENT))
  }
}
