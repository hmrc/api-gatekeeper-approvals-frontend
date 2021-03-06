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

package uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.actions

import scala.concurrent.Future
import play.api.mvc._

import uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.models.ApplicationRequest
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.models.MarkedSubmissionApplicationRequest
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models._
import uk.gov.hmrc.apiplatform.modules.gkauth.controllers.GatekeeperBaseController
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.models.SubmissionInstanceApplicationRequest
import uk.gov.hmrc.apiplatform.modules.gkauth.controllers.actions.GatekeeperAuthorisationActions


trait GatekeeperRoleWithApplicationActions extends LoggedInRequestActionBuilders with GatekeeperAuthorisationActions {
  self: GatekeeperBaseController =>
  
  def loggedInWithApplication: (ApplicationId) => (ApplicationRequest[AnyContent] => Future[Result]) => Action[AnyContent] =
    roleWithApplication(anyAuthenticatedUserRefiner) _

  def loggedInWithApplicationAndSubmission: (ApplicationId) => (MarkedSubmissionApplicationRequest[AnyContent] => Future[Result]) => Action[AnyContent] =
    roleWithApplicationAndSubmission(anyAuthenticatedUserRefiner) _

  def loggedInWithApplicationAndSubmissionAndInstance: (ApplicationId, Int) => (SubmissionInstanceApplicationRequest[AnyContent] => Future[Result]) => Action[AnyContent] =
    roleWithApplicationAndSubmissionAndInstance(anyAuthenticatedUserRefiner) _

}
