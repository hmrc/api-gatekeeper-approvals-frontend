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

import cats.data.NonEmptyList
import org.joda.time.{DateTime, Days}
import org.mockito.captor.ArgCaptor

import scala.concurrent.ExecutionContext.Implicits.global
import play.api.http.Status
import play.api.test.Helpers._
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.ApplicationSubmissionsController.{CurrentSubmittedInstanceDetails, DeclinedInstanceDetails, GrantedInstanceDetails, ViewModel}
import uk.gov.hmrc.apiplatform.modules.submissions.services.SubmissionReviewServiceMockModule
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.views.html.ApplicationSubmissionsPage
import uk.gov.hmrc.apiplatform.modules.submissions.domain.models.Submission
import uk.gov.hmrc.apiplatform.modules.submissions.domain.models.Submission.Status.{Declined, Granted, Submitted}
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.ImportantSubmissionData
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.TermsAndConditionsLocation
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.PrivacyPolicyLocation
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.Standard
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.ResponsibleIndividual
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.ApplicationState
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.State


class ApplicationSubmissionsControllerSpec extends AbstractControllerSpec {
  trait Setup extends AbstractSetup
      with SubmissionReviewServiceMockModule {
        
    val page = mock[ApplicationSubmissionsPage]
    when(page.apply(*[ViewModel])(*,*)).thenReturn(play.twirl.api.HtmlFormat.empty)
    val viewModelCaptor = ArgCaptor[ViewModel]

    val controller = new ApplicationSubmissionsController(
      config,
      strideAuthConfig,
      AuthConnectorMock.aMock,
      forbiddenHandler,
      mcc,
      page,
      errorHandler,
      ApplicationActionServiceMock.aMock,
      SubmissionServiceMock.aMock,
    )

    val requesterEmail = "test@example.com"
    val submittedTimestamp = DateTime.now()
    val declinedTimestamp = DateTime.now().minus(Days.days(5))
    val grantedTimestamp = DateTime.now().minus(Days.days(7))
    def markedSubmissionWithStatusHistoryOf(statuses: Submission.Status*) = {
      val latestInstance = markedSubmission.submission.latestInstance.copy(statusHistory = NonEmptyList.fromList(statuses.toList).get)
      markedSubmission.copy(submission = markedSubmission.submission.copy(instances = NonEmptyList.of(latestInstance)))
    }
    val responsibleIndividual = ResponsibleIndividual("Bob Example", "bob@example.com")
    val appWithImportantData = anApplication(applicationId).copy(
          access = Standard(List.empty, Some(ImportantSubmissionData(None, responsibleIndividual, Set.empty, TermsAndConditionsLocation.InDesktopSoftware, PrivacyPolicyLocation.InDesktopSoftware, List.empty))),
          state = ApplicationState(name = State.PENDING_GATEKEEPER_APPROVAL)
        )
  }

  "page" should {
    "return 200 when submitted app with no previous declines" in new Setup {
      AuthConnectorMock.Authorise.thenReturn()
      ApplicationActionServiceMock.Process.thenReturn(appWithImportantData.copy(state = ApplicationState(name = State.PENDING_RESPONSIBLE_INDIVIDUAL_VERIFICATION)))
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(markedSubmissionWithStatusHistoryOf(Submitted(submittedTimestamp, requesterEmail)))

      val result = controller.page(applicationId)(fakeRequest)
      status(result) shouldBe Status.OK

      verify(page).apply(viewModelCaptor)(*, *)
      viewModelCaptor.value.currentSubmission shouldBe Some(CurrentSubmittedInstanceDetails(requesterEmail, submittedTimestamp.toString("dd MMMM yyyy")))
      viewModelCaptor.value.declinedInstances shouldBe List()
      viewModelCaptor.value.grantedInstance shouldBe None
      viewModelCaptor.value.responsibleIndividualEmail shouldBe Some("bob@example.com")
      viewModelCaptor.value.pendingResponsibleIndividualVerification shouldBe true
    }

    "return 200 when no current submission but with previous declines" in new Setup {
      AuthConnectorMock.Authorise.thenReturn()
      ApplicationActionServiceMock.Process.thenReturn(appWithImportantData)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(markedSubmissionWithStatusHistoryOf(
        Declined(declinedTimestamp, requesterEmail, "reasons")
      ))

      val result = controller.page(applicationId)(fakeRequest)
      status(result) shouldBe Status.OK

      verify(page).apply(viewModelCaptor)(*, *)
      viewModelCaptor.value.currentSubmission shouldBe None
      viewModelCaptor.value.grantedInstance shouldBe None
      viewModelCaptor.value.responsibleIndividualEmail shouldBe Some("bob@example.com")
      viewModelCaptor.value.pendingResponsibleIndividualVerification shouldBe false
      viewModelCaptor.value.declinedInstances shouldBe List(
        DeclinedInstanceDetails(declinedTimestamp.toString("dd MMMM yyyy"), 0)
      )
    }

    "return 200 when submission has been granted" in new Setup {
      AuthConnectorMock.Authorise.thenReturn()
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(markedSubmissionWithStatusHistoryOf(
        Granted(grantedTimestamp, requesterEmail)
      ))

      val result = controller.page(applicationId)(fakeRequest)
      status(result) shouldBe Status.OK

      verify(page).apply(viewModelCaptor)(*, *)
      viewModelCaptor.value.currentSubmission shouldBe None
      viewModelCaptor.value.declinedInstances shouldBe List()
      viewModelCaptor.value.grantedInstance shouldBe Some(
        GrantedInstanceDetails(grantedTimestamp.toString("dd MMMM yyyy"))
      )
    }

    "return 404 if no marked application is found" in new Setup {
      AuthConnectorMock.Authorise.thenReturn()
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenNotFound()

      val result = controller.page(applicationId)(fakeRequest)
      status(result) shouldBe Status.NOT_FOUND
    }

    "return 404 if no application is found" in new Setup {
      AuthConnectorMock.Authorise.thenReturn()
      ApplicationActionServiceMock.Process.thenNotFound()

      val result = controller.page(applicationId)(fakeRequest)
      status(result) shouldBe Status.NOT_FOUND
    }

    "return 403 for InsufficientEnrolments" in new Setup {
      AuthConnectorMock.Authorise.thenReturnInsufficientEnrolments()
      val result = controller.page(applicationId)(fakeRequest)
      status(result) shouldBe Status.FORBIDDEN
    }
    
    "return 303 for SessionRecordNotFound" in new Setup {
      AuthConnectorMock.Authorise.thenReturnSessionRecordNotFound()
      val result = controller.page(applicationId)(fakeRequest)
      status(result) shouldBe Status.SEE_OTHER
    }  
  }

  "whichPage" should {
    "redirect to index page when submission found and hasEverBeenSubmitted is true" in new Setup {
      AuthConnectorMock.Authorise.thenReturn()
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestSubmission.thenReturnHasBeenSubmitted(applicationId)

      val result = controller.whichPage(applicationId)(fakeRequest)
      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some(s"/api-gatekeeper-approvals/applications/${applicationId.value}/reviews")
    }

    "redirect to Gatekeeper when submission found but hasEverBeenSubmitted is false" in new Setup {
      AuthConnectorMock.Authorise.thenReturn()
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestSubmission.thenReturn(applicationId)

      val result = controller.whichPage(applicationId)(fakeRequest)
      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some(s"http://localhost:9684/api-gatekeeper/applications/${applicationId.value}")
    }

    "redirect to Gatekeeper when no submission found" in new Setup {
      AuthConnectorMock.Authorise.thenReturn()
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestSubmission.thenNotFound()

      val result = controller.whichPage(applicationId)(fakeRequest)
      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some(s"http://localhost:9684/api-gatekeeper/applications/${applicationId.value}")
    }
  }
}
