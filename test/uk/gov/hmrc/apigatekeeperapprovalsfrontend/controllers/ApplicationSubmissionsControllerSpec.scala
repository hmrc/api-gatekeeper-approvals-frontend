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

package uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers

import java.time.format.DateTimeFormatter
import java.time.{LocalDateTime, ZoneOffset}
import scala.concurrent.ExecutionContext.Implicits.global

import cats.data.NonEmptyList
import org.mockito.captor.ArgCaptor

import play.api.http.Status
import play.api.test.Helpers._
import uk.gov.hmrc.apiplatform.modules.applications.domain.models.{PrivacyPolicyLocations, TermsAndConditionsLocations}
import uk.gov.hmrc.apiplatform.modules.gkauth.domain.models.GatekeeperRoles
import uk.gov.hmrc.apiplatform.modules.gkauth.services.{LdapAuthorisationServiceMockModule, StrideAuthorisationServiceMockModule}
import uk.gov.hmrc.apiplatform.modules.submissions.domain.models.Submission
import uk.gov.hmrc.apiplatform.modules.submissions.domain.models.Submission.Status.{Declined, Granted, Submitted}
import uk.gov.hmrc.apiplatform.modules.submissions.services.SubmissionReviewServiceMockModule

import uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.ApplicationSubmissionsController.{
  CurrentSubmittedInstanceDetails,
  DeclinedInstanceDetails,
  GrantedInstanceDetails,
  ViewModel
}
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models._
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.views.html.ApplicationSubmissionsPage

class ApplicationSubmissionsControllerSpec extends AbstractControllerSpec {

  trait Setup extends AbstractSetup
      with SubmissionReviewServiceMockModule
      with StrideAuthorisationServiceMockModule
      with LdapAuthorisationServiceMockModule {

    val page            = mock[ApplicationSubmissionsPage]
    when(page.apply(*[ViewModel])(*, *)).thenReturn(play.twirl.api.HtmlFormat.empty)
    val viewModelCaptor = ArgCaptor[ViewModel]

    val controller = new ApplicationSubmissionsController(
      config,
      StrideAuthorisationServiceMock.aMock,
      LdapAuthorisationServiceMock.aMock,
      mcc,
      page,
      errorHandler,
      ApplicationActionServiceMock.aMock,
      SubmissionServiceMock.aMock
    )

    val requesterEmail     = "test@example.com"
    val submittedTimestamp = LocalDateTime.now(ZoneOffset.UTC)
    val declinedTimestamp  = LocalDateTime.now(ZoneOffset.UTC).minusDays(5)
    val grantedTimestamp   = LocalDateTime.now(ZoneOffset.UTC).minusDays(7)

    def markedSubmissionWithStatusHistoryOf(statuses: Submission.Status*) = {
      val latestInstance = markedSubmission.submission.latestInstance.copy(statusHistory = NonEmptyList.fromList(statuses.toList).get)
      markedSubmission.copy(submission = markedSubmission.submission.copy(instances = NonEmptyList.of(latestInstance)))
    }
    val responsibleIndividual                                             = ResponsibleIndividual("Bob Example", "bob@example.com")

    val appWithImportantData = anApplication(applicationId).copy(
      access = Standard(
        List.empty,
        Some(SellResellOrDistribute("Yes")),
        Some(ImportantSubmissionData(None, responsibleIndividual, Set.empty, TermsAndConditionsLocations.InDesktopSoftware, PrivacyPolicyLocations.InDesktopSoftware, List.empty))
      ),
      state = ApplicationState(name = State.PENDING_GATEKEEPER_APPROVAL)
    )
  }

  "page" should {
    "return 200 when submitted app with no previous declines" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      ApplicationActionServiceMock.Process.thenReturn(appWithImportantData.copy(state = ApplicationState(name = State.PENDING_RESPONSIBLE_INDIVIDUAL_VERIFICATION)))
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(markedSubmissionWithStatusHistoryOf(Submitted(submittedTimestamp, requesterEmail)))

      val result = controller.page(applicationId)(fakeRequest)
      status(result) shouldBe Status.OK

      verify(page).apply(viewModelCaptor)(*, *)
      viewModelCaptor.value.currentSubmission shouldBe Some(CurrentSubmittedInstanceDetails(requesterEmail, DateTimeFormatter.ofPattern("dd MMMM yyyy").format(submittedTimestamp)))
      viewModelCaptor.value.declinedInstances shouldBe List()
      viewModelCaptor.value.grantedInstance shouldBe None
      viewModelCaptor.value.responsibleIndividualEmail shouldBe Some("bob@example.com")
      viewModelCaptor.value.pendingResponsibleIndividualVerification shouldBe true
      viewModelCaptor.value.isDeleted shouldBe false
    }

    "return 200 when no current submission but with previous declines" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
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
      viewModelCaptor.value.isDeleted shouldBe false
      viewModelCaptor.value.declinedInstances shouldBe List(
        DeclinedInstanceDetails(DateTimeFormatter.ofPattern("dd MMMM yyyy").format(declinedTimestamp), 0)
      )
    }

    "return 200 when submission has been granted" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(markedSubmissionWithStatusHistoryOf(
        Granted(grantedTimestamp, requesterEmail, None, None)
      ))

      val result = controller.page(applicationId)(fakeRequest)
      status(result) shouldBe Status.OK

      verify(page).apply(viewModelCaptor)(*, *)
      viewModelCaptor.value.currentSubmission shouldBe None
      viewModelCaptor.value.declinedInstances shouldBe List()
      viewModelCaptor.value.grantedInstance shouldBe Some(
        GrantedInstanceDetails(DateTimeFormatter.ofPattern("dd MMMM yyyy").format(grantedTimestamp))
      )
    }

    "return 404 if no marked application is found" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenNotFound()

      val result = controller.page(applicationId)(fakeRequest)
      status(result) shouldBe Status.NOT_FOUND
    }

    "return 404 if no application is found" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      ApplicationActionServiceMock.Process.thenNotFound()

      val result = controller.page(applicationId)(fakeRequest)
      status(result) shouldBe Status.NOT_FOUND
    }

    "return 403 for InsufficientEnrolments" in new Setup {
      StrideAuthorisationServiceMock.Auth.hasInsufficientEnrolments()
      LdapAuthorisationServiceMock.Auth.notAuthorised
      val result = controller.page(applicationId)(fakeRequest)
      status(result) shouldBe Status.FORBIDDEN
    }

    "return 303 for SessionRecordNotFound" in new Setup {
      StrideAuthorisationServiceMock.Auth.sessionRecordNotFound()
      LdapAuthorisationServiceMock.Auth.notAuthorised
      val result = controller.page(applicationId)(fakeRequest)
      status(result) shouldBe Status.SEE_OTHER
    }
  }

  "whichPage" should {
    "redirect to index page when submission found and hasEverBeenSubmitted is true" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestSubmission.thenReturnHasBeenSubmitted(applicationId)

      val result = controller.whichPage(applicationId)(fakeRequest)
      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some(s"/api-gatekeeper-approvals/applications/${applicationId.value}/reviews")
    }

    "redirect to Gatekeeper when submission found but hasEverBeenSubmitted is false" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestSubmission.thenReturn(applicationId)

      val result = controller.whichPage(applicationId)(fakeRequest)
      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some(s"http://localhost:9684/api-gatekeeper/applications/${applicationId.value}")
    }

    "redirect to Gatekeeper when no submission found and authorised using stride" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestSubmission.thenNotFound()

      val result = controller.whichPage(applicationId)(fakeRequest)
      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some(s"http://localhost:9684/api-gatekeeper/applications/${applicationId.value}")
    }

    "redirect to Gatekeeper when no submission found and authorised using ldap" in new Setup {
      StrideAuthorisationServiceMock.Auth.invalidBearerToken()
      LdapAuthorisationServiceMock.Auth.succeeds
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestSubmission.thenNotFound()

      val result = controller.whichPage(applicationId)(fakeRequest)
      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some(s"http://localhost:9684/api-gatekeeper/applications/${applicationId.value}")
    }
  }
}
