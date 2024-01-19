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
import uk.gov.hmrc.apiplatform.modules.common.domain.services.SealedTraitJsonFormatting

sealed trait TermsOfUseInvitationState

object TermsOfUseInvitationState {
  case object EMAIL_SENT                    extends TermsOfUseInvitationState
  case object REMINDER_EMAIL_SENT           extends TermsOfUseInvitationState
  case object OVERDUE                       extends TermsOfUseInvitationState
  case object WARNINGS                      extends TermsOfUseInvitationState
  case object FAILED                        extends TermsOfUseInvitationState
  case object TERMS_OF_USE_V2_WITH_WARNINGS extends TermsOfUseInvitationState
  case object TERMS_OF_USE_V2               extends TermsOfUseInvitationState

  val values = Set(EMAIL_SENT, REMINDER_EMAIL_SENT, OVERDUE, WARNINGS, FAILED, TERMS_OF_USE_V2_WITH_WARNINGS, TERMS_OF_USE_V2)

  def apply(text: String): Option[TermsOfUseInvitationState] = TermsOfUseInvitationState.values.find(_.toString() == text.toUpperCase)

  def unsafeApply(text: String): TermsOfUseInvitationState = apply(text).getOrElse(throw new RuntimeException(s"$text is not a valid Terms of use state"))

  implicit val format: Format[TermsOfUseInvitationState] = SealedTraitJsonFormatting.createFormatFor[TermsOfUseInvitationState]("Terms of use state", apply)
}
