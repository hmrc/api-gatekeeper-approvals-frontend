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

package uk.gov.hmrc.apiplatform.modules.submissions.domain.models

import play.api.libs.json.Format

import uk.gov.hmrc.apiplatform.modules.common.domain.services.SimpleEnumJsonFormatting

// TODO caps
enum TermsOfUseInvitationState {
  case EMAIL_SENT
  case REMINDER_EMAIL_SENT
  case OVERDUE
  case WARNINGS
  case FAILED
  case TERMS_OF_USE_V2_WITH_WARNINGS
  case TERMS_OF_USE_V2
}

object TermsOfUseInvitationState {
  def apply(text: String): Option[TermsOfUseInvitationState] = TermsOfUseInvitationState.values.find(_.toString() == text.toUpperCase)

  given Format[TermsOfUseInvitationState] = SimpleEnumJsonFormatting.createEnumFormatFor[TermsOfUseInvitationState]("Terms of use state", apply)
}
