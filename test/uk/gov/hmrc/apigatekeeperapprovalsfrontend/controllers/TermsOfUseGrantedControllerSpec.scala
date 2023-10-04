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

import scala.concurrent.ExecutionContext.Implicits.global

import cats.data.NonEmptyList
import org.joda.time.{DateTime, Days}
import org.mockito.captor.ArgCaptor

import play.api.http.Status
import play.api.test.Helpers._
import uk.gov.hmrc.apiplatform.modules.applications.domain.models.{PrivacyPolicyLocations, TermsAndConditionsLocations}
import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress
import uk.gov.hmrc.apiplatform.modules.gkauth.domain.models.GatekeeperRoles
import uk.gov.hmrc.apiplatform.modules.gkauth.services.{LdapAuthorisationServiceMockModule, StrideAuthorisationServiceMockModule}
import uk.gov.hmrc.apiplatform.modules.submissions.domain.models.Submission

import uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.TermsOfUseGrantedController.ViewModel
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models._
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.views.html.TermsOfUseGrantedPage

class TermsOfUseGrantedControllerSpec extends AbstractControllerSpec {

  trait Setup extends AbstractSetup
      with StrideAuthorisationServiceMockModule
      with LdapAuthorisationServiceMockModule {

    val firstPage       = mock[TermsOfUseGrantedPage]
    when(firstPage.apply(*[ViewModel])(*, *)).thenReturn(play.twirl.api.HtmlFormat.empty)
    val viewModelCaptor = ArgCaptor[ViewModel]

    val controller = new TermsOfUseGrantedController(
      config,
      StrideAuthorisationServiceMock.aMock,
      mcc,
      errorHandler,
      firstPage,
      ApplicationActionServiceMock.aMock,
      SubmissionServiceMock.aMock
    )

    val requesterEmail     = "test@example.com"
    val submittedTimestamp = DateTime.now()
    val declinedTimestamp  = DateTime.now().minus(Days.days(5))
    val grantedTimestamp   = DateTime.now().minus(Days.days(7))

    def markedSubmissionWithStatusHistoryOf(statuses: Submission.Status*) = {
      val latestInstance = markedSubmission.submission.latestInstance.copy(statusHistory = NonEmptyList.fromList(statuses.toList).get)
      markedSubmission.copy(submission = markedSubmission.submission.copy(instances = NonEmptyList.of(latestInstance)))
    }
    val responsibleIndividual                                             = ResponsibleIndividual("Bob Example", LaxEmailAddress("bob@example.com"))

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
    "return 200 when standard app" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      ApplicationActionServiceMock.Process.thenReturn(appWithImportantData)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(applicationId)

      val result = controller.page(applicationId)(fakeRequest)
      status(result) shouldBe Status.OK

      verify(firstPage).apply(viewModelCaptor)(*, *)
      viewModelCaptor.value.applicationId shouldBe appWithImportantData.id
      viewModelCaptor.value.appName shouldBe appWithImportantData.name
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
      val fakeYesRequest = fakeRequest.withFormUrlEncodedBody("grant" -> "yes")

      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(applicationId)

      val result = controller.action(applicationId)(fakeYesRequest)

      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result).value shouldBe uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.routes.TermsOfUseNotesController.page(applicationId).url
    }

    "redirect when no selected" in new Setup {
      val fakeNoRequest = fakeRequest.withFormUrlEncodedBody("grant" -> "no")

      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(applicationId)

      val result = controller.action(applicationId)(fakeNoRequest)

      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result).value shouldBe uk.gov.hmrc.apigatekeeperapprovalsfrontend.controllers.routes.TermsOfUseInvitationController.page.url
    }
  }
}
