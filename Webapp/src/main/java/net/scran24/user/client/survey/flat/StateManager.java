/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.client.survey.flat;

import java.util.logging.Logger;

import net.scran24.common.client.CurrentUser;
import net.scran24.user.client.survey.portionsize.experimental.PortionSizeScriptManager;

import org.workcraft.gwt.shared.client.Callback;
import org.workcraft.gwt.shared.client.Option;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.History;

/**
 * Manages client-side survey serialisation, cross-session persistence and
 * history.
 */
public class StateManager {
  private Survey currentState;
  private int historyEventCounter = 0;

  private final Logger log = Logger.getLogger(StateManager.class.getSimpleName());

  public final String version_id;
  public final String scheme_id;

  public Survey getCurrentState() {
    return currentState;
  }

  public void makeHistoryEntry() {
    // log.info("Making history entry #" + historyEventCounter);
    // new RuntimeException().printStackTrace();
    // log.info(SurveyXmlSerialiser.toXml(currentState));

    History.newItem(Integer.toString(historyEventCounter), false);
    StateManagerUtil.setHistoryState(CurrentUser.getUserInfo().userName, historyEventCounter, currentState);
    historyEventCounter++;
  }

  public void updateState(Survey newState, boolean makeHistoryEntry) {
    currentState = newState;

    if (makeHistoryEntry) {
      // log.info ("Making history entry");
      makeHistoryEntry();
    }

    StateManagerUtil.setLatestState(CurrentUser.getUserInfo().userName, currentState, scheme_id, version_id);
    // log.info("Updated latest state");

    // updateUi.call(newState);
  }

  public StateManager(Survey initialState, String scheme_id, String version_id, final Callback showNextPage,
      final PortionSizeScriptManager scriptManager) {
    this.scheme_id = scheme_id;
    this.version_id = version_id;
    this.currentState = initialState;

    log.info("Making initial history entry");
    makeHistoryEntry();

    History.addValueChangeHandler(new ValueChangeHandler<String>() {
      @Override
      public void onValueChange(ValueChangeEvent<String> event) {

        try {
          final int state_id = Integer.parseInt(event.getValue());

          StateManagerUtil.getHistoryState(CurrentUser.getUserInfo().userName, state_id, scriptManager)
            .accept(new Option.SideEffectVisitor<Survey>() {
              @Override
              public void visitSome(Survey item) {
                log.info("Switching to historical state #" + state_id);
                updateState(item, false);
                showNextPage.call();
              }

              @Override
              public void visitNone() {
                log.warning("Failed to load historical state, keeping current state");
              }
            });
        } catch (NumberFormatException e) {
          // Ignore malformed state ids
        }
      }
    });
  }
}
