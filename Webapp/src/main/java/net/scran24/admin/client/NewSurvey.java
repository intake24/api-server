/*
This file is part of Intake24.

Copyright 2015, 2016 Newcastle University.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

This file is based on Intake24 v1.0.

© Crown copyright, 2012, 2013, 2014

Licensed under the Open Government Licence 3.0: 

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.admin.client;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.ui.*;
import net.scran24.admin.client.services.SurveyManagementServiceAsync;
import net.scran24.common.client.WidgetFactory;
import net.scran24.datastore.shared.SurveySchemeReference;
import net.scran24.datastore.shared.SurveySchemes;

import org.workcraft.gwt.shared.client.Option;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.rpc.AsyncCallback;

public class NewSurvey extends Composite {
  final private SurveyManagementServiceAsync service = SurveyManagementServiceAsync.Util.getInstance();
  private final FlowPanel contents;

  public NewSurvey() {
    contents = new FlowPanel();

    final Grid surveyParametersGrid = new Grid(7, 2);
    surveyParametersGrid.setCellPadding(5);

    contents.add(surveyParametersGrid);

    final Label idLabel = new Label("Survey identifier: ");
    final TextBox idTextBox = new TextBox();

    surveyParametersGrid.setWidget(0, 0, idLabel);
    surveyParametersGrid.setWidget(0, 1, idTextBox);

    final Label schemeBoxLabel = new Label("Survey scheme: ");
    final ListBox schemeBox = new ListBox();
    schemeBox.setMultipleSelect(false);

    final Label localeLabel = new Label("Locale: ");
    final ListBox localeBox = new ListBox();
    localeBox.setMultipleSelect(false);

    localeBox.addItem("English (UK)", "en_GB");
    localeBox.addItem("English (New Zealand)", "en_NZ");
    localeBox.addItem("Portuguese (Portugal)", "pt_PT");
    localeBox.addItem("Danish (Denmark)", "da_DK");
    localeBox.addItem("Arabic (UAE)", "ar_AE");

    for (SurveySchemeReference s : SurveySchemes.allSchemes)
      schemeBox.addItem(s.description(), s.id());

    schemeBox.setSelectedIndex(0);

    surveyParametersGrid.setWidget(1, 0, schemeBoxLabel);
    surveyParametersGrid.setWidget(1, 1, schemeBox);

    surveyParametersGrid.setWidget(2, 0, localeLabel);
    surveyParametersGrid.setWidget(2, 1, localeBox);

    final Label genUserLabel = new Label("Allow auto login: ");
    final CheckBox genCheckBox = new CheckBox();

    surveyParametersGrid.setWidget(3, 0, genUserLabel);
    surveyParametersGrid.setWidget(3, 1, genCheckBox);

    final Label forwardToSurveyMonkey = new Label("SurveyMonkey support:");
    final CheckBox smCheckBox = new CheckBox();

    surveyParametersGrid.setWidget(4, 0, forwardToSurveyMonkey);
    surveyParametersGrid.setWidget(4, 1, smCheckBox);

    final Label surveyMonkeyUrl = new Label("SurveyMonkey link:");
    final TextBox smUrlTextBox = new TextBox();

    surveyParametersGrid.setWidget(5, 0, surveyMonkeyUrl);
    surveyParametersGrid.setWidget(5, 1, smUrlTextBox);
    
    smUrlTextBox.setEnabled(false);
    
    final Label supportEmailLabel = new Label("Support e-mail:");
    final TextBox supportEmailTextBox = new TextBox();
    
    surveyParametersGrid.setWidget(6, 0, supportEmailLabel);
    surveyParametersGrid.setWidget(6, 1, supportEmailTextBox);

    smCheckBox.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
      @Override
      public void onValueChange(ValueChangeEvent<Boolean> valueChangeEvent) {
        smUrlTextBox.setEnabled(valueChangeEvent.getValue());
      }
    });

    final FlowPanel errorDiv = new FlowPanel();
    errorDiv.getElement().addClassName("scran24-admin-survey-id-error-message");

    contents.add(errorDiv);

    final Button createButton = WidgetFactory.createButton("Create survey");

    createButton.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        createButton.setEnabled(false);
        final String id = idTextBox.getText();
        errorDiv.clear();

        String smUrlText = smUrlTextBox.getText();

        Option<String> smUrl;

        if (smCheckBox.getValue())
          smUrl = smUrlText.isEmpty() ? Option.<String>none() : Option.some(smUrlText);
        else
          smUrl = Option.<String>none();

        if (smCheckBox.getValue() && smUrlText.isEmpty()) {
          errorDiv.add(new Label("Please paste the SurveyMonkey link!"));
          createButton.setEnabled(true);
          return;
        } else if (smCheckBox.getValue() && !smUrlText.contains("intake24_username=[intake24_username_value]")) {
          errorDiv.add(new Label("Invalid SurveyMonkey link: intake24_username variable missing!"));
          createButton.setEnabled(true);
          return;
        }

        service.createSurvey(id, schemeBox.getValue(schemeBox.getSelectedIndex()), localeBox.getValue(localeBox.getSelectedIndex()),
            genCheckBox.getValue(), smUrl, supportEmailTextBox.getValue(), new AsyncCallback<Option<String>>() {
              @Override
              public void onSuccess(Option<String> result) {
                result.accept(new Option.SideEffectVisitor<String>() {
                  @Override
                  public void visitSome(String error) {
                    errorDiv.add(new Label(error));
                    createButton.setEnabled(true);
                  }

                  @Override
                  public void visitNone() {
                    contents.clear();
                    contents.add(new HTMLPanel(SafeHtmlUtils.fromSafeConstant(
                        "<h2>Survey created!</h2><p>Please go to <strong>Survey management</strong> and upload the staff accounts for the new survey.</p>")));
                  }
                });
              }

              @Override
              public void onFailure(Throwable caught) {
                createButton.setEnabled(true);
                errorDiv.add(new Label("Server error (" + SafeHtmlUtils.htmlEscape(caught.getMessage()) + "), please check server logs"));
              }
            });
      }
    });
    createButton.getElement().addClassName("scran24-admin-button");

    VerticalPanel buttonsPanel = new VerticalPanel();
    buttonsPanel.add(createButton);

    contents.add(buttonsPanel);

    initWidget(contents);
  }
}
