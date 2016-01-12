/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.admin.client;

import java.util.ArrayList;

import org.workcraft.gwt.shared.client.Callback1;
import org.workcraft.gwt.shared.client.Option;

import net.scran24.common.client.NavigationBar;
import net.scran24.common.client.UserInfoUpload;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.Window.Location;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.RootPanel;

public class Scran24Admin implements EntryPoint {
	
	FlowPanel content;
	
	private void createSurvey() {
		content.clear();
		content.add(new HTMLPanel("<h1>New survey</h1>"));
		content.add(new NewSurvey());
	}
	
	private void manageSurveys() {
		content.clear();
		content.add(new HTMLPanel("<h1>Survey management</h1>"));
		content.add(new SurveyManagement());
	}
	
	private void downloadMissingFoods() {
		content.clear();
		content.add(new HTMLPanel("<h1>Missing foods report</h1>"));
		content.add(new DownloadMissingFoods());
	}
	
	private void uploadAdminAccounts() {
		content.clear();
		content.add(new HTMLPanel("<h1>Upload admin accounts</h1>"));
		
		final FlowPanel messageDiv = new FlowPanel();
		
		content.add(messageDiv);
		
		content.add(new UserInfoUpload("admin", "admin", new ArrayList<String>(), new Callback1<Option<String>>() {
			@Override
			public void call(Option<String> res) {
				res.accept(new Option.SideEffectVisitor<String>() {
					@Override
					public void visitSome(String error) {
						messageDiv.clear();
						messageDiv.getElement().getStyle().setColor("#d00");
						messageDiv.add(new HTMLPanel(SafeHtmlUtils.fromString(error)));
					}

					@Override
					public void visitNone() {
						messageDiv.clear();
						messageDiv.getElement().getStyle().setColor("#0d0");
						messageDiv.add(new HTMLPanel(SafeHtmlUtils.fromString("Admin accounts uploaded successfully")));
					}
				});
			}
		}));
	}
	
	@Override
	public void onModuleLoad() {
		final RootPanel links = RootPanel.get("navigation-bar");
		RootPanel contentDiv = RootPanel.get("main-content");
		
		content = new FlowPanel();
		content.addStyleName("intake24-staff-ui-container");
		
		contentDiv.add(content);
		
		Anchor logOut = new Anchor("Log out", "../common/logout" + Location.getQueryString());
		
		Anchor createSurvey = new Anchor ("+ New survey");
		
		createSurvey.getElement().getStyle().setMarginRight(15, Unit.PX);
		
		createSurvey.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				createSurvey();	
			}
		});
		
		Anchor manageSurveys = new Anchor ("Manage surveys");
		
		manageSurveys.getElement().getStyle().setMarginRight(15, Unit.PX);
		
		manageSurveys.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				manageSurveys();	
			}
		});
		
		Anchor downloadMissingFoods = new Anchor ("Missing foods");
		
		downloadMissingFoods.getElement().getStyle().setMarginRight(15, Unit.PX);
		
		downloadMissingFoods.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				downloadMissingFoods();	
			}
		});	
		
		Anchor manageAdminAccounts = new Anchor ("Manage admin accounts");
		
		manageAdminAccounts.getElement().getStyle().setMarginRight(15, Unit.PX);
		
		manageAdminAccounts.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				uploadAdminAccounts();
			}
		});
		
		links.clear();
		
		links.add(new NavigationBar(createSurvey, manageSurveys, downloadMissingFoods, manageAdminAccounts, logOut));
		
		createSurvey();
		
		Document.get().getElementById("loading").removeFromParent();
	}
}
