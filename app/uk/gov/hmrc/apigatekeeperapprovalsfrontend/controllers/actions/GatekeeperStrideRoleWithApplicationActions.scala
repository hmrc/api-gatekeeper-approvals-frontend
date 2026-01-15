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

package uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.actions

import scala.concurrent.Future

import play.api.mvc._

import uk.gov.hmrc.apiplatform.modules.common.domain.models.ApplicationId
import uk.gov.hmrc.apiplatform.modules.gkauth.controllers.GatekeeperBaseController
import uk.gov.hmrc.apiplatform.modules.gkauth.domain.models.{GatekeeperRoles, GatekeeperStrideRole}

import uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.models.{ApplicationRequest, MarkedSubmissionApplicationRequest}

trait GatekeeperStrideRoleWithApplicationActions extends LoggedInRequestActionBuilders {
  self: GatekeeperBaseController =>

  private def loggedInThruStrideRoleWithApplication: (GatekeeperStrideRole) => (ApplicationId) => (ApplicationRequest[AnyContent] => Future[Result]) => Action[AnyContent] =
    (role) => roleWithApplication(gatekeeperRoleActionRefiner(role)) _

  private def loggedInThruStrideRoleWithApplicationAndSubmission
      : (GatekeeperStrideRole) => (ApplicationId) => (MarkedSubmissionApplicationRequest[AnyContent] => Future[Result]) => Action[AnyContent] =
    (role) => roleWithApplicationAndSubmission(gatekeeperRoleActionRefiner(role)) _

  def strideAdvancedUserWithApplication: (ApplicationId) => (ApplicationRequest[AnyContent] => Future[Result]) => Action[AnyContent] =
    loggedInThruStrideRoleWithApplication(GatekeeperRoles.ADVANCEDUSER)

  def strideAdvancedUserWithApplicationAndSubmission: (ApplicationId) => (MarkedSubmissionApplicationRequest[AnyContent] => Future[Result]) => Action[AnyContent] =
    loggedInThruStrideRoleWithApplicationAndSubmission(GatekeeperRoles.ADVANCEDUSER)
}
