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
import scala.concurrent.Future

import cats.data.OptionT
import org.mockito.quality.Strictness
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}

import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.ApplicationWithCollaborators
import uk.gov.hmrc.apiplatform.modules.common.domain.models.ApplicationId
import uk.gov.hmrc.apiplatform.modules.gkauth.domain.models.LoggedInRequest

import uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.models.ApplicationRequest
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.services.ApplicationActionService

trait ApplicationActionServiceMockModule extends MockitoSugar with ArgumentMatchersSugar {

  trait BaseApplicationActionServiceMock {
    def aMock: ApplicationActionService

    object Process {

      def thenReturn[A](application: ApplicationWithCollaborators) = {
        import cats.implicits._

        when(aMock.process[A](eqTo(application.id), *)(*))
          .thenAnswer((a: ApplicationId, req: LoggedInRequest[A]) => OptionT.pure[Future](new ApplicationRequest[A](application, req)))
      }

      def thenNotFound[A]() = {
        import cats.implicits._

        when(aMock.process[A](*[ApplicationId], *)(*))
          .thenAnswer((a: ApplicationId, req: LoggedInRequest[A]) => OptionT.none)
      }
    }
  }

  object ApplicationActionServiceMock extends BaseApplicationActionServiceMock {
    val aMock = mock[ApplicationActionService](withSettings.strictness(Strictness.LENIENT))
  }
}
