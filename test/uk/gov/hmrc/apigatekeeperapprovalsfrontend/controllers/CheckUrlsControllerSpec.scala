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

import play.api.http.Status
import play.api.test.Helpers._

import uk.gov.hmrc.apiplatform.modules.applications.common.domain.models.FullName
import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.ApplicationWithCollaboratorsFixtures
import uk.gov.hmrc.apiplatform.modules.applications.submissions.domain.models._
import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress
import uk.gov.hmrc.apiplatform.modules.gkauth.domain.models.GatekeeperRoles
import uk.gov.hmrc.apiplatform.modules.gkauth.services.StrideAuthorisationServiceMockModule

import uk.gov.hmrc.apigatekeeperapprovalsfrontend.views.html.CheckUrlsPage

class CheckUrlsControllerSpec extends AbstractControllerSpec with ApplicationWithCollaboratorsFixtures {

  trait Setup extends AbstractSetup
      with StrideAuthorisationServiceMockModule {
    val page = app.injector.instanceOf[CheckUrlsPage]

    val controller = new CheckUrlsController(
      StrideAuthorisationServiceMock.aMock,
      mcc,
      SubmissionReviewServiceMock.aMock,
      errorHandler,
      page,
      ApplicationActionServiceMock.aMock,
      SubmissionServiceMock.aMock
    )

    val responsibleIndividual = ResponsibleIndividual(FullName("Bob Example"), LaxEmailAddress("bob@example.com"))

    val appWithImportantData = standardApp
      .withState(appStateTesting)
      .withAccess(standardAccess.withDesktopSoftware)

    def appWithData(
        privacyPolicyLocation: PrivacyPolicyLocation = PrivacyPolicyLocations.InDesktopSoftware,
        termsAndConditionsLocation: TermsAndConditionsLocation = TermsAndConditionsLocations.InDesktopSoftware
      ) = {
      standardApp
        .withState(appStateTesting)
        .withAccess(
          standardAccess
            .withDesktopSoftware
            .withTermsAndConditionsLocation(termsAndConditionsLocation)
            .withPrivacyPolicyLocation(privacyPolicyLocation)
        )
    }
  }

  "checkUrlsPage" should {

    "return 200 for both privacy policy and t&c in desktop" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      ApplicationActionServiceMock.Process.thenReturn(appWithData())
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(applicationIdOne)

      val result = controller.checkUrlsPage(applicationIdOne)(fakeRequest)

      status(result) shouldBe Status.OK
      contentAsString(result) should not include ("This application has been deleted")
    }

    "return 200 for both privacy policy and t&c with URLs" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      ApplicationActionServiceMock.Process.thenReturn(appWithData(PrivacyPolicyLocations.Url("aurl"), TermsAndConditionsLocations.Url("aurl")))
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(applicationIdOne)

      val result = controller.checkUrlsPage(applicationIdOne)(fakeRequest)

      status(result) shouldBe Status.OK
    }

    "return 200 for only privacy policy in desktop" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      ApplicationActionServiceMock.Process.thenReturn(appWithData(PrivacyPolicyLocations.Url("aurl")))
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(applicationIdOne)

      val result = controller.checkUrlsPage(applicationIdOne)(fakeRequest)

      status(result) shouldBe Status.OK
    }

    "return 200 for only terms and conditions in desktop" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      ApplicationActionServiceMock.Process.thenReturn(appWithData(termsAndConditionsLocation = TermsAndConditionsLocations.Url("aurl")))
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(applicationIdOne)

      val result = controller.checkUrlsPage(applicationIdOne)(fakeRequest)

      status(result) shouldBe Status.OK
    }

    "return 200 for both being NoneProvided" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      ApplicationActionServiceMock.Process.thenReturn(appWithData(PrivacyPolicyLocations.NoneProvided, TermsAndConditionsLocations.NoneProvided))
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(applicationIdOne)

      val result = controller.checkUrlsPage(applicationIdOne)(fakeRequest)

      status(result) shouldBe Status.OK
    }

    "return 200 with a deleted application" in new Setup {
      val deletedApp = appWithData().modifyState(_.toDeleted(instant).copy(requestedByName = Some("delete-user@example.com")))
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      ApplicationActionServiceMock.Process.thenReturn(deletedApp)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(applicationIdOne)

      val result = controller.checkUrlsPage(applicationIdOne)(fakeRequest)

      status(result) shouldBe Status.OK
      contentAsString(result) should include("This application has been deleted")
    }

    "return 404" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      ApplicationActionServiceMock.Process.thenReturn(application)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenNotFound()

      val result = controller.checkUrlsPage(applicationIdOne)(fakeRequest)

      status(result) shouldBe Status.NOT_FOUND
    }
  }

  "checkUrlsAction" should {
    "redirect to correct page when marking URLs as checked" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      ApplicationActionServiceMock.Process.thenReturn(appWithImportantData)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(applicationIdOne)
      SubmissionReviewServiceMock.UpdateActionStatus.thenReturn(submissionReview)

      val result = controller.checkUrlsAction(applicationIdOne)(fakeSubmitCheckedRequest)

      status(result) shouldBe SEE_OTHER
    }

    "redirect to correct page when marking URLs as come-back-later" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      ApplicationActionServiceMock.Process.thenReturn(appWithImportantData)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(applicationIdOne)
      SubmissionReviewServiceMock.UpdateActionStatus.thenReturn(submissionReview)

      val result = controller.checkUrlsAction(applicationIdOne)(fakeSubmitComebackLaterRequest)

      status(result) shouldBe SEE_OTHER
    }

    "return bad request when sending an empty submit-action" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      ApplicationActionServiceMock.Process.thenReturn(appWithImportantData)
      SubmissionServiceMock.FetchLatestMarkedSubmission.thenReturn(applicationIdOne)

      val result = controller.checkUrlsAction(applicationIdOne)(fakeRequest)

      status(result) shouldBe BAD_REQUEST
    }
  }
}
