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

import java.time.temporal.ChronoUnit
import scala.concurrent.ExecutionContext.Implicits.global

import cats.data.NonEmptyList
import org.mockito.captor.ArgCaptor

import play.api.http.Status
import play.api.test.Helpers._
import uk.gov.hmrc.apiplatform.modules.applications.access.domain.models.{Access, SellResellOrDistribute}
import uk.gov.hmrc.apiplatform.modules.applications.common.domain.models.FullName
import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.{ApplicationState, State}
import uk.gov.hmrc.apiplatform.modules.applications.submissions.domain.models.{ImportantSubmissionData, PrivacyPolicyLocations, ResponsibleIndividual, TermsAndConditionsLocations}
import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress
import uk.gov.hmrc.apiplatform.modules.gkauth.domain.models.GatekeeperRoles
import uk.gov.hmrc.apiplatform.modules.gkauth.services.{LdapAuthorisationServiceMockModule, StrideAuthorisationServiceMockModule}
import uk.gov.hmrc.apiplatform.modules.submissions.domain.models.Submission

import uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.SendNewTermsOfUseController.ViewModel
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.views.html.{SendNewTermsOfUseConfirmPage, SendNewTermsOfUseRequestedPage}

class SendNewTermsOfUseControllerSpec extends AbstractControllerSpec {

  trait Setup extends AbstractSetup
      with StrideAuthorisationServiceMockModule
      with LdapAuthorisationServiceMockModule {

    val confirmPage     = mock[SendNewTermsOfUseConfirmPage]
    val requestedPage   = mock[SendNewTermsOfUseRequestedPage]
    when(confirmPage.apply(*[ViewModel])(*, *)).thenReturn(play.twirl.api.HtmlFormat.empty)
    when(requestedPage.apply(*[ViewModel])(*, *)).thenReturn(play.twirl.api.HtmlFormat.empty)
    val viewModelCaptor = ArgCaptor[ViewModel]

    val controller = new SendNewTermsOfUseController(
      config,
      StrideAuthorisationServiceMock.aMock,
      mcc,
      errorHandler,
      confirmPage,
      requestedPage,
      ApplicationActionServiceMock.aMock,
      SubmissionServiceMock.aMock
    )

    val requesterEmail     = "test@example.com"
    val submittedTimestamp = instant
    val declinedTimestamp  = instant.minus(5, ChronoUnit.DAYS)
    val grantedTimestamp   = instant.minus(7, ChronoUnit.DAYS)

    def markedSubmissionWithStatusHistoryOf(statuses: Submission.Status*) = {
      val latestInstance = markedSubmission.submission.latestInstance.copy(statusHistory = NonEmptyList.fromList(statuses.toList).get)
      markedSubmission.copy(submission = markedSubmission.submission.copy(instances = NonEmptyList.of(latestInstance)))
    }
    val responsibleIndividual                                             = ResponsibleIndividual(FullName("Bob Example"), LaxEmailAddress("bob@example.com"))

    val appWithImportantData = anApplication(applicationId).copy(
      access = Access.Standard(
        List.empty,
        None,
        None,
        Set.empty,
        Some(SellResellOrDistribute("Yes")),
        Some(ImportantSubmissionData(None, responsibleIndividual, Set.empty, TermsAndConditionsLocations.InDesktopSoftware, PrivacyPolicyLocations.InDesktopSoftware, List.empty))
      ),
      state = ApplicationState(State.PENDING_GATEKEEPER_APPROVAL, None, None, None, instant)
    )
  }

  "page" should {
    "return 200 when standard app" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      ApplicationActionServiceMock.Process.thenReturn(appWithImportantData.copy(state = ApplicationState(State.PRODUCTION, None, None, None, instant)))
      SubmissionServiceMock.FetchLatestSubmission.thenNotFound()
      SubmissionServiceMock.FetchTermsOfUseInvitation.thenNotFound()

      val result = controller.page(applicationId)(fakeRequest)
      status(result) shouldBe Status.OK

      verify(confirmPage).apply(viewModelCaptor)(*, *)
      viewModelCaptor.value.applicationId shouldBe appWithImportantData.id
      viewModelCaptor.value.appName shouldBe appWithImportantData.name
    }

    "return 400 when app already has submissions" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      ApplicationActionServiceMock.Process.thenReturn(appWithImportantData.copy(state = ApplicationState(State.PRODUCTION, None, None, None, instant)))
      SubmissionServiceMock.FetchLatestSubmission.thenReturn(appWithImportantData.id)
      SubmissionServiceMock.FetchTermsOfUseInvitation.thenNotFound()

      val result = controller.page(applicationId)(fakeRequest)
      status(result) shouldBe Status.BAD_REQUEST
      contentAsString(result) should include("Application already invited")
    }

    "return 400 when app already invited" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      ApplicationActionServiceMock.Process.thenReturn(appWithImportantData.copy(state = ApplicationState(State.PRODUCTION, None, None, None, instant)))
      SubmissionServiceMock.FetchLatestSubmission.thenNotFound()
      SubmissionServiceMock.FetchTermsOfUseInvitation.thenReturn(appWithImportantData.id)

      val result = controller.page(applicationId)(fakeRequest)
      status(result) shouldBe Status.BAD_REQUEST
      contentAsString(result) should include("Application already invited")
    }

    "return 400 when app not in Production" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      ApplicationActionServiceMock.Process.thenReturn(appWithImportantData)

      val result = controller.page(applicationId)(fakeRequest)
      status(result) shouldBe Status.BAD_REQUEST
      contentAsString(result) should include("Invalid application status")
    }

    "return 400 when app not Standard" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      ApplicationActionServiceMock.Process.thenReturn(appWithImportantData.copy(access = Access.Privileged()))

      val result = controller.page(applicationId)(fakeRequest)
      status(result) shouldBe Status.BAD_REQUEST
      contentAsString(result) should include("Invalid application access type")
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

  "action" should {
    "return OK when yes selected" in new Setup {
      val fakeYesRequest = fakeRequest.withFormUrlEncodedBody("invite-admins" -> "yes")

      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(applicationId)
      SubmissionServiceMock.TermsOfUseInvite.thenReturn(applicationId, application)

      val result = controller.action(applicationId)(fakeYesRequest)

      status(result) shouldBe Status.OK
    }

    "return 400 when yes selected and error calling TPA" in new Setup {
      val fakeYesRequest = fakeRequest.withFormUrlEncodedBody("invite-admins" -> "yes")

      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(applicationId)
      SubmissionServiceMock.TermsOfUseInvite.thenReturnError(applicationId)

      val result = controller.action(applicationId)(fakeYesRequest)

      status(result) shouldBe Status.BAD_REQUEST
      contentAsString(result) should include("Error inviting for terms of use")
    }

    "redirect when no selected" in new Setup {
      val fakeNoRequest = fakeRequest.withFormUrlEncodedBody("invite-admins" -> "no")

      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(applicationId)

      val result = controller.action(applicationId)(fakeNoRequest)

      status(result) shouldBe Status.SEE_OTHER
    }
  }
}
