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

package uk.gov.hmrc.apiplatform.modules.submissions.services

import org.mockito.MockitoSugar
import org.mockito.ArgumentMatchersSugar
import org.joda.time.DateTime

import scala.concurrent.Future.successful
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models._
import uk.gov.hmrc.apiplatform.modules.submissions.domain.models._
import uk.gov.hmrc.apiplatform.modules.submissions.MarkedSubmissionsTestData
import cats.data.NonEmptyList

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
        val response = Some(MarkedSubmission(submissionWithUnknownQuestion, Map.empty, markedAnswers))
        when(aMock.fetchLatestMarkedSubmission(eqTo(applicationId))(*)).thenReturn(successful(response))
      }

      def thenReturnWith(applicationId: ApplicationId, submission: Submission) = {
        val response = Some(MarkedSubmission(submission, Map.empty, markedAnswers))
        when(aMock.fetchLatestMarkedSubmission(eqTo(applicationId))(*)).thenReturn(successful(response))
      }

      def thenNotFound() = {
        when(aMock.fetchLatestMarkedSubmission(*[ApplicationId])(*)).thenReturn(successful(None))
      }
    }

    object FetchLatestSubmission {
      def thenReturn(applicationId: ApplicationId) = {
        val response = Some(extendedSubmission)
        when(aMock.fetchLatestSubmission(eqTo(applicationId))(*)).thenReturn(successful(response))
      }

      def thenReturnHasBeenSubmitted(applicationId: ApplicationId) = {
        val updatedInstance = submission.latestInstance.copy(statusHistory = Submission.Status.Submitted(DateTime.now, "user") :: submission.latestInstance.statusHistory)
        val submittedSubmission = submission.copy(instances = NonEmptyList(updatedInstance, submission.instances.tail))
        val response = Some(ExtendedSubmission(submittedSubmission, initialProgress))
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

    object Decline {
      def thenReturn(applicationId: ApplicationId, application: Application) = {
        val response = Right(application)
        when(aMock.decline(eqTo(applicationId), *, *)(*)).thenReturn(successful(response))
      }

      def thenReturnError(applicationId: ApplicationId) = {
        val response = Left("error")
        when(aMock.decline(eqTo(applicationId), *, *)(*)).thenReturn(successful(response))
      }
    }
  }

  object SubmissionServiceMock extends BaseSubmissionServiceMock {
    val aMock = mock[SubmissionService](withSettings.lenient())
  }
}
