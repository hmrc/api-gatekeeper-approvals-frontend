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

package uk.gov.hmrc.apigatekeeperapprovalsfrontend.views.helpers

object FormKeys {
  val firstnameField = "firstname"

  val declineReasonsRequiredKey = "declinereasons.error.required.field"

  val declineReasonsRequiredGlobalKey = "declinereasons.error.required.global"

  val formKeysMap = Map(
    declineReasonsRequiredKey -> declineReasonsRequiredGlobalKey
  )

  val globalKeys = formKeysMap.values.toSeq

  val globalToField = Map(
    declineReasonsRequiredGlobalKey -> firstnameField
  )
}
