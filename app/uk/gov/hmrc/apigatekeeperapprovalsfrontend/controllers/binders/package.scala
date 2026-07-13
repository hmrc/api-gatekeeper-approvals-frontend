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

import play.api.mvc.QueryStringBindable

import uk.gov.hmrc.apiplatform.modules.common.domain.models.*

// N.B. Lots commented out here until Play supports opaque types on the paths.
//
package object binders {

  // $COVERAGE-OFF$
  private def applicationIdFromString(text: String): Either[String, ApplicationId] = {
    ApplicationId.apply(text)
      .toRight(s"Cannot accept $text as ApplicationId")
  }

  // implicit def applicationIdPathBinder(implicit textBinder: PathBindable[String]): PathBindable[ApplicationId] = new PathBindable[ApplicationId] {

  //   override def bind(key: String, value: String): Either[String, ApplicationId] = {
  //     textBinder.bind(key, value).flatMap(applicationIdFromString)
  //   }

  //   override def unbind(key: String, applicationId: ApplicationId): String = {
  //     applicationId.value.toString()
  //   }
  // }

  implicit def applicationIdQueryStringBindable(implicit textBinder: QueryStringBindable[String]): QueryStringBindable[ApplicationId] = new QueryStringBindable[ApplicationId] {

    override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, ApplicationId]] = {
      textBinder.bind(key, params).map(_.flatMap(applicationIdFromString))
    }

    override def unbind(key: String, applicationId: ApplicationId): String = {
      textBinder.unbind(key, applicationId.value.toString())
    }
  }

  // $COVERAGE-ON$
}
