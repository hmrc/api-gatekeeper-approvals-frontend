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

package uk.gov.hmrc.apigatekeeperapprovalsfrontend.repositories

import java.time.Clock
import scala.concurrent.ExecutionContext.Implicits.global

import com.mongodb.MongoException
import org.scalatest.BeforeAndAfterEach

import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.utils.ServerBaseISpec

import uk.gov.hmrc.apiplatform.modules.applications.submissions.domain.models.SubmissionId
import uk.gov.hmrc.apiplatform.modules.common.utils.FixedClock
import uk.gov.hmrc.apiplatform.modules.submissions.SubmissionsTestData

import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.SubmissionReview

class SubmissionReviewRepositoryISpec
    extends ServerBaseISpec
    with SubmissionsTestData
    with FixedClock
    with BeforeAndAfterEach {

  protected override def appBuilder: GuiceApplicationBuilder =
    GuiceApplicationBuilder()
      .configure(
        "metrics.jvm" -> false,
        "mongodb.uri" -> s"mongodb://localhost:27017/test-${this.getClass.getSimpleName}"
      )
      .overrides(bind[Clock].toInstance(clock))

  val submissionReviewRepo: SubmissionReviewRepo = app.injector.instanceOf[SubmissionReviewRepo]

  val submissionReview1 = SubmissionReview(submissionId, 0, true, true, false, false)
  val submissionReview2 = SubmissionReview(submissionId, 1, true, false, false, true)

  override def beforeEach(): Unit = {
    await(submissionReviewRepo.collection.drop().toFuture())
    await(submissionReviewRepo.ensureIndexes())
  }

  "create and find" should {

    "not find a record that is not there" in {
      await(submissionReviewRepo.find(SubmissionId.random, 0)) mustBe None
    }

    "store a record and retrieve it" in {
      await(submissionReviewRepo.create(submissionReview1)) mustBe submissionReview1
      await(submissionReviewRepo.create(submissionReview2)) mustBe submissionReview2
      await(submissionReviewRepo.find(submissionId, 0)).value mustBe submissionReview1
    }

    "not store multiple records of the same submission id" in {
      await(submissionReviewRepo.create(submissionReview1)) mustBe submissionReview1

      intercept[MongoException] {
        await(submissionReviewRepo.create(submissionReview1))
      }

      await(
        submissionReviewRepo.collection
          .countDocuments()
          .toFuture()
          .map(x => x.toInt)
      ) mustBe 1
    }
  }

  "update" should {

    "create a record and update it" in {
      val submissionReview1New = SubmissionReview(submissionId, 0, true, true, true, true)
      await(submissionReviewRepo.create(submissionReview1)) mustBe submissionReview1
      await(submissionReviewRepo.update(submissionReview1New)) mustBe submissionReview1New
      await(submissionReviewRepo.find(submissionId, 0)).value mustBe submissionReview1New
    }
  }
}
