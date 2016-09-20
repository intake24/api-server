/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.client.surveyscheme;

import java.util.HashMap;

import net.scran24.common.client.AsyncRequest;
import net.scran24.common.client.AsyncRequestAuthHandler;
import net.scran24.common.client.CurrentUser;
import net.scran24.common.client.WidgetFactory;
import net.scran24.datastore.shared.UserInfo;
import net.scran24.user.client.services.UserDataServiceAsync;
import net.scran24.user.client.survey.SimpleSurveyStageInterface;
import net.scran24.user.client.survey.SurveyStage;
import net.scran24.user.client.survey.flat.Survey;
import net.scran24.user.client.survey.prompts.RadioButtonQuestion;
import net.scran24.user.client.survey.prompts.TextBoxQuestion;

import org.pcollections.client.PVector;
import org.pcollections.client.TreePVector;
import org.workcraft.gwt.shared.client.Callback1;
import org.workcraft.gwt.shared.client.Callback2;
import org.workcraft.gwt.shared.client.Option;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTMLPanel;

public class UserDataQuestion implements SurveyStage<Survey> {
	final public static String FLAG_SKIP_USERDATA_UPLOAD = "skip-userdata-upload";
	
	final private UserDataServiceAsync userDataService = UserDataServiceAsync.Util.getInstance();
	final private Survey state;

	public UserDataQuestion(final Survey state) {
		this.state = state;
	}

	@Override
	public SimpleSurveyStageInterface getInterface(final Callback1<Survey> onComplete, Callback2<Survey, Boolean> onIntermediateStateChange) {
		final FlowPanel content = new FlowPanel();
		content.addStyleName("intake24-survey-content-container");

		content.add(new HTMLPanel("<p>Before you continue, please answer a few questions about yourself.</p>"));

		final PVector<String> ageOptions = TreePVector.<String>empty()
				.plus("11").plus("12").plus("13").plus("14").plus("15").plus("16").plus("17").plus("18");
		
		final PVector<String> genderOptions = 
				TreePVector.<String>empty().plus("Male").plus("Female");
		
		final RadioButtonQuestion ageBlock = new RadioButtonQuestion(SafeHtmlUtils.fromSafeConstant("<p>What age are you?</p>"), ageOptions, "ageGroup", Option.<String>none());
		content.add(ageBlock);
		final RadioButtonQuestion genderBlock = new RadioButtonQuestion(SafeHtmlUtils.fromSafeConstant("<p>Are you...</p>"), genderOptions, "genderGroup", Option.<String>none());
		content.add(genderBlock);
		
		final TextBoxQuestion postCode = new TextBoxQuestion(SafeHtmlUtils.fromSafeConstant("<p>What is your post code?</p>"));
		
		content.add(postCode);
		
		final TextBoxQuestion schoolName = new TextBoxQuestion(SafeHtmlUtils.fromSafeConstant("<p>What is the name of the School you go to?</p>"));
		
		content.add(schoolName);
		
		final TextBoxQuestion townName = new TextBoxQuestion(SafeHtmlUtils.fromSafeConstant("<p>What is the name of your town?</p>"));
		
		content.add(townName);

		final Button accept = WidgetFactory.createButton("Continue");
		
		accept.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				Option<String> ageChoice = ageBlock.getChoice();
				
				if (ageChoice.isEmpty()) {
					ageBlock.showWarning();
					return;
				}
				else 
					ageBlock.clearWarning();
				
				Option<String> genderChoice = genderBlock.getChoice();
				
				if (genderChoice.isEmpty()) {
					genderBlock.showWarning();
					return;
				}
				else 
					genderBlock.clearWarning();
				
				if (postCode.textBox.getText().isEmpty()) {
					postCode.showWarning();
					return;
				} else
					postCode.clearWarning();
				
				if (schoolName.textBox.getText().isEmpty()) {
					schoolName.showWarning();
					return;
				} else
					schoolName.clearWarning();
				
				if (townName.textBox.getText().isEmpty()) {
					townName.showWarning();
					return;
				} else
					townName.clearWarning();
				
				final HashMap<String, String> data = new HashMap<String, String>();
				data.put("age", ageChoice.getOrDie()); 
				data.put("gender", genderChoice.getOrDie());
				data.put("postCode", postCode.textBox.getText());
				data.put("schoolName", schoolName.textBox.getText());
				data.put("townName", townName.textBox.getText());
				
				accept.setEnabled(false);
				
				AsyncRequestAuthHandler.execute(new AsyncRequest<Void>() {
					@Override
					public void execute(AsyncCallback<Void> callback) {
						userDataService.submit(data, callback);
					}
				}, new AsyncCallback<Void>() {
					@Override
					public void onFailure(Throwable caught) {
						onComplete.call(state.withFlag(FLAG_SKIP_USERDATA_UPLOAD));																								
					}

					@Override
					public void onSuccess(Void result) {
						UserInfo userInfo = CurrentUser.getUserInfo();
						CurrentUser.setUserInfo(new UserInfo(userInfo.userName, userInfo.surveyId, userInfo.surveyParameters, data));
						onComplete.call(state);
					}
				});
				
				userDataService.submit(data, new AsyncCallback<Void>() {
					@Override
					public void onSuccess(Void result) {
					}
					
					@Override
					public void onFailure(Throwable caught) {
						Window.alert(caught.getMessage());
					}
				});
			}
		});

		content.add(WidgetFactory.createButtonsPanel(accept));

		return new SimpleSurveyStageInterface(content);
	}
}