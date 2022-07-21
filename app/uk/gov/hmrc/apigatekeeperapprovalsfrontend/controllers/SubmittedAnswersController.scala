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
import uk.gov.hmrc.apiplatform.modules.gkauth.services.StrideAuthorisationService
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.config.ErrorHandler
import scala.concurrent.ExecutionContext
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.ApplicationId
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.services.ApplicationActionService
import uk.gov.hmrc.apiplatform.modules.submissions.services.SubmissionService

import scala.concurrent.Future.successful
import play.api.mvc._
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.views.html.SubmittedAnswersPage
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.services.SubmissionQuestionsAndAnswers._
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.services.SubmissionQuestionsAndAnswers


object SubmittedAnswersController {  
  case class ViewModel(
    appName: String,
    applicationId: ApplicationId,
    index: Int,
    questionAnswerGroups: List[QuestionAndAnswerGroup],
    isGranted: Boolean
  )
}

@Singleton
class SubmittedAnswersController @Inject()(
  strideAuthorisationService: StrideAuthorisationService,
  mcc: MessagesControllerComponents,
  errorHandler: ErrorHandler,
  submittedAnswersPage: SubmittedAnswersPage,
  val applicationActionService: ApplicationActionService,
  val submissionService: SubmissionService
)(implicit override val ec: ExecutionContext) extends AbstractApplicationController(strideAuthorisationService, mcc, errorHandler) {
  
  import SubmittedAnswersController._

  def page(applicationId: ApplicationId, index: Int) = loggedInWithApplicationAndSubmissionAndInstance(applicationId, index) { implicit request =>
    val appName = request.application.name
    val submission = request.submission
    val instance = request.instance

    successful(Ok(submittedAnswersPage(ViewModel(appName, applicationId, index, SubmissionQuestionsAndAnswers(submission, index), instance.isGranted))))
  }
}
