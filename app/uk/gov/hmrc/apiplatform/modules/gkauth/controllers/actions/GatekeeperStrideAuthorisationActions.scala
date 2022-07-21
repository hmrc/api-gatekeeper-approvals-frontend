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

package uk.gov.hmrc.apiplatform.modules.gkauth.controllers.actions

import play.api.mvc.{AnyContent, Result}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import uk.gov.hmrc.apiplatform.modules.gkauth.domain.models.GatekeeperStrideRole
import uk.gov.hmrc.apiplatform.modules.gkauth.domain.models.GatekeeperRoles
import uk.gov.hmrc.apiplatform.modules.gkauth.domain.models.LoggedInRequest

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.Future.successful
import play.api.mvc.ActionRefiner
import play.api.mvc.MessagesRequest
import uk.gov.hmrc.apiplatform.modules.gkauth.services._

trait ForbiddenHandler {
  def handle(msgResult: MessagesRequest[_]): Result
}

trait GatekeeperStrideAuthorisationActions {
  self: FrontendBaseController =>

  def strideAuthorisationService: StrideAuthorisationService

  implicit def ec: ExecutionContext
  
  def gatekeeperRoleActionRefiner(minimumRoleRequired: GatekeeperStrideRole): ActionRefiner[MessagesRequest, LoggedInRequest] = 
    new ActionRefiner[MessagesRequest, LoggedInRequest] {
      def executionContext = ec

      def refine[A](msgRequest: MessagesRequest[A]): Future[Either[Result, LoggedInRequest[A]]] = {
        strideAuthorisationService.refineStride(minimumRoleRequired)(msgRequest)
      }
    }
}

trait GatekeeperAuthorisationActions {
  self: FrontendBaseController with GatekeeperStrideAuthorisationActions =>
    
  def ldapAuthorisationService: LdapAuthorisationService
    
  val anyAuthenticatedUserRefiner = new ActionRefiner[MessagesRequest, LoggedInRequest] {

    override def executionContext = ec

    override protected def refine[A](request: MessagesRequest[A]): Future[Either[Result,LoggedInRequest[A]]] = {
      ldapAuthorisationService.refineLdap(request)
      .recover {
        case _ => Left(())
      }
      .flatMap(_ match {
        case Right(lir) => successful(Right(lir))
        case Left(_) => strideAuthorisationService.refineStride(GatekeeperRoles.USER)(request)
      })
    }
  }
}
