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
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.{ApplicationId,Application,Submission,SubmissionId,MarkedSubmission}
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.connectors.ThirdPartyApplicationConnector
import scala.concurrent.Future.{successful, failed}
import java.util.UUID
import uk.gov.hmrc.auth.core.InsufficientEnrolments
import uk.gov.hmrc.auth.core.SessionRecordNotFound

trait ThirdPartyApplicationConnectorMockModule extends MockitoSugar with ArgumentMatchersSugar {
  trait BaseThirdPartyApplicationConnectorMock {
    def aMock: ThirdPartyApplicationConnector

    object FetchApplicationById {
      def thenReturn() = {
        when(aMock.fetchApplicationById(*[ApplicationId])(*)).thenAnswer((appId: ApplicationId) => successful(Some(Application(appId, "app name"))))
      }
    }

    object FetchLatestMarkedSubmission {
      def thenReturn() = {
        when(aMock.fetchLatestMarkedSubmission(*[ApplicationId])(*)).thenAnswer((appId: ApplicationId) => 
          successful(Some(MarkedSubmission(Submission(SubmissionId.random, appId), Map.empty))))
      }
    }
  }

  object ThirdPartyApplicationConnectorMock extends BaseThirdPartyApplicationConnectorMock {
    val aMock = mock[ThirdPartyApplicationConnector]
  }
}
