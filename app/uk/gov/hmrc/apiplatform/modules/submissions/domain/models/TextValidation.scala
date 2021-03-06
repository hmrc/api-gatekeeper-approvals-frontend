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

package uk.gov.hmrc.apiplatform.modules.submissions.domain.models

import play.api.libs.json.Json

sealed trait TextValidation

object TextValidation {
  case object Url extends TextValidation
  case class MatchRegex(regex: String) extends TextValidation
  case object Email extends TextValidation

  import uk.gov.hmrc.play.json.Union

  implicit val formatAsUrl = Json.format[Url.type]
  implicit val formatMatchRegex = Json.format[MatchRegex]
  implicit val formatIsEmail = Json.format[Email.type]

  implicit val formatTextValidation = Union.from[TextValidation]("validationType")
    .and[Url.type]("url")
    .and[MatchRegex]("regex")
    .and[Email.type]("email")
    .format
}