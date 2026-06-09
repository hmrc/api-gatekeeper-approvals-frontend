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
import org.mockito.ArgumentMatchers.{any as `*`, eq as eqTo}
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar

import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.{ApplicationWithCollaborators, ApplicationWithCollaboratorsFixtures}
import uk.gov.hmrc.apiplatform.modules.commands.applications.domain.models._
import uk.gov.hmrc.apiplatform.modules.common.domain.models.{ApplicationId, LaxEmailAddress}

import uk.gov.hmrc.apigatekeeperapprovalsfrontend.services.ApplicationService

trait ApplicationServiceMockModule extends MockitoSugar with ApplicationWithCollaboratorsFixtures {

  trait BaseApplicationServiceMock {
    val CHT = new CommandHandlerTypes[DispatchSuccessResult] {}

    import CHT.Implicits._

    def aMock: ApplicationService

    object FetchByApplicationId {

      def thenReturn(applicationId: ApplicationId) = {
        val response = Some(standardApp.withId(applicationId))
        when(aMock.fetchByApplicationId(eqTo(applicationId))(using *)).thenReturn(successful(response))
      }

      def thenNotFound() = {
        when(aMock.fetchByApplicationId(*[ApplicationId])(using *)).thenReturn(successful(None))
      }
    }

    object FetchLinkedSubordinateApplicationByApplicationId {

      def thenReturn(subordinateApplicationId: ApplicationId) = {
        when(aMock.fetchLinkedSubordinateApplicationByApplicationId(*[ApplicationId])(using *))
          .thenReturn(successful(Some(standardApp.withId(subordinateApplicationId))))
      }

      def thenReturnNone() = {
        when(aMock.fetchLinkedSubordinateApplicationByApplicationId(*[ApplicationId])(using *))
          .thenReturn(successful(None))
      }
    }

    object DeclineApplicationApprovalRequest {

      def thenReturnSuccess() = {
        when(aMock.declineApplicationApprovalRequest(*[ApplicationId], *, *, *[Set[LaxEmailAddress]])(using *)).thenReturn(DispatchSuccessResult(
          mock[ApplicationWithCollaborators]
        ).asSuccess)
      }

      def thenReturnFailure() = {
        when(aMock.declineApplicationApprovalRequest(*[ApplicationId], *, *, *)(using *)).thenThrow(new RuntimeException("Application id not found"))
      }
    }
  }

  object ApplicationServiceMock extends BaseApplicationServiceMock {
    val aMock = mock[ApplicationService](withSettings.strictness(Strictness.LENIENT))
  }
}
