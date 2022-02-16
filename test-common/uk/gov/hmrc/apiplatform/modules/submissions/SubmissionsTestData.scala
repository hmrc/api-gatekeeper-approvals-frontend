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

package uk.gov.hmrc.apiplatform.modules.submissions

import uk.gov.hmrc.time.DateTimeUtils
import cats.data.NonEmptyList
import uk.gov.hmrc.apigatekeeperapprovalsfrontend.domain.models.ApplicationId
import uk.gov.hmrc.apiplatform.modules.submissions.domain.models._
import scala.collection.immutable.ListMap
import scala.util.Random
import org.joda.time.DateTime

trait QuestionnaireTestData {
  object DevelopmentPractices {
    val question1 = YesNoQuestion(
      QuestionId("653d2ee4-09cf-46a0-bc73-350a385ae860"),
      Wording("Do your development practices follow our guidance?"),
      Statement(
        CompoundFragment(
          StatementText("You must develop software following our"),
          StatementLink("development practices (opens in a new tab)", "http://www.google.com"),
          StatementText(".")
        )
      ),
      yesMarking = Pass,
      noMarking = Warn
    )

    val question2 = YesNoQuestion(
      QuestionId("6139f57d-36ab-4338-85b3-a2079d6cf376"),
      Wording("Does your error handling meet our specification?"),
      Statement(
        CompoundFragment(
          StatementText("We will check for evidence that you comply with our"),
          StatementLink("error handling specification (opens in new tab)", "http://www.google.com"),
          StatementText(".")
        )
      ),
      yesMarking = Pass,
      noMarking = Fail
    )
      
    val question3 = YesNoQuestion(
      QuestionId("3c5cd29d-bec2-463f-8593-cd5412fab1e5"),
      Wording("Does your software meet accessibility standards?"),
      Statement(
        CompoundFragment(
          StatementText("Web-based software must meet level AA of the"),
          StatementLink("Web Content Accessibility Guidelines (WCAG) (opens in new tab)", "http://www.google.com"),
          StatementText(". Desktop software should follow equivalent offline standards.")
        )
      ),
      yesMarking = Pass,
      noMarking = Warn
    )

    val questionnaire = Questionnaire(
      id = QuestionnaireId("796336a5-f7b4-4dad-8003-a818e342cbb4"),
      label = Label("Development practices"),
      questions = NonEmptyList.of(
        QuestionItem(question1), 
        QuestionItem(question2), 
        QuestionItem(question3)
      )
    )
  }
    
  object OrganisationDetails {
      val questionRI1 = TextQuestion(
        QuestionId("36b7e670-83fc-4b31-8f85-4d3394908495"),
        Wording("What is the name of your responsible individual"),
        
        Statement(
          List(
            StatementText("The responsible individual:"),
            CompoundFragment(
              StatementText("ensures your software meets our "),
              StatementLink("terms of use", "/api-documentation/docs/terms-of-use")
            ),
            CompoundFragment(
              StatementText("understands the "),
              StatementLink("consequences of not meeting the terms of use", "/api-documentation/docs/terms-of-use")
            )
          )
        )
      )
      val questionRI2 = TextQuestion(
        QuestionId("fb9b8036-cc88-4f4e-ad84-c02caa4cebae"),
        Wording("What is the email address of your responsible individual"),
        Statement(
          List(
            StatementText("The responsible individual:"),
            CompoundFragment(
              StatementText("ensures your software meets our "),
              StatementLink("terms of use", "/api-documentation/docs/terms-of-use")
            ),
            CompoundFragment(
              StatementText("understands the "),
              StatementLink("consequences of not meeting the terms of use", "/api-documentation/docs/terms-of-use")
            )
          )
        )
      )
    val question1 = TextQuestion(
      QuestionId("b9dbf0a5-e72b-4c89-a735-26f0858ca6cc"),
      Wording("Give us your organisation's website URL"),
      Statement(
        List(
          StatementText("For example https://example.com")
        )
      ),
      Some(("My organisation doesn't have a website", Fail))
    )

    val question2 = ChooseOneOfQuestion(
      QuestionId("cbdf264f-be39-4638-92ff-6ecd2259c662"),
      Wording("Identify your organisation"),
      Statement(
        List(
          StatementText("Provide evidence that your organisation is officially registered in the UK."),
          StatementText("Choose one option")
        )
      ),
      ListMap(
        (PossibleAnswer("Unique Taxpayer Reference (UTR)") -> Pass),
        (PossibleAnswer("VAT registration number") -> Pass),
        (PossibleAnswer("Corporation Tax Unique Taxpayer Reference (UTR)") -> Pass),
        (PossibleAnswer("PAYE reference") -> Pass),
        (PossibleAnswer("My organisation is in the UK and doesn't have any of these") -> Pass),
        (PossibleAnswer("My organisation is outside the UK and doesn't have any of these") -> Warn)
      )
    )

    val question2a = TextQuestion(
      QuestionId("4e148791-1a07-4f28-8fe4-ba3e18cdc118"),
      Wording("What is your company registration number?"),
      Statement(
        List(
          StatementText("You can find your company registration number on any official documentation you receive from Companies House."),
          StatementText("It's 8 characters long or 2 letters followed by 6  numbers. Check and documents from Companies House.")
        )
      ),
      Some(("My organisation doesn't have a company registration", Warn))
    )

    val question2b = TextQuestion(
      QuestionId("55da0b97-178c-45b5-a139-b61ad7b9ca84"),
      Wording("What is your Unique Taxpayer Reference (UTR)?"),
      Statement(List.empty)
    )
    val question2c = TextQuestion(
      QuestionId("dd12fd8b-907b-4ba1-95d3-ef6317f36199"),
      Wording("What is your VAT registration number?"),
      Statement(List.empty)
    )
    val question2d = TextQuestion(
      QuestionId("6be23951-ac69-47bf-aa56-86d3d690ee0b"),
      Wording("What is your Corporation Tax Unique Taxpayer Reference (UTR)?"),
      Statement(List.empty)
    )
    val question2e = TextQuestion(
      QuestionId("a143760e-72f3-423b-a6b4-558db37a3453"),
      Wording("What is your PAYE reference?"),
      Statement(List.empty)
    )
      
    val question3 = AcknowledgementOnly(
      QuestionId("a12f314e-bc12-4e0d-87ba-1326acb31008"),
      Wording("Provide evidence of your organisation's registration"),
      Statement(
        List(
          StatementText("You will need to provide evidence that your organisation is officially registered in a country outside of the UK."),
          StatementText("You will be asked for a digital copy of the official registration document.")
        )
      )
    )

    val questionnaire = Questionnaire(
      id = QuestionnaireId("ac69b129-524a-4d10-89a5-7bfa46ed95c7"),
      label = Label("Organisation details"),
      questions = NonEmptyList.of(
        QuestionItem(questionRI1),
        QuestionItem(questionRI2),
        QuestionItem(question1),
        QuestionItem(question2),
        QuestionItem(question2a, AskWhenAnswer(question2, "My organisation is in the UK and doesn't have any of these")),
        QuestionItem(question2b, AskWhenAnswer(question2, "Unique Taxpayer Reference (UTR)")),
        QuestionItem(question2c, AskWhenAnswer(question2, "VAT registration number")),
        QuestionItem(question2d, AskWhenAnswer(question2, "Corporation Tax Unique Taxpayer Reference (UTR)")),
        QuestionItem(question2e, AskWhenAnswer(question2, "PAYE reference")),
        QuestionItem(question3,  AskWhenAnswer(question2, "My organisation is outside the UK and doesn't have any of these"))
      )
    )
  }

  object CustomersAuthorisingYourSoftware {
    val question1 = AcknowledgementOnly(
      QuestionId("95da25e8-af3a-4e05-a621-4a5f4ca788f6"),
      Wording("Customers authorising your software"),
      Statement(
        List(
          StatementText("Your customers will see the information you provide here when they authorise your software to interact with HMRC."),
          StatementText("Before you continue, you will need:"),
          StatementBullets(
            List(
              StatementText("the name of your software"),
              StatementText("the location of your servers which store customer data"),
              StatementText("a link to your privacy policy"),
              StatementText("a link to your terms and conditions")
            )
          )
        )
      )
    )

    val question2 = TextQuestion(
      QuestionId("4d5a41c8-8727-4d09-96c0-e2ce1bc222d3"),
      Wording("Confirm the name of your software"),
      Statement(
        List(
          StatementText("We show this name to your users when they authorise your software to interact with HMRC."),
          CompoundFragment(
            StatementText("It must comply with our "),
            StatementLink("naming guidelines (opens in a new tab)", "https://developer.service.hmrc.gov.uk/api-documentation/docs/using-the-hub/name-guidelines"),
            StatementText(".")            
          ),
          StatementText("Application name")
        )
      )
    )

    val question3 = MultiChoiceQuestion(
      QuestionId("57d706ad-c0b8-462b-a4f8-90e7aa58e57a"),
      Wording("Where are your servers that store customer information?"),
      Statement(
        StatementText("Select all that apply.")
      ),
      ListMap(
        (PossibleAnswer("In the UK") -> Pass),
        (PossibleAnswer("In the European Economic Area") -> Pass),
        (PossibleAnswer("Outside the European Economic Area") -> Warn)
      )
    )

    val question4 = TextQuestion(
      QuestionId("c0e4b068-23c9-4d51-a1fa-2513f50e428f"),
      Wording("Give us your privacy policy URL"),
      Statement(
        List(
          StatementText("Include the policy which covers the software you are requesting production credentials for."),
          StatementText("For example https://example.com/privacy-policy")
        )
      ),
      Some(("I don't have a privacy policy", Fail))
    )
      
    val question5 = TextQuestion(
      QuestionId("0a6d6973-c49a-49c3-93ff-de58daa1b90c"),
      Wording("Give us your terms and conditions URL"),
      Statement(
        List(
          StatementText("Your terms and conditions should cover the software you are requesting production credentials for."),
          StatementText("For example https://example.com/terms-conditions")
        )
      ),
      Some(("I don't have terms and conditions", Fail))
    )
      
    val questionnaire = Questionnaire(
      id = QuestionnaireId("3a7f3369-8e28-447c-bd47-efbabeb6d93f"),
      label = Label("Customers authorising your software"),
      questions = NonEmptyList.of(
        QuestionItem(question1),
        QuestionItem(question2),
        QuestionItem(question3, AskWhenContext(DeriveContext.Keys.IN_HOUSE_SOFTWARE, "No")),
        QuestionItem(question4),
        QuestionItem(question5)
      )
    )
  }

  val activeQuestionnaireGroupings = 
    NonEmptyList.of(
      GroupOfQuestionnaires(
        heading = "Your processes",
        links = NonEmptyList.of(
          DevelopmentPractices.questionnaire
        )            
      ),
      GroupOfQuestionnaires(
        heading = "Your software",
        links = NonEmptyList.of(
          CustomersAuthorisingYourSoftware.questionnaire
        )            
      ),
      GroupOfQuestionnaires(
        heading = "Your details",
        links = NonEmptyList.of(
          OrganisationDetails.questionnaire
        )
      )
    )

  val questionIdsOfInterest = QuestionIdsOfInterest(
    responsibleIndividualNameId   = OrganisationDetails.questionRI1.id,
    responsibleIndividualEmailId  = OrganisationDetails.questionRI2.id,
    applicationNameId             = CustomersAuthorisingYourSoftware.question2.id,
    privacyPolicyUrlId            = CustomersAuthorisingYourSoftware.question4.id,
    termsAndConditionsUrlId       = CustomersAuthorisingYourSoftware.question5.id,
    organisationUrlId             = OrganisationDetails.question1.id,
    identifyYourOrganisationId    = OrganisationDetails.question2.id
  )

  object DeriveContext {
    object Keys {
      val VAT_OR_ITSA = "VAT_OR_ITSA"
      val IN_HOUSE_SOFTWARE = "IN_HOUSE_SOFTWARE" // Stored on Application
    }
  }

  val simpleContext = Map(DeriveContext.Keys.IN_HOUSE_SOFTWARE -> "Yes", DeriveContext.Keys.VAT_OR_ITSA -> "No")
  val soldContext = Map(DeriveContext.Keys.IN_HOUSE_SOFTWARE -> "No", DeriveContext.Keys.VAT_OR_ITSA -> "No")
  val vatContext = Map(DeriveContext.Keys.IN_HOUSE_SOFTWARE -> "Yes", DeriveContext.Keys.VAT_OR_ITSA -> "Yes")

  def answer(desiredMark: Mark)(question: Question): Map[QuestionId, Option[ActualAnswer]] = {
    val answers: List[Option[ActualAnswer]] = question match {

      case YesNoQuestion(id, _, _, yesMarking, noMarking, absence) =>
        (if(yesMarking == desiredMark) Some(SingleChoiceAnswer("Yes")) else None) ::
        (if(noMarking == desiredMark) Some(SingleChoiceAnswer("No")) else None) ::
        (absence.flatMap(a => if(a._2 == desiredMark) Some(NoAnswer) else None)) ::
        List.empty[Option[ActualAnswer]]

      case ChooseOneOfQuestion(id, _, _, marking, absence) => {
        marking.map {
          case (pa, mark) => Some(SingleChoiceAnswer(pa.value))
          case _ => None
        }
        .toList ++
        List(absence.flatMap(a => if(a._2 == desiredMark) Some(NoAnswer) else None))
      }

      case TextQuestion(id, _, _, absence) => 
        if(desiredMark == Pass)
          Some(TextAnswer(Random.nextString(Random.nextInt(25)+1))) ::
          absence.flatMap(a => if(a._2 == desiredMark) Some(NoAnswer) else None) ::
          List.empty[Option[ActualAnswer]]
        else
          List(Some(NoAnswer))  // Cos we can't do anything else

      case AcknowledgementOnly(id, _, _) => List(Some(NoAnswer))

      case MultiChoiceQuestion(id, _, _, marking, absence) => 
        marking.map {
          case (pa, mark) if(mark == desiredMark) => Some(MultipleChoiceAnswer(Set(pa.value)))
          case _ => None
        }
        .toList ++
        List(absence.flatMap(a => if(a._2 == desiredMark) Some(NoAnswer) else None))
    }

    Map(question.id -> Random.shuffle(
      answers.collect {
        case Some(a) => a
      }
    ).headOption)
  }

  def answersQ(desiredMark: Mark)(questionnaire: Questionnaire): Map[QuestionId, ActualAnswer] = {
    questionnaire.questions
    .toList
    .map(qi => qi.question)
    .flatMap(x => answer(desiredMark)(x))
    .collect {
      case (id, Some(a)) => id -> a
    }
    .toMap
  }

  def answersG(desiredMark: Mark)(groups: NonEmptyList[GroupOfQuestionnaires]): Map[QuestionId, ActualAnswer] = {
    groups
    .flatMap(g => g.links)
    .toList
    .flatMap(qn => answersQ(desiredMark)(qn))
    .toMap
  }
}

trait SubmissionsTestData extends QuestionnaireTestData {

  val submissionId = Submission.Id.random
  val applicationId = ApplicationId.random

  val sampleAnswersToQuestions = Map(
    (DevelopmentPractices.question1.id -> SingleChoiceAnswer("Yes")),
    (DevelopmentPractices.question2.id -> SingleChoiceAnswer("No")),
    (DevelopmentPractices.question3.id -> SingleChoiceAnswer("No")),
    (OrganisationDetails.questionRI1.id -> TextAnswer("Bob Cratchett")),
    (OrganisationDetails.questionRI2.id -> TextAnswer("bob@example.com")),
    (OrganisationDetails.question1.id -> TextAnswer("https://example.com")),
    (OrganisationDetails.question2.id -> SingleChoiceAnswer("VAT registration number")),
    (OrganisationDetails.question2c.id -> TextAnswer("1234567")),
    (CustomersAuthorisingYourSoftware.question1.id -> AcknowledgedAnswer),
    (CustomersAuthorisingYourSoftware.question2.id -> TextAnswer("name of software")),
    (CustomersAuthorisingYourSoftware.question3.id -> MultipleChoiceAnswer(Set("In the UK"))),
    (CustomersAuthorisingYourSoftware.question4.id -> TextAnswer("https://example.com/privacy-policy")),
    (CustomersAuthorisingYourSoftware.question5.id -> NoAnswer)
  )

  val initialStatus = Submission.Status.Created(DateTimeUtils.now, "bob@example.com")
  val initialInstances = NonEmptyList.of(Submission.Instance(0, Map.empty, NonEmptyList.of(initialStatus)))

  val submission = Submission(submissionId, applicationId, DateTimeUtils.now, activeQuestionnaireGroupings, questionIdsOfInterest, initialInstances)

  private val answersIncludingUnknownQuestion = submission.latestInstance.answersToQuestions ++ Map(QuestionId.random -> TextAnswer("not there"))
  val submissionWithUnknownQuestion = submission.copy(instances = NonEmptyList.of(submission.latestInstance.copy(answersToQuestions = answersIncludingUnknownQuestion)))
}

trait ExtendedSubmissionsTestData extends SubmissionsTestData {
  import AsIdsHelpers._

  def progress(state: QuestionnaireState)(groups: NonEmptyList[GroupOfQuestionnaires]): Map[QuestionnaireId, QuestionnaireProgress] = groups.flatMap(g => g.links).map(qn => qn.id -> QuestionnaireProgress(state, qn.questions.asIds)).toList.toMap

  val initialProgress = progress(QuestionnaireState.NotStarted)(activeQuestionnaireGroupings)
  val completedProgress = progress(QuestionnaireState.Completed)(activeQuestionnaireGroupings)

  val extendedSubmission = ExtendedSubmission(submission, initialProgress)
}

trait MarkedSubmissionsTestData extends ExtendedSubmissionsTestData {
  val markedAnswers: Map[QuestionId, Mark] = Map(
    (DevelopmentPractices.question1.id -> Pass),
    (DevelopmentPractices.question2.id -> Fail),
    (DevelopmentPractices.question3.id -> Warn),
    (OrganisationDetails.question1.id -> Pass),
    (OrganisationDetails.questionRI1.id -> Pass),
    (OrganisationDetails.questionRI2.id -> Pass),
    (CustomersAuthorisingYourSoftware.question3.id -> Pass),
    (CustomersAuthorisingYourSoftware.question4.id -> Pass),
    (CustomersAuthorisingYourSoftware.question5.id -> Fail)
  )

  val markedSubmission = MarkedSubmission(submission, completedProgress, markedAnswers)
  val submittableStatus = Submission.Status.Created(DateTimeUtils.now, "bob@example.com")
  val submittableInstance = Submission.Instance(0, sampleAnswersToQuestions, NonEmptyList.of(submittableStatus))
  val submittableSubmission = submission.copy(instances = NonEmptyList.of(submittableInstance))
  

  def markAsPass(now: DateTime = DateTimeUtils.now, requestedBy: String = "bob@example.com")(submission: Submission): MarkedSubmission = {
    val completedProgress = progress(QuestionnaireState.Completed)(submission.groups)
    val answers = answersG(Pass)(submission.groups)
    val marks = answers.map { case (q,a) => q -> Pass }

    val initialInstance = Submission.Instance(0, answers, NonEmptyList.of(Submission.Status.Created(now, requestedBy)))
    val newSub = submission.copy(instances = NonEmptyList.of(initialInstance))
    MarkedSubmission(newSub, completedProgress, marks)
  }

  implicit class SubmissionSyntax(submission: Submission) {
    def submit(now: DateTime = DateTimeUtils.now, requestedBy: String = "bob@example.com"): Submission = {
      require(submission.latestInstance.isCreated)

      val replacementInstance = submission.latestInstance.copy(statusHistory = NonEmptyList(Submission.Status.Submitted(now, requestedBy), submission.latestInstance.statusHistory.toList))

      submission.copy(
        instances = NonEmptyList(replacementInstance, submission.instances.tail)
      )
    }

    def declined(now: DateTime = DateTimeUtils.now, gatekeeperUserName: String = "Default Gatekeeper User Name", reasons: String = "Default Decline Reason"): Submission = {
      require(submission.latestInstance.isSubmitted)

      val answers = submission.latestInstance.answersToQuestions
      val originalRequestedBy = submission.latestInstance.statusHistory.last match {
        case Submission.Status.Created(_, requestedBy) => requestedBy
        case _ => "bob@example.com"
      }

      val replaceLastInstance = submission.latestInstance.copy(statusHistory = NonEmptyList(Submission.Status.Declined(now, gatekeeperUserName, reasons), submission.latestInstance.statusHistory.toList))
      val newInstanceIndex = submission.latestInstance.index + 1
      val newInstance = Submission.Instance(newInstanceIndex, answers, NonEmptyList.of(Submission.Status.Created(now, originalRequestedBy)))

      submission.copy(
        instances = NonEmptyList(newInstance, replaceLastInstance :: submission.instances.tail)
      )
    }

    def granted(now: DateTime = DateTimeUtils.now, gatekeeperUserName: String = "Default Gatekeeper User Name"): Submission = {
      require(submission.latestInstance.isSubmitted)

      val replaceLastInstance = submission.latestInstance.copy(statusHistory = NonEmptyList(Submission.Status.Granted(now, gatekeeperUserName), submission.latestInstance.statusHistory.toList))

      submission.copy(
        instances = NonEmptyList(replaceLastInstance, submission.instances.tail.toList)
      )
    }
  }
}

object SubmissionsTestData extends SubmissionsTestData
