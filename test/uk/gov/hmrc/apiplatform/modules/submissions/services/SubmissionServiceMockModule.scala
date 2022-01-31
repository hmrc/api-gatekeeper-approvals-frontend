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

import scala.concurrent.Future.successful
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models._
import uk.gov.hmrc.apiplatform.modules.submissions.domain.models._
import uk.gov.hmrc.apiplatform.modules.submissions.SubmissionsTestData
import cats.data.NonEmptyList

trait SubmissionServiceMockModule extends MockitoSugar with ArgumentMatchersSugar with SubmissionsTestData {
  trait BaseSubmissionServiceMock {
    def aMock: SubmissionService

    object FetchLatestMarkedSubmission {
      def thenReturn(applicationId: ApplicationId) = {
        val response = Some(markedSubmission)
        when(aMock.fetchLatestMarkedSubmission(eqTo(applicationId))(*)).thenReturn(successful(response))
      }

      def thenReturnIncludingAnUnknownQuestion(applicationId: ApplicationId) = {
        val answersIncludingUnknownQuestion = submission.latestInstance.answersToQuestions ++ Map(QuestionId.random -> TextAnswer("not there"))
        val submissionWithUnknownQuestion = submission.copy(instances = NonEmptyList.of(submission.latestInstance.copy(answersToQuestions = answersIncludingUnknownQuestion)))
        val response = Some(MarkedSubmission(submissionWithUnknownQuestion, Map.empty, markedAnswers))
        when(aMock.fetchLatestMarkedSubmission(eqTo(applicationId))(*)).thenReturn(successful(response))
      }

      def thenNotFound() = {
        when(aMock.fetchLatestMarkedSubmission(*[ApplicationId])(*)).thenReturn(successful(None))
      }
    }

    object Approve {
      def thenReturn(applicationId: ApplicationId, application: Application) = {
        val response = Right(application)
        when(aMock.approve(eqTo(applicationId), *)(*)).thenReturn(successful(response))
      }

      def thenReturnError(applicationId: ApplicationId) = {
        val response = Left("error")
        when(aMock.approve(eqTo(applicationId), *)(*)).thenReturn(successful(response))
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