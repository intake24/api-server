/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.staff.client;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import net.scran24.common.client.EmbeddedData;
import net.scran24.common.client.NavigationBar;
import net.scran24.common.client.UserInfoUpload;
import net.scran24.common.client.WidgetFactory;
import net.scran24.datastore.shared.SurveyParameters;
import net.scran24.datastore.shared.SurveyState;

import org.workcraft.gwt.shared.client.Callback1;
import org.workcraft.gwt.shared.client.Option;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.Window.Location;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.datepicker.client.DatePicker;

public class StaffPage implements EntryPoint {

	// private final PromptMessages messages = GWT.create(PromptMessages.class);

	private FlowPanel content;
	private RootPanel links;
	private SurveyControlServiceAsync surveyControl = SurveyControlServiceAsync.Util.getInstance(EmbeddedData.surveyId());
	private final String surveyId = EmbeddedData.surveyId();
	private SurveyParameters parameters;

	private void initStage1(final FlowPanel div) {
		div.add(new HTMLPanel("h2","1. Set the start and end dates"));

		HorizontalPanel hpanel = new HorizontalPanel();

		hpanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_TOP);
		hpanel.setSpacing(10);

		final DatePicker dateFrom = new DatePicker();
		final DatePicker dateTo = new DatePicker();

		hpanel.add(new Label("Start date:"));
		hpanel.add(dateFrom);
		hpanel.add(new Label("End date:"));
		hpanel.add(dateTo);

		final Button cont = WidgetFactory.createButton("Continue", new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				final long twelveHours = 12 * 60 * 60 * 1000;
				long timeFrom = dateFrom.getValue().getTime() - twelveHours;
				long timeTo = dateTo.getValue().getTime() + twelveHours;

				initStage2(timeFrom, timeTo, div);
			}
		});

		cont.setEnabled(false);
		cont.getElement().addClassName("scran24-admin-button");

		dateFrom.addValueChangeHandler(new ValueChangeHandler<Date>() {
			@Override
			public void onValueChange(ValueChangeEvent<Date> event) {
				if (event.getValue() != null && dateTo.getValue() != null) {
					cont.setEnabled(true);
				}
			}
		});

		dateTo.addValueChangeHandler(new ValueChangeHandler<Date>() {
			@Override
			public void onValueChange(ValueChangeEvent<Date> event) {
				if (event.getValue() != null && dateFrom.getValue() != null) {
					cont.setEnabled(true);
				}
			}
		});

		div.add(hpanel);
		div.add(cont);
	}

	private void initStage2(final long startDate, final long endDate, FlowPanel div) {
		div.clear();
		div.add(new HTMLPanel("h2", "2. Upload the respondent accounts"));

		final FlowPanel messageDiv = new FlowPanel();

		final Button start = WidgetFactory.createButton("Activate survey", new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				surveyControl.getParameters(new AsyncCallback<SurveyParameters>() {

					@Override
					public void onFailure(Throwable caught) {
						messageDiv.clear();
						messageDiv.getElement().getStyle().setColor("#d00");
						messageDiv.add(new HTMLPanel(SafeHtmlUtils.fromString("Survey could not be started: " + caught.getMessage())));
					}

					@Override
					public void onSuccess(SurveyParameters result) {
						surveyControl.setParameters(result.withDates(startDate, endDate).withState(SurveyState.ACTIVE), new AsyncCallback<Void>() {
							@Override
							public void onSuccess(Void result) {
								Location.reload();
							}

							@Override
							public void onFailure(Throwable caught) {
								messageDiv.clear();
								messageDiv.getElement().getStyle().setColor("#d00");
								messageDiv.add(new HTMLPanel(SafeHtmlUtils.fromString("Survey could not be started: " + caught.getMessage())));
							}
						});
					}
				});

			}
		});

		start.getElement().addClassName("intake24-admin-button");
		start.setEnabled(false);

		List<String> permissions = Arrays.asList(new String[] { "processSurvey:" + surveyId });

		final UserInfoUpload userUpload = new UserInfoUpload(surveyId, "respondent", permissions, new Callback1<Option<String>>() {
			@Override
			public void call(Option<String> res) {
				res.accept(new Option.SideEffectVisitor<String>() {
					@Override
					public void visitSome(String error) {
						messageDiv.clear();
						messageDiv.getElement().getStyle().setColor("#d00");
						messageDiv.add(new HTMLPanel(SafeHtmlUtils.fromString(error)));
						start.setEnabled(false);
					}

					@Override
					public void visitNone() {
						messageDiv.clear();
						messageDiv.getElement().getStyle().setColor("#0d0");
						messageDiv.add(new HTMLPanel(SafeHtmlUtils.fromString("Respondent accounts uploaded successfully")));
						start.setEnabled(true);
					}
				});
			}
		});

		div.add(userUpload);
		div.add(messageDiv);
		div.add(start);
	}

	private void showSurveyStatus() {
		content.clear();
		

		switch (parameters.state) {
		case SUSPENDED:
			content.add(new HTMLPanel("<h1>Survey is suspended</h1>"));
			content.add(new HTMLPanel("<h2>Reason</h2><p>" + SafeHtmlUtils.htmlEscape(parameters.suspensionReason) + "</p>"));

			Button resumeButton = WidgetFactory.createButton("Resume", new ClickHandler() {
				@Override
				public void onClick(ClickEvent event) {
					surveyControl.setParameters(parameters.withState(SurveyState.ACTIVE).withSuspensionReason(""), new AsyncCallback<Void>() {
						@Override
						public void onFailure(Throwable caught) {
							content.clear();
							content.add(new HTMLPanel("<p>Server error: </p>" + caught.getMessage()));
						}

						@Override
						public void onSuccess(Void result) {
							Location.reload();
						}
					});
				}
			});

			resumeButton.getElement().addClassName("scran24-admin-button");

			content.add(resumeButton);
			break;
		case NOT_INITIALISED:
			content.add(new HTMLPanel("<h1>Survey has not yet been activated</h1>"));
			content.add(new HTMLPanel("<p>Follow the instructions below to activate the survey."));

			FlowPanel initDiv = new FlowPanel();
			content.add(initDiv);

			initStage1(initDiv);
			break;
		case ACTIVE:
			Date now = new Date();

			boolean showSuspend = true;

			if (now.getTime() < parameters.startDate)
				content.add(new HTMLPanel("<h1>Survey is active, but not yet started</h1>"));
			else if (now.getTime() > parameters.endDate) {
				content.add(new HTMLPanel("<h1>Survey is finished</h1>"));
				showSuspend = false;
			} else
				content.add(new HTMLPanel("<h1>Survey is running</h1>"));

			content.add(new HTMLPanel("<h2>Start date (inclusive)</h2>"));
			content.add(new HTMLPanel("<p>" + DateTimeFormat.getFormat("EEE, MMMM d, yyyy").format(new Date(parameters.startDate)) + "</p>"));

			content.add(new HTMLPanel("<h2>End date (exclusive)</h2>"));
			content.add(new HTMLPanel("<p>" + DateTimeFormat.getFormat("EEE, MMMM d, yyyy").format(new Date(parameters.endDate)) + "</p>"));

			if (showSuspend) {

				content.add(new HTMLPanel(SafeHtmlUtils.fromSafeConstant("<h3>Suspend survey</h3>")));
				content.add(new HTMLPanel(SafeHtmlUtils.fromSafeConstant("<p>Reason for suspension:</p>")));
				final TextBox reason = new TextBox();
				reason.getElement().getStyle().setWidth(600, Unit.PX);

				Button suspend = WidgetFactory.createButton("Suspend", new ClickHandler() {
					@Override
					public void onClick(ClickEvent event) {
						if (reason.getText().isEmpty()) {
							Window.alert("Please give a reason for suspension.");
						} else {
							surveyControl.setParameters(parameters.withSuspensionReason(reason.getText()).withState(SurveyState.SUSPENDED),
									new AsyncCallback<Void>() {
										@Override
										public void onSuccess(Void result) {
											Location.reload();
										}

										@Override
										public void onFailure(Throwable caught) {
											Window.alert("Server error: " + caught.getMessage());
										}
									});
						}
					}
				});

				suspend.getElement().addClassName("scran24-admin-button");

				content.add(reason);
				content.add(new HTMLPanel("<p></p>"));
				content.add(suspend);
			}
			break;
		}
	}

	private void downloadData() {
		content.clear();
		content.add(new HTMLPanel("<h1>Download data</h1>"));
		final DownloadData download = new DownloadData();

		switch (parameters.state) {
		case SUSPENDED:
		case ACTIVE:
			download.dateFrom.setValue(new Date(parameters.startDate), true);
			download.dateTo.setValue(new Date(parameters.endDate), true);
			break;
		case NOT_INITIALISED:
			break;
		}
		content.add(download);
	}

	private void updateUsers() {
		content.clear();
		content.add(new HTMLPanel("<h2>Upload participant accounts from CSV</h2>"));
		content
				.add(new HTMLPanel(
						"<p><strong>Note:</strong> participant records from CSV files you upload using this page will be appended to the existing user list, which means that you can only add new accounts or update the passwords and custom fields for existing participants.</p><p>You <strong>cannot delete</strong> participant accounts from this page; if you need to delete existing participants please contact support.</p>"));

		final FlowPanel messageDiv = new FlowPanel();

		List<String> permissions = Arrays.asList(new String[] { "processSurvey:" + surveyId });

		final UserInfoUpload userUpload = new UserInfoUpload(surveyId, "respondent", permissions, new Callback1<Option<String>>() {
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
						messageDiv.add(new HTMLPanel(SafeHtmlUtils.fromString("Respondent accounts uploaded successfully")));
					}
				});
			}
		});

		content.add(messageDiv);
		content.add(userUpload);
	}

	private void updateSchedule() {
		content.clear();
		content.add(new HTMLPanel("<h1>Update schedule</h1>"));
		HorizontalPanel hpanel = new HorizontalPanel();

		hpanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_TOP);
		hpanel.setSpacing(10);

		final DatePicker dateFrom = new DatePicker();
		final DatePicker dateTo = new DatePicker();

		final Button update = WidgetFactory.createButton("Update", new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				final long twelveHours = 12 * 60 * 60 * 1000;
				final long timeFrom = dateFrom.getValue().getTime() - twelveHours;
				final long timeTo = dateTo.getValue().getTime() + twelveHours;

				surveyControl.getParameters(new AsyncCallback<SurveyParameters>() {
					@Override
					public void onFailure(Throwable caught) {
						content.clear();
						content.add(new HTMLPanel("<p>Server error: </p>" + caught.getMessage()));
					}

					@Override
					public void onSuccess(SurveyParameters result) {
						surveyControl.setParameters(result.withDates(timeFrom, timeTo), new AsyncCallback<Void>() {
							@Override
							public void onFailure(Throwable caught) {
								content.clear();
								content.add(new HTMLPanel("<p>Server error: </p>" + caught.getMessage()));
							}

							@Override
							public void onSuccess(Void result) {
								Location.reload();
							}
						});
					}
				});
			}
		});

		update.getElement().addClassName("scran24-admin-button");

		dateFrom.addValueChangeHandler(new ValueChangeHandler<Date>() {
			@Override
			public void onValueChange(ValueChangeEvent<Date> event) {
				if (event.getValue() != null && dateTo.getValue() != null) {
					update.setEnabled(true);
				}
			}
		});

		dateTo.addValueChangeHandler(new ValueChangeHandler<Date>() {
			@Override
			public void onValueChange(ValueChangeEvent<Date> event) {
				if (event.getValue() != null && dateFrom.getValue() != null) {
					update.setEnabled(true);
				}
			}
		});

		update.setEnabled(false);

		switch (parameters.state) {
		case SUSPENDED:
		case ACTIVE:
			dateFrom.setValue(new Date(parameters.startDate), true);
			dateTo.setValue(new Date(parameters.endDate), true);
			break;
		case NOT_INITIALISED:
			break;
		}

		hpanel.add(new Label("Start date:"));
		hpanel.add(dateFrom);
		hpanel.add(new Label("End date:"));
		hpanel.add(dateTo);

		content.add(hpanel);
		content.add(update);
	}

	@Override
	public void onModuleLoad() {
		final Element loading = Document.get().getElementById("loading");
		links = RootPanel.get("navigation-bar");

		RootPanel contentDiv = RootPanel.get("main-content");
		content = new FlowPanel();
		content.addStyleName("intake24-staff-ui-container");
		contentDiv.add(content);

		surveyControl.getParameters(new AsyncCallback<SurveyParameters>() {
			@Override
			public void onSuccess(SurveyParameters result) {
				parameters = result;

				final Anchor downloadData = new Anchor("Download data");
				downloadData.addClickHandler(new ClickHandler() {
					@Override
					public void onClick(ClickEvent event) {
						downloadData();
					}
				});

				final Anchor surveyCtl = new Anchor("Survey control");
				surveyCtl.addClickHandler(new ClickHandler() {
					@Override
					public void onClick(ClickEvent event) {
						showSurveyStatus();
					}
				});

				final Anchor updateUsers = new Anchor("Create participant accounts");
				updateUsers.addClickHandler(new ClickHandler() {
					@Override
					public void onClick(ClickEvent event) {
						updateUsers();
					}
				});

				final Anchor updateSchedule = new Anchor("Update schedule");
				updateSchedule.addClickHandler(new ClickHandler() {
					@Override
					public void onClick(ClickEvent event) {
						updateSchedule();
					}
				});

				links.clear();
				links.add(new NavigationBar(downloadData, surveyCtl, updateUsers, updateSchedule, new Anchor("Log out", "../../../common/logout"
						+ Location.getQueryString())));

				showSurveyStatus();
				loading.removeFromParent();
			}

			@Override
			public void onFailure(Throwable caught) {
				content.clear();
				content.add(new HTMLPanel("<p>Server error</p?" + caught.getMessage()));
				loading.removeFromParent();
			}
		});
	}
}