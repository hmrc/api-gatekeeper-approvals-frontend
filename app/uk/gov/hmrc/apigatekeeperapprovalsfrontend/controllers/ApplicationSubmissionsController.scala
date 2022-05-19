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

package uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.apiplatform.modules.stride.config.StrideAuthConfig
import uk.gov.hmrc.apiplatform.modules.stride.connectors.AuthConnector
import uk.gov.hmrc.apiplatform.modules.stride.controllers.actions.ForbiddenHandler
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.config.{ErrorHandler, GatekeeperConfig}

import scala.concurrent.ExecutionContext
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.ApplicationId
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.views.html.ApplicationSubmissionsPage
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.services.ApplicationActionService
import uk.gov.hmrc.apiplatform.modules.submissions.domain.models._
import uk.gov.hmrc.apiplatform.modules.submissions.services.SubmissionService

import scala.concurrent.Future.successful
import play.api.mvc._
import uk.gov.hmrc.apiplatform.modules.submissions.domain.models.Submission.Status.Submitted


object ApplicationSubmissionsController {

  case class CurrentSubmittedInstanceDetails(requesterEmail: String, timestamp: String)

  case class DeclinedInstanceDetails(timestamp: String, index: Int)

  case class GrantedInstanceDetails(timestamp: String)
  
  case class ViewModel(
    applicationId: ApplicationId,
    appName: String,
    applicationDetailsUrl: String,
    currentSubmission: Option[CurrentSubmittedInstanceDetails],
    declinedInstances: List[DeclinedInstanceDetails],
    grantedInstance: Option[GrantedInstanceDetails],
    responsibleIndividualEmail: Option[String]
  )
}

@Singleton
class ApplicationSubmissionsController @Inject()(
  config: GatekeeperConfig,
  strideAuthConfig: StrideAuthConfig,
  authConnector: AuthConnector,
  forbiddenHandler: ForbiddenHandler,
  mcc: MessagesControllerComponents,
  applicationSubmissionsPage: ApplicationSubmissionsPage,
  errorHandler: ErrorHandler,
  val applicationActionService: ApplicationActionService,
  val submissionService: SubmissionService
)(implicit override val ec: ExecutionContext) extends AbstractApplicationController(strideAuthConfig, authConnector, forbiddenHandler, mcc, errorHandler) {
  
  import ApplicationSubmissionsController._
  import cats.data.OptionT
  import cats.implicits._

  def whichPage(applicationId: ApplicationId): Action[AnyContent] = loggedInWithApplication(applicationId) { implicit request =>
    val gatekeeperApplicationUrl = s"${config.applicationsPageUri}/${applicationId.value}"

    val hasEverBeenSubmitted: Submission => Boolean = submission => submission.instances.find(i => i.isSubmitted || i.isGranted || i.isGrantedWithWarnings || i.isDeclined).nonEmpty

    (
      for {
        submission <- OptionT(submissionService.fetchLatestSubmission(request.application.id))
        if hasEverBeenSubmitted(submission)
      } yield submission
    )
    .fold(
      Redirect(gatekeeperApplicationUrl)
    )(
      _ => Redirect(uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.routes.ApplicationSubmissionsController.page(applicationId))
    )
  }

  def page(applicationId: ApplicationId): Action[AnyContent] = loggedInWithApplicationAndSubmission(applicationId) { implicit request =>
    val appName = request.application.name
    val gatekeeperApplicationUrl = s"${config.applicationsPageUri}/${applicationId.value}"

    val latestInstance = request.markedSubmission.submission.latestInstance
    val latestInstanceStatus = latestInstance.statusHistory.head
    val currentSubmission = latestInstanceStatus match {
        case status: Submitted => Some(CurrentSubmittedInstanceDetails(status.requestedBy, status.timestamp.asText))
        case _ => None
      }

    val declinedSubmissions =
      request.markedSubmission.submission.instances.filter(i => i.statusHistory.head.isDeclined)
      .map(i => DeclinedInstanceDetails(i.statusHistory.head.timestamp.asText, i.index))

    val grantedInstance =
      if(latestInstanceStatus.isGranted || latestInstanceStatus.isGrantedWithWarnings)
        Some(GrantedInstanceDetails(latestInstanceStatus.timestamp.asText))
      else
        None

    val responsibleIndividualEmail =
      request.application.importantSubmissionData.map(i => i.responsibleIndividual.emailAddress)

    successful(Ok(applicationSubmissionsPage(ViewModel(applicationId, appName, gatekeeperApplicationUrl, currentSubmission, declinedSubmissions, grantedInstance, responsibleIndividualEmail))))
  }
}
