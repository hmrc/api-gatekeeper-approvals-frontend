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
import scala.concurrent.Future.successful

import org.joda.time.{DateTime, DateTimeZone}
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}

import uk.gov.hmrc.apiplatform.modules.submissions.MarkedSubmissionsTestData
import uk.gov.hmrc.apiplatform.modules.submissions.domain.models._
import uk.gov.hmrc.apiplatform.modules.submissions.domain.models.TermsOfUseInvitationState.EMAIL_SENT

import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models._

trait SubmissionServiceMockModule extends MockitoSugar with ArgumentMatchersSugar with MarkedSubmissionsTestData {

  trait BaseSubmissionServiceMock {
    def aMock: SubmissionService

    object FetchLatestMarkedSubmission {

      def thenReturn(applicationId: ApplicationId) = {
        val response = Some(markedSubmission)
        when(aMock.fetchLatestMarkedSubmission(eqTo(applicationId))(*)).thenReturn(successful(response))
      }

      def thenReturn(submission: MarkedSubmission) = {
        when(aMock.fetchLatestMarkedSubmission(*[ApplicationId])(*)).thenReturn(successful(Some(submission)))
      }

      def thenReturnIncludingAnUnknownQuestion(applicationId: ApplicationId) = {
        val answersIncludingUnknownQuestion = aSubmission.latestInstance.answersToQuestions ++ Map(Question.Id.random -> TextAnswer("not there"))
        val submissionWithUnknownQuestion   = aSubmission.answeringWith(answersIncludingUnknownQuestion)

        val response = Some(MarkedSubmission(submissionWithUnknownQuestion, markedAnswers))
        when(aMock.fetchLatestMarkedSubmission(eqTo(applicationId))(*)).thenReturn(successful(response))
      }

      def thenReturnWith(applicationId: ApplicationId, submission: Submission) = {
        val response = Some(MarkedSubmission(submission, markedAnswers))
        when(aMock.fetchLatestMarkedSubmission(eqTo(applicationId))(*)).thenReturn(successful(response))
      }

      def thenReturnWith(applicationId: ApplicationId, submission: MarkedSubmission) = {
        when(aMock.fetchLatestMarkedSubmission(eqTo(applicationId))(*)).thenReturn(successful(Some(submission)))
      }

      def thenNotFound() = {
        when(aMock.fetchLatestMarkedSubmission(*[ApplicationId])(*)).thenReturn(successful(None))
      }
    }

    object FetchLatestSubmission {

      def thenReturn(applicationId: ApplicationId) = {
        val response = Some(aSubmission)
        when(aMock.fetchLatestSubmission(eqTo(applicationId))(*)).thenReturn(successful(response))
      }

      def thenReturnHasBeenSubmitted(applicationId: ApplicationId) = {
        val submittedSubmission = (Submission.addStatusHistory(Submission.Status.Answering(DateTime.now.withZone(DateTimeZone.UTC), true)) andThen Submission.submit(DateTime.now, "user"))(aSubmission)
        val response            = Some(submittedSubmission)
        when(aMock.fetchLatestSubmission(eqTo(applicationId))(*)).thenReturn(successful(response))
      }

      def thenReturnHasBeenGranted(applicationId: ApplicationId) = {
        val submittedSubmission = (Submission.addStatusHistory(Submission.Status.Answering(DateTime.now.withZone(DateTimeZone.UTC), true)) andThen Submission.submit(DateTime.now, "user") andThen Submission.warnings(DateTime.now, "user") andThen Submission.grantWithWarnings(DateTime.now, "user", "Warnings", None) andThen Submission.grant(DateTime.now, "user", None))(aSubmission)
        val response            = Some(submittedSubmission)
        when(aMock.fetchLatestSubmission(eqTo(applicationId))(*)).thenReturn(successful(response))
      }

      def thenNotFound() = {
        when(aMock.fetchLatestSubmission(*[ApplicationId])(*)).thenReturn(successful(None))
      }
    }

    object Grant {

      def thenReturn(applicationId: ApplicationId, application: Application) = {
        val response = Right(application)
        when(aMock.grant(eqTo(applicationId), *)(*)).thenReturn(successful(response))
      }

      def thenReturnError(applicationId: ApplicationId) = {
        val response = Left("error")
        when(aMock.grant(eqTo(applicationId), *)(*)).thenReturn(successful(response))
      }
    }

    object GrantWithWarnings {

      def thenReturn(applicationId: ApplicationId, application: Application) = {
        val response = Right(application)
        when(aMock.grantWithWarnings(eqTo(applicationId), *, *, *)(*)).thenReturn(successful(response))
      }

      def thenReturnError(applicationId: ApplicationId) = {
        val response = Left("error")
        when(aMock.grantWithWarnings(eqTo(applicationId), *, *, *)(*)).thenReturn(successful(response))
      }
    }

    object TermsOfUseInvite {

      def thenReturn(applicationId: ApplicationId) = {
        val response = Right(TermsOfUseInvitationSuccessful)
        when(aMock.termsOfUseInvite(eqTo(applicationId))(*)).thenReturn(successful(response))
      }

      def thenReturnError(applicationId: ApplicationId) = {
        val response = Left("error")
        when(aMock.termsOfUseInvite(eqTo(applicationId))(*)).thenReturn(successful(response))
      }
    }

    object FetchTermsOfUseInvitation {

      def thenReturn(applicationId: ApplicationId) = {
        val response = Some(TermsOfUseInvitation(applicationId, Instant.now, Instant.now, Instant.now, None, EMAIL_SENT))
        when(aMock.fetchTermsOfUseInvitation(eqTo(applicationId))(*)).thenReturn(successful(response))
      }

      def thenNotFound() = {
        when(aMock.fetchTermsOfUseInvitation(*[ApplicationId])(*)).thenReturn(successful(None))
      }
    }

    object FetchTermsOfUseInvitations {

      def thenReturn() = {
        val response = List(TermsOfUseInvitation(applicationId, Instant.now, Instant.now, Instant.now, None, EMAIL_SENT))
        when(aMock.fetchTermsOfUseInvitations()(*)).thenReturn(successful(response))
      }

      def thenNotFound() = {
        when(aMock.fetchTermsOfUseInvitation(*[ApplicationId])(*)).thenReturn(successful(None))
      }
    }

    object GrantWithWarningsOrDeclineForTouUplift {

      def thenReturn(applicationId: ApplicationId, application: Application) = {
        val response = Right(application)
        when(aMock.grantWithWarningsOrDeclineForTouUplift(eqTo(applicationId), *, *, *)(*)).thenReturn(successful(response))
      }

      def thenReturnError(applicationId: ApplicationId) = {
        val response = Left("error")
        when(aMock.grantWithWarningsOrDeclineForTouUplift(eqTo(applicationId), *, *, *)(*)).thenReturn(successful(response))
      }
    }

    object GrantForTouUplift {

      def thenReturn(applicationId: ApplicationId, application: Application) = {
        val response = Right(application)
        when(aMock.grantForTouUplift(eqTo(applicationId), *)(*)).thenReturn(successful(response))
      }

      def thenReturnError(applicationId: ApplicationId) = {
        val response = Left("error")
        when(aMock.grantForTouUplift(eqTo(applicationId), *)(*)).thenReturn(successful(response))
      }
    }
  }

  object SubmissionServiceMock extends BaseSubmissionServiceMock {
    val aMock = mock[SubmissionService](withSettings.lenient())
  }
}
