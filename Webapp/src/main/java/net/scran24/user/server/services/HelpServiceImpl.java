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

package net.scran24.user.server.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletException;

import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailConstants;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.workcraft.gwt.shared.client.Option;

import com.google.gwt.core.server.StackTraceDeobfuscator;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.google.inject.Injector;
import com.twilio.sdk.TwilioRestClient;
import com.twilio.sdk.TwilioRestException;
import com.twilio.sdk.resource.factory.MessageFactory;
import com.twilio.sdk.resource.instance.Account;

import net.scran24.common.server.auth.ScranUserId;
import net.scran24.datastore.DataStore;
import net.scran24.datastore.DataStoreException;
import net.scran24.datastore.SupportStaffRecord;
import net.scran24.user.client.services.HelpService;
import net.scran24.user.client.services.HelpServiceException;

public class HelpServiceImpl extends RemoteServiceServlet implements HelpService {
  private static final long serialVersionUID = -5525469181691523598L;

  // private final NotificationMessages messages =
  // GWT.create(NotificationMessages.class);

  private final Logger log = LoggerFactory.getLogger(HelpServiceImpl.class);

  public static final long helpRequestCooldown = 60 * 60 * 1000;

  private StackTraceDeobfuscator deobfuscator;

  private DataStore dataStore;

  private TwilioRestClient twilioClient;

  private String smtpHostName;
  private int smtpPort;
  private String smtpUserName;
  private String smtpPassword;
  private String fromEmail;
  private String fromName;
  private String fromPhoneNumber;

  private Map<String, RateInfo> rateMap = new ConcurrentHashMap<String, RateInfo>();

  @Override
  public void init() throws ServletException {
    Injector injector = (Injector) this.getServletContext()
      .getAttribute("intake24.injector");

    dataStore = injector.getInstance(DataStore.class);

    twilioClient = new TwilioRestClient(getServletContext().getInitParameter("twilioAccountSid"),
        getServletContext().getInitParameter("twilioAuthToken"));

    smtpHostName = getServletContext().getInitParameter("smtpHostName");
    smtpPort = Integer.parseInt(getServletContext().getInitParameter("smtpPort"));
    smtpUserName = getServletContext().getInitParameter("smtpUserName");
    smtpPassword = getServletContext().getInitParameter("smtpPassword");
    fromEmail = getServletContext().getInitParameter("emailNotificationFromAddress");
    fromName = getServletContext().getInitParameter("emailNotificationFromName");

    fromPhoneNumber = getServletContext().getInitParameter("smsNotificationFromNumber");

    String path = getServletContext().getRealPath("/WEB-INF/deploy/user/symbolMaps");

    log.debug("Symbol maps path: " + path);

    deobfuscator = StackTraceDeobfuscator.fromFileSystem(path);
  }

  private void sendSmsNotification(String name, String surveyId, String number, List<String> numbers) {
    Account account = twilioClient.getAccount();

    for (String to : numbers) {

      MessageFactory messageFactory = account.getMessageFactory();
      List<NameValuePair> params = new ArrayList<NameValuePair>();
      params.add(new BasicNameValuePair("To", to));
      params.add(new BasicNameValuePair("From", fromPhoneNumber));
      params.add(
          new BasicNameValuePair("Body", "Please call " + name + " on " + number + " (survey id: " + surveyId + ")"));
      try {
        messageFactory.create(params);
      } catch (TwilioRestException e) {
        log.error("Failed to send SMS notification", e);
      }
    }
  }

  private void sendEmailNotification(String name, String surveyId, String number, List<String> addresses) {
    Email email = new SimpleEmail();

    email.setHostName(smtpHostName);
    email.setSmtpPort(smtpPort);
    email.setAuthenticator(new DefaultAuthenticator(smtpUserName, smtpPassword));
    email.setSSLOnConnect(true);
    email.setCharset(EmailConstants.UTF_8);

    try {
      email.setFrom(fromEmail, fromName);
      email.setSubject("Someone needs help completing their survey");
      email.setMsg("Please call " + name + " on " + number + " (survey id: " + surveyId + ")");

      for (String address : addresses)
        email.addTo(address);

      email.send();
    } catch (EmailException e) {
      log.error("Failed to send e-mail notification", e);
    }
  }

  @Override
  public boolean requestCall(final String name, final String number) {
    Subject subject = SecurityUtils.getSubject();
    ScranUserId userId = (ScranUserId) subject.getPrincipal();

    if (userId == null)
      throw new HelpServiceException("User must be logged in");
    else if (userId.survey.equals("demo"))
      throw new HelpServiceException("This feature is disabled for demo survey");
    else {

      log.info("Received call back request from " + userId.survey + "/" + userId.username);

      try {
        Option<Long> lastRequestTime = dataStore.getLastHelpRequestTime(userId.survey, userId.username);

        long timeSinceLastRequest;

        if (lastRequestTime.isEmpty()) {
          log.info("No previous call back requests from this user");
          timeSinceLastRequest = Long.MAX_VALUE;
        } else {
          timeSinceLastRequest = System.currentTimeMillis() - lastRequestTime.getOrDie();
          log.info("Last call back request was " + timeSinceLastRequest / 1000 + " seconds ago");
        }

        if (timeSinceLastRequest < helpRequestCooldown) {
          log.info("Request refused");
          return false;
        } else {
          log.info("Request accepted");

          dataStore.setLastHelpRequestTime(userId.survey, userId.username, System.currentTimeMillis());

          List<SupportStaffRecord> supportStaff = dataStore.getSupportStaffRecords();

          final ArrayList<String> sendToAddresses = new ArrayList<String>();
          final ArrayList<String> sendToNumbers = new ArrayList<String>();

          for (SupportStaffRecord staff : supportStaff) {
            staff.email.accept(new Option.SideEffectVisitor<String>() {
              @Override
              public void visitSome(String item) {
                sendToAddresses.add(item);
              }

              @Override
              public void visitNone() {
              }
            });

            staff.phoneNumber.accept(new Option.SideEffectVisitor<String>() {
              @Override
              public void visitSome(String item) {
                sendToNumbers.add(item);
              }

              @Override
              public void visitNone() {
              }
            });
          }

          if (sendToAddresses.isEmpty())
            log.warn("No staff e-mail addresses available to receive e-mail notification!");
          else {
            sendEmailNotification(name, userId.survey, number, sendToAddresses);
            log.info("E-mail notification sent");
          }

          if (sendToNumbers.isEmpty())
            log.warn("No staff phone numbers available to receive SMS notification!");
          else {
            sendSmsNotification(name, userId.survey, number, sendToNumbers);
            log.info("SMS notification sent");
          }

          return true;
        }
      } catch (DataStoreException e) {
        throw new HelpServiceException(e);
      }
    }
  }

  @Override
  public void reportUncaughtException(String strongName, List<String> classNames, List<String> messages,
      List<StackTraceElement[]> stackTraces, String surveyState) {
    Subject subject = SecurityUtils.getSubject();
    ScranUserId userId = (ScranUserId) subject.getPrincipal();

    if (userId == null)
      throw new RuntimeException("User must be logged in");

    String rateKey = userId.survey + "#" + userId.username;
    RateInfo rateInfo = rateMap.get(rateKey);

    boolean rateExceeded = false;

    long time = System.currentTimeMillis();

    if (rateInfo == null) {
      rateMap.put(rateKey, new RateInfo(1, time));
    } else {
      long timeSinceLastRequest = time - rateInfo.lastRequestTime;

      if (timeSinceLastRequest > 10000) {
        rateMap.put(rateKey, new RateInfo(1, time));
      } else if (rateInfo.requestCount >= 10) {
        rateExceeded = true;
      } else {
        rateMap.put(rateKey, new RateInfo(rateInfo.requestCount + 1, time));
      }
    }

    if (!rateExceeded) {
      System.out.println(String.format("Sending email", userId.survey, userId.username));

      Email email = new SimpleEmail();

      email.setHostName(smtpHostName);
      email.setSmtpPort(smtpPort);
      email.setCharset(EmailConstants.UTF_8);
      email.setAuthenticator(new DefaultAuthenticator(smtpUserName, smtpPassword));
      email.setSSLOnConnect(true);

      StringBuilder sb = new StringBuilder();

      for (int i = 0; i < classNames.size(); i++) {
        sb.append(String.format("%s: %s\n", classNames.get(i), messages.get(i)));

        StackTraceElement[] deobfStackTrace = deobfuscator.resymbolize(stackTraces.get(i), strongName);

        for (StackTraceElement ste : deobfStackTrace) {
          sb.append(String.format("  %s\n", ste.toString()));
        }
        sb.append("\n");
      }

      sb.append("Survey state:\n");
      sb.append(surveyState);
      sb.append("\n");

      try {
        email.setFrom("no-reply@intake24.co.uk", "Intake24");
        email
          .setSubject(String.format("Client exception (%s/%s): %s", userId.survey, userId.username, messages.get(0)));

        email.setMsg(sb.toString());

        email.addTo("bugs@intake24.co.uk");

        email.send();
      } catch (EmailException ee) {
        log.error("Failed to send e-mail notification", ee);
      }
    }

  }
}