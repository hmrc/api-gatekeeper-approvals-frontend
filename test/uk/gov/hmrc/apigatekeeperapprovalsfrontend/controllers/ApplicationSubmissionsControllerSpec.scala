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

import java.time.Duration
import scala.concurrent.ExecutionContext.Implicits.global

import cats.data.NonEmptyList

import play.api.http.Status
import play.api.test.Helpers._

import uk.gov.hmrc.apiplatform.modules.applications.common.domain.models.FullName
import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.{ApplicationStateData, ApplicationWithCollaboratorsFixtures}
import uk.gov.hmrc.apiplatform.modules.applications.submissions.domain.models.ResponsibleIndividual
import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress
import uk.gov.hmrc.apiplatform.modules.common.utils.FixedClock
import uk.gov.hmrc.apiplatform.modules.gkauth.domain.models.GatekeeperRoles
import uk.gov.hmrc.apiplatform.modules.gkauth.services.{LdapAuthorisationServiceMockModule, StrideAuthorisationServiceMockModule}
import uk.gov.hmrc.apiplatform.modules.submissions.domain.models.Submission
import uk.gov.hmrc.apiplatform.modules.submissions.domain.models.Submission.Status.{Declined, Granted, Submitted}
import uk.gov.hmrc.apiplatform.modules.submissions.services.SubmissionReviewServiceMockModule

import uk.gov.hmrc.apigatekeeperapprovalsfrontend.views.html.ApplicationSubmissionsPage

class ApplicationSubmissionsControllerSpec extends AbstractControllerSpec with ApplicationWithCollaboratorsFixtures {

  trait Setup extends AbstractSetup
      with SubmissionReviewServiceMockModule
      with StrideAuthorisationServiceMockModule
      with LdapAuthorisationServiceMockModule
      with FixedClock {

    val page = app.injector.instanceOf[ApplicationSubmissionsPage]

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
    val submittedTimestamp = instant
    val declinedTimestamp  = instant.minus(Duration.ofDays(5))
    val grantedTimestamp   = instant.minus(Duration.ofDays(7))

    def markedSubmissionWithStatusHistoryOf(statuses: Submission.Status*) = {
      val latestInstance = markedSubmission.submission.latestInstance.copy(statusHistory = NonEmptyList.fromList(statuses.toList).get)
      markedSubmission.copy(submission = markedSubmission.submission.copy(instances = NonEmptyList.of(latestInstance)))
    }
    val responsibleIndividual                                             = ResponsibleIndividual(FullName("Bob Example"), LaxEmailAddress("bob@example.com"))

    val appWithImportantData =
      standardApp
        .withAccess(standardAccess.withDesktopSoftware)
        .withState(ApplicationStateData.pendingGatekeeperApproval)
  }

  "page" should {
    "return 200 when submitted app with no previous declines" in new Setup {

      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      ApplicationActionServiceMock.Process.thenReturn(appWithImportantData.withState(ApplicationStateData.pendingRIVerification))
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(markedSubmissionWithStatusHistoryOf(Submitted(submittedTimestamp, requesterEmail)))

      val result = controller.page(applicationId)(fakeRequest)
      status(result) shouldBe Status.OK
      contentAsString(result) should include(appWithImportantData.name.value)
      contentAsString(result) should include("The responsible individual has not verified yet, so you can only decline this request.")
      contentAsString(result) shouldNot include("Production access granted")
      contentAsString(result) shouldNot include("Previously declined")
      contentAsString(result) shouldNot include("This application has been deleted")
      contentAsString(result) should include("Decline request")
      contentAsString(result) shouldNot include("Check the request")
      contentAsString(result) should include(requesterEmail)
    }

    "return 200 when no current submission but with previous declines" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      ApplicationActionServiceMock.Process.thenReturn(appWithImportantData)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(markedSubmissionWithStatusHistoryOf(
        Declined(declinedTimestamp, requesterEmail, "reasons")
      ))

      val result = controller.page(applicationId)(fakeRequest)
      status(result) shouldBe Status.OK
      contentAsString(result) should include(appWithImportantData.name.value)
      contentAsString(result) shouldNot include("Production access granted")
      contentAsString(result) should include("Previously declined")
      contentAsString(result) shouldNot include("This application has been deleted")
      contentAsString(result) shouldNot include("Decline request")
      contentAsString(result) should include("Check")
      contentAsString(result) should include(formatDMY(declinedTimestamp))
    }

    "return 200 when submission has been granted" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(markedSubmissionWithStatusHistoryOf(
        Granted(grantedTimestamp, requesterEmail, None, None)
      ))

      val result = controller.page(applicationId)(fakeRequest)
      status(result) shouldBe Status.OK
      contentAsString(result) should include(appWithImportantData.name.value)
      contentAsString(result) should include("Production access granted")
      contentAsString(result) shouldNot include("Previously declined")
      contentAsString(result) shouldNot include("This application has been deleted")
      contentAsString(result) shouldNot include("Decline request")
      contentAsString(result) should include("Check")
      contentAsString(result) should include(formatDMY(grantedTimestamp))
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
