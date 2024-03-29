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

package uk.gov.hmrc.apigatekeeperapprovalsfrontend.utils

import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.{ApplicationState, Collaborator, State}
import uk.gov.hmrc.apiplatform.modules.common.domain.models.{ApplicationId, ClientId}
import uk.gov.hmrc.apiplatform.modules.common.utils.FixedClock

import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.Application

trait ApplicationTestData extends FixedClock {

  def anApplication(
      id: ApplicationId = ApplicationId.random,
      clientId: ClientId = ClientId.random,
      name: String = "app name",
      collaborators: Set[Collaborator] = Set.empty
    ) = Application(id, clientId, name, collaborators, state = ApplicationState(State.TESTING, None, None, None, instant))
}
