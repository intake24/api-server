/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.common.client.widgets;

import java.util.ArrayList;

import net.scran24.common.client.WidgetFactory;

import org.workcraft.gwt.shared.client.Function1;
import org.workcraft.gwt.shared.client.Pair;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;

public class LabelledCounter extends Composite {
  public int index;

  public final Label box = new Label();

  private ArrayList<Pair<String, Double>> values;

  public void update() {
    box.setText(values.get(index).left);
  }

  public double getValue() {
    return values.get(index).right;
  }

  public LabelledCounter(final ArrayList<Pair<String, Double>> values, int startIndex) {
    this(values, startIndex, new Function1<Double, Boolean>() {
      @Override
      public Boolean apply(Double argument) {
        return true;
      }
    });
  }

  public LabelledCounter(final ArrayList<Pair<String, Double>> values, int startIndex, final Function1<Double, Boolean> validate) {
    this.values = values;
    index = startIndex;

    VerticalPanel panel = new VerticalPanel();

    panel.setStyleName("counterPanel");

    panel.setHorizontalAlignment(VerticalPanel.ALIGN_CENTER);

    Button inc = WidgetFactory.createButton("▲", new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        if (index < (LabelledCounter.this.values.size() - 1)) {
          index++;
          if (validate.apply(getValue()))
            update();
          else
            index--;
        }
      }
    });

    inc.setStyleName("counterIncButton");

    Button dec = WidgetFactory.createButton("▼", new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        if (index > 0) {
          index--;
          if (validate.apply(getValue()))
            update();
          else
            index++;
        }
      }
    });

    dec.setStyleName("counterDecButton");

    box.setStyleName("counterTextBox");
    box.getElement().getStyle().setFontSize(100, Unit.PCT);
    box.getElement().getStyle().setHeight(1.2, Unit.EM);

    panel.add(inc);
    panel.add(box);
    panel.add(dec);

    update();

    initWidget(panel);
  }
}
