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

import scala.concurrent.{ExecutionContext, Future}

import play.api.mvc.Results.NotFound
import play.api.mvc._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import uk.gov.hmrc.apigatekeeperapprovalsfrontend.config.ErrorHandler
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.models.ApplicationRequest
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.ApplicationId
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.services.ApplicationActionService
import uk.gov.hmrc.modules.stride.controllers.GatekeeperBaseController
import uk.gov.hmrc.modules.stride.controllers.models.LoggedInRequest
import uk.gov.hmrc.modules.stride.domain.models.GatekeeperRole

trait ApplicationActionBuilders {
  def errorHandler: ErrorHandler
  def applicationActionService: ApplicationActionService

  implicit def hc(implicit request: Request[_]): HeaderCarrier =
    HeaderCarrierConverter.fromRequestAndSession(request, request.session)
  
  def applicationRequestRefiner(applicationId: ApplicationId)(implicit ec: ExecutionContext): ActionRefiner[LoggedInRequest, ApplicationRequest] = {
    new ActionRefiner[LoggedInRequest, ApplicationRequest] {
      override protected def executionContext: ExecutionContext = ec

      override def refine[A](request: LoggedInRequest[A]): Future[Either[Result, ApplicationRequest[A]]] = {
        implicit val implicitRequest: Request[_] = request
        import cats.implicits._

        applicationActionService.process(applicationId, request)
        .toRight(NotFound(errorHandler.notFoundTemplate(Request(request, request.messagesApi)))).value
      }
    }
  }
}

trait ApplicationActions extends ApplicationActionBuilders {
  self: GatekeeperBaseController =>

  private def strideRoleWithApplication(minimumGatekeeperRole: GatekeeperRole.GatekeeperRole)(applicationId: ApplicationId)(block: ApplicationRequest[_] => Future[Result]): Action[AnyContent] =
    Action.async { implicit request =>
      (
        gatekeeperRoleActionRefiner(minimumGatekeeperRole) andThen
        applicationRequestRefiner(applicationId)
      )
      .invokeBlock(request, block)
    }

  def loggedInWithApplication(applicationId: ApplicationId)(block: ApplicationRequest[_] => Future[Result]): Action[AnyContent] =
    strideRoleWithApplication(GatekeeperRole.USER)(applicationId)(block)

  def superUserWithApplication(applicationId: ApplicationId)(block: ApplicationRequest[_] => Future[Result]): Action[AnyContent] =
    strideRoleWithApplication(GatekeeperRole.SUPERUSER)(applicationId)(block)

  def adminWithApplication(applicationId: ApplicationId)(block: ApplicationRequest[_] => Future[Result]): Action[AnyContent] =
    strideRoleWithApplication(GatekeeperRole.ADMIN)(applicationId)(block)
}
