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
import uk.gov.hmrc.apiplatform.modules.gkauth.domain.models.LoggedInRequest

import uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.models.{ApplicationRequest, MarkedSubmissionApplicationRequest, SubmissionInstanceApplicationRequest}

trait LoggedInRequestActionBuilders extends ApplicationActionBuilders {
  self: GatekeeperBaseController =>

  protected def role(refiner: ActionRefiner[MessagesRequest, LoggedInRequest])()(block: LoggedInRequest[AnyContent] => Future[Result]): Action[AnyContent] =
    Action.async { implicit request =>
      refiner.invokeBlock(request, block)
    }

  protected def roleWithApplication(
      refiner: ActionRefiner[MessagesRequest, LoggedInRequest]
    )(
      applicationId: ApplicationId
    )(
      block: ApplicationRequest[AnyContent] => Future[Result]
    ): Action[AnyContent] =
    Action.async { implicit request =>
      (
        refiner andThen
          applicationRequestRefiner(applicationId)
      )
        .invokeBlock(request, block)
    }

  protected def roleWithApplicationAndSubmission(
      refiner: ActionRefiner[MessagesRequest, LoggedInRequest]
    )(
      applicationId: ApplicationId
    )(
      block: MarkedSubmissionApplicationRequest[AnyContent] => Future[Result]
    ): Action[AnyContent] =
    Action.async { implicit request =>
      (
        refiner andThen
          applicationRequestRefiner(applicationId) andThen
          applicationSubmissionRefiner
      )
        .invokeBlock(request, block)
    }

  protected def roleWithApplicationAndSubmissionAndInstance(
      refiner: ActionRefiner[MessagesRequest, LoggedInRequest]
    )(
      applicationId: ApplicationId,
      index: Int
    )(
      block: SubmissionInstanceApplicationRequest[AnyContent] => Future[Result]
    ): Action[AnyContent] =
    Action.async { implicit request =>
      (
        refiner andThen
          applicationRequestRefiner(applicationId) andThen
          applicationSubmissionRefiner andThen
          submissionInstanceRefiner(index)
      )
        .invokeBlock(request, block)
    }
}
