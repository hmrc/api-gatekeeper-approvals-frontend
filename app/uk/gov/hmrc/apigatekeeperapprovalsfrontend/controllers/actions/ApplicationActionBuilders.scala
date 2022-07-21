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

import play.api.mvc._

import uk.gov.hmrc.apigatekeeperapprovalsfrontend.config.ErrorHandler
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.models.ApplicationRequest
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.models.MarkedSubmissionApplicationRequest
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models._
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.services.ApplicationActionService
import uk.gov.hmrc.apiplatform.modules.gkauth.controllers.GatekeeperBaseController
import uk.gov.hmrc.apiplatform.modules.gkauth.domain.models._
import uk.gov.hmrc.apiplatform.modules.submissions.services.SubmissionService
import uk.gov.hmrc.apiplatform.modules.common.services.EitherTHelper
import scala.concurrent.Future.successful
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.models.SubmissionInstanceApplicationRequest

trait ApplicationActionBuilders {
  self: GatekeeperBaseController =>
  
  def errorHandler: ErrorHandler
  def applicationActionService: ApplicationActionService
  def submissionService: SubmissionService

  val E = EitherTHelper.make[Result]

  def applicationRequestRefiner(applicationId: ApplicationId)(implicit ec: ExecutionContext): ActionRefiner[LoggedInRequest, ApplicationRequest] = {
    new ActionRefiner[LoggedInRequest, ApplicationRequest] {
      override protected def executionContext: ExecutionContext = ec

      override def refine[A](request: LoggedInRequest[A]): Future[Either[Result, ApplicationRequest[A]]] = {
        implicit val implicitRequest: Request[A] = request
        import cats.implicits._

        applicationActionService.process(applicationId, request)
        .toRight(NotFound(errorHandler.notFoundTemplate(Request(request, request.messagesApi)))).value
      }
    }
  }

  def applicationSubmissionRefiner(implicit ec: ExecutionContext): ActionRefiner[ApplicationRequest, MarkedSubmissionApplicationRequest] =
    new ActionRefiner[ApplicationRequest, MarkedSubmissionApplicationRequest] {
      override def executionContext = ec
      override def refine[A](request: ApplicationRequest[A]): Future[Either[Result, MarkedSubmissionApplicationRequest[A]]] = {
        implicit val implicitRequest: MessagesRequest[A] = request
        
        (
          for {
            submission <- E.fromOptionF(submissionService.fetchLatestMarkedSubmission(request.application.id), NotFound(errorHandler.notFoundTemplate(request)) )
          } yield new MarkedSubmissionApplicationRequest(submission, request)
        )
        .value
      }
    }

    def submissionInstanceRefiner(index: Int)(implicit ec: ExecutionContext): ActionRefiner[MarkedSubmissionApplicationRequest, SubmissionInstanceApplicationRequest] =
      new ActionRefiner[MarkedSubmissionApplicationRequest, SubmissionInstanceApplicationRequest] {
        override def executionContext = ec
        
        override def refine[A](request: MarkedSubmissionApplicationRequest[A]): Future[Either[Result, SubmissionInstanceApplicationRequest[A]]] = {          
          successful(request.submission.instances.find(_.index == index) match {
            case Some(instance) => Right(new SubmissionInstanceApplicationRequest(request.markedSubmission, instance, request))
            case None => Left(BadRequest(s"No submission with index $index found for application"))
          })
        }
      }
    }

