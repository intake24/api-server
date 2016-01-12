/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.client.survey.flat.rules;

import org.pcollections.client.PSet;
import org.workcraft.gwt.shared.client.Option;

import net.scran24.user.client.survey.flat.Prompt;
import net.scran24.user.client.survey.flat.PromptRule;
import net.scran24.user.client.survey.flat.SelectionType;
import net.scran24.user.client.survey.flat.Survey;
import net.scran24.user.client.survey.flat.SurveyOperation;
import net.scran24.user.client.survey.prompts.ConfirmCompletionPrompt;
import net.scran24.user.shared.WithPriority;

public class ConfirmCompletion implements PromptRule<Survey, SurveyOperation> {
	@Override
	public Option<Prompt<Survey, SurveyOperation>> apply(Survey state, SelectionType selectionType, PSet<String> surveyFlags) {
		if (state.completionConfirmed())
			return Option.none();
		else
			return Option.<Prompt<Survey, SurveyOperation>>some(new ConfirmCompletionPrompt());
	}

	@Override
	public String toString() {
		return "Confirm survey completion";
	}

	public static WithPriority<PromptRule<Survey, SurveyOperation>> withPriority(int priority) {
		return new WithPriority<PromptRule<Survey, SurveyOperation>>(new ConfirmCompletion(), priority);
	}
}