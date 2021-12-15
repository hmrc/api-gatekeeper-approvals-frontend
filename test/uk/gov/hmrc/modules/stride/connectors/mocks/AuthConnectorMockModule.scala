/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.modules.stride.connectors.mocks

import org.mockito.MockitoSugar
import org.mockito.ArgumentMatchersSugar
import uk.gov.hmrc.modules.stride.connectors.AuthConnector
import uk.gov.hmrc.auth.core.{Enrolment, Enrolments}
import uk.gov.hmrc.auth.core.retrieve.{~, Name, Retrieval}

import scala.concurrent.Future.successful
import java.util.UUID

class AuthConnectorMockModule extends MockitoSugar with ArgumentMatchersSugar {
  trait BaseAuthConnectorMock {
    def aMock: AuthConnector

    val userName = "userName"
    val superUserName = "superUserName"
    val adminName = "adminName"

    val userRole = s"userRole${UUID.randomUUID}"
    val adminRole = s"adminRole${UUID.randomUUID}"
    val superUserRole = s"superUserRole${UUID.randomUUID}"

    object Authorise {
      def thenReturn() = {
        val response = successful(new ~(Some(Name(Some(adminName), None)), Enrolments(Set(Enrolment(adminRole)))))

        when(aMock.authorise(*, any[Retrieval[~[Option[Name], Enrolments]]])(*, *)).thenReturn(response)
      }
    }
  }

  object AuthConnectorMock extends BaseAuthConnectorMock {
    val aMock = mock[AuthConnector](withSettings.lenient())
  }
}
