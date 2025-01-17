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

package uk.gov.hmrc.apiplatform.modules.submissions.services

import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future.successful

import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.ApplicationWithCollaboratorsFixtures
import uk.gov.hmrc.apiplatform.modules.commands.applications.domain.models.{ApplicationCommands, DispatchSuccessResult}
import uk.gov.hmrc.apiplatform.modules.common.domain.models.ApplicationId
import uk.gov.hmrc.apiplatform.modules.common.utils.FixedClock
import uk.gov.hmrc.apiplatform.modules.submissions.MarkedSubmissionsTestData
import uk.gov.hmrc.apiplatform.modules.submissions.connectors.SubmissionsConnector
import uk.gov.hmrc.apiplatform.modules.submissions.domain.models.TermsOfUseInvitationState.EMAIL_SENT
import uk.gov.hmrc.apiplatform.modules.submissions.domain.models.{TermsOfUseInvitation, TermsOfUseInvitationWithApplication}

import uk.gov.hmrc.apigatekeeperapprovalsfrontend.connectors.ApplicationCommandConnector
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.utils.AsyncHmrcSpec

class SubmissionServiceSpec extends AsyncHmrcSpec with MarkedSubmissionsTestData with ApplicationWithCollaboratorsFixtures {

  trait Setup extends FixedClock {
    implicit val hc: HeaderCarrier                                   = HeaderCarrier()
    val applicationId                                                = applicationIdOne
    val mockSubmissionsConnector: SubmissionsConnector               = mock[SubmissionsConnector]
    val mockApplicationCommandConnector: ApplicationCommandConnector = mock[ApplicationCommandConnector]
    val requestedBy                                                  = "bob@example.com"
    val app                                                          = standardApp.withState(appStateTesting)

    val underTest = new SubmissionService(mockSubmissionsConnector, mockApplicationCommandConnector, clock)
  }

  "fetchLatestSubmission" should {
    "successfully fetch latest submission" in new Setup {
      when(mockSubmissionsConnector.fetchLatestSubmission(*[ApplicationId])(*)).thenReturn(successful(Some(aSubmission)))
      val result = await(underTest.fetchLatestSubmission(applicationId))
      result shouldBe Some(aSubmission)
    }
  }

  "fetchLatestMarkedSubmission" should {
    "successfully fetch latest marked submission" in new Setup {
      when(mockSubmissionsConnector.fetchLatestMarkedSubmission(*[ApplicationId])(*)).thenReturn(successful(Some(markedSubmission)))
      val result = await(underTest.fetchLatestMarkedSubmission(applicationId))
      result shouldBe Some(markedSubmission)
    }
  }

  "grant" should {
    "call application command connector correctly" in new Setup {
      val cmd    = ApplicationCommands.GrantApplicationApprovalRequest(requestedBy, instant, None, None)
      when(mockApplicationCommandConnector.dispatch(eqTo(applicationId), eqTo(cmd), eqTo(Set.empty))(*)).thenReturn(successful(Right(DispatchSuccessResult(app))))
      val result = await(underTest.grant(applicationId, requestedBy))
      result shouldBe Right(DispatchSuccessResult(app))
    }
  }

  "grantWithWarnings" should {
    "call application command connector correctly" in new Setup {
      val warnings = "warn"
      val manager  = "manager"
      val cmd      = ApplicationCommands.GrantApplicationApprovalRequest(requestedBy, instant, Some(warnings), Some(manager))
      when(mockApplicationCommandConnector.dispatch(eqTo(applicationId), eqTo(cmd), eqTo(Set.empty))(*)).thenReturn(successful(Right(DispatchSuccessResult(app))))
      val result   = await(underTest.grantWithWarnings(applicationId, requestedBy, warnings, Some(manager)))
      result shouldBe Right(DispatchSuccessResult(app))
    }
  }

  "termsOfUseInvite" should {
    "call submission connector correctly" in new Setup {
      val cmd    = ApplicationCommands.SendTermsOfUseInvitation(requestedBy, instant)
      when(mockApplicationCommandConnector.dispatch(eqTo(applicationId), eqTo(cmd), eqTo(Set.empty))(*)).thenReturn(successful(Right(DispatchSuccessResult(app))))
      val result = await(underTest.termsOfUseInvite(applicationId, requestedBy))
      result shouldBe Right(DispatchSuccessResult(app))
    }
  }

  "fetchTermsOfUseInvitation" should {
    "call submission connector correctly" in new Setup {
      val invite = TermsOfUseInvitation(applicationId, Instant.now, Instant.now, Instant.now, None, EMAIL_SENT)
      when(mockSubmissionsConnector.fetchTermsOfUseInvitation(eqTo(applicationId))(*)).thenReturn(successful(Some(invite)))
      val result = await(underTest.fetchTermsOfUseInvitation(applicationId))
      result shouldBe Some(invite)
    }
  }

  "fetchTermsOfUseInvitations" should {
    "call submission connector correctly" in new Setup {
      val invite = TermsOfUseInvitation(applicationId, Instant.now, Instant.now, Instant.now, None, EMAIL_SENT)
      when(mockSubmissionsConnector.fetchTermsOfUseInvitations()(*)).thenReturn(successful(List(invite)))
      val result = await(underTest.fetchTermsOfUseInvitations())
      result shouldBe List(invite)
    }
  }

  "searchTermsOfUseInvitations" should {
    "call submission connector correctly" in new Setup {
      val invite = TermsOfUseInvitationWithApplication(applicationId, Instant.now, Instant.now, Instant.now, None, EMAIL_SENT, "app name")
      when(mockSubmissionsConnector.searchTermsOfUseInvitations(*)(*)).thenReturn(successful(List(invite)))
      val result = await(underTest.searchTermsOfUseInvitations(Seq("status" -> "EMAIL_SENT")))
      result shouldBe List(invite)
    }
  }

  "grantWithWarningsOrDeclineForTouUplift" should {
    "call submission connector correctly with a Failed submission" in new Setup {
      when(mockSubmissionsConnector.declineForTouUplift(eqTo(applicationId), *, *)(*)).thenReturn(successful(Right(app)))
      val result = await(underTest.grantWithWarningsOrDeclineForTouUplift(applicationId, failedSubmission, "requestedBy", "reasons"))
      result shouldBe Right(app)
    }

    "call submission connector correctly with a Warnings submission" in new Setup {
      when(mockSubmissionsConnector.grantWithWarningsForTouUplift(eqTo(applicationId), *, *)(*)).thenReturn(successful(Right(app)))
      val result = await(underTest.grantWithWarningsOrDeclineForTouUplift(applicationId, warningsSubmission, "requestedBy", "reasons"))
      result shouldBe Right(app)
    }

    "return an error if the supplied submission is not Failed or Warnings" in new Setup {
      val result = await(underTest.grantWithWarningsOrDeclineForTouUplift(applicationId, aSubmission, "requestedBy", "reasons"))
      result shouldBe Left("Error - invalid submission status")
    }
  }

  "grantForTouUplift" should {
    "call submission connector correctly" in new Setup {
      val cmd    = ApplicationCommands.GrantTermsOfUseApproval(requestedBy, instant, "reasons", Some("Mr Supervisor"))
      when(mockApplicationCommandConnector.dispatch(eqTo(applicationId), eqTo(cmd), eqTo(Set.empty))(*)).thenReturn(successful(Right(DispatchSuccessResult(app))))
      val result = await(underTest.grantForTouUplift(applicationId, requestedBy, "reasons", Some("Mr Supervisor")))
      result shouldBe Right(DispatchSuccessResult(app))
    }
  }

  "resetForTouUplift" should {
    "call submission connector correctly" in new Setup {
      when(mockSubmissionsConnector.resetForTouUplift(eqTo(applicationId), *, *)(*)).thenReturn(successful(Right(app)))
      val result = await(underTest.resetForTouUplift(applicationId, "requestedBy", "reasons"))
      result shouldBe Right(app)
    }
  }

  "deleteTouUplift" should {
    "call submission connector correctly" in new Setup {
      when(mockSubmissionsConnector.deleteTouUplift(eqTo(applicationId), *)(*)).thenReturn(successful(Right(app)))
      val result = await(underTest.deleteTouUplift(applicationId, "requestedBy"))
      result shouldBe Right(app)
    }
  }
}
