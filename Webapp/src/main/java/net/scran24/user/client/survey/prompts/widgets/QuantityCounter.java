package net.scran24.user.client.survey.prompts.widgets;

import java.util.ArrayList;

import org.workcraft.gwt.shared.client.Function1;
import org.workcraft.gwt.shared.client.Pair;

import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;

import net.scran24.common.client.widgets.LabelledCounter;
import net.scran24.user.client.survey.prompts.messages.PromptMessages;

public class QuantityCounter extends Composite {

  private static final PromptMessages messages = PromptMessages.Util.getInstance();

  public final LabelledCounter wholeCounter;
  public final LabelledCounter fractionalCounter;
  public final Label wholeLabel;

  public final double min;
  public final double max;
  
  public double getValue() {
    return wholeCounter.getValue() + fractionalCounter.getValue();
  }

  private final Function1<Double, Boolean> validate = new Function1<Double, Boolean>() {
    @Override
    public Boolean apply(Double argument) {
      double v = getValue();
      return v >= min && v <= max;
    }
  };

  public QuantityCounter(double min, double max, double init) {
    this.min = min;
    this.max = max;

    final FlowPanel panel = new FlowPanel();

    ArrayList<Pair<String, Double>> wholeLabels = new ArrayList<Pair<String, Double>>();

    NumberFormat nf = NumberFormat.getDecimalFormat();

    for (int i = 0; i < 31; i++) {
      wholeLabels.add(Pair.create(nf.format(i), (double) i));
    }

    ArrayList<Pair<String, Double>> fracLabels = new ArrayList<Pair<String, Double>>();

    fracLabels.add(Pair.create(messages.quantity_noFraction(), 0.0));
    fracLabels.add(Pair.create(messages.quantity_oneFourth(), 0.25));
    fracLabels.add(Pair.create(messages.quantity_oneHalf(), 0.5));
    fracLabels.add(Pair.create(messages.quantity_threeFourths(), 0.75));
    
    int startWholeIndex = (int)Math.floor(init);

    double frac = init - Math.floor(init);
    int startFractionalIndex = 0;

    if (frac < 0.25) 
      startFractionalIndex = 0;
    else if (frac < 0.5)
      startFractionalIndex = 1;
    else if (frac < 0.75)
      startFractionalIndex = 2;
    else
      startFractionalIndex = 3;

    wholeCounter = new LabelledCounter(wholeLabels, startWholeIndex, validate);

    wholeCounter.addStyleName("intake24-quantity-prompt-whole-counter");
    wholeCounter.getElement().setId("intake24-quantity-prompt-whole-counter");

    fractionalCounter = new LabelledCounter(fracLabels, startFractionalIndex, validate);
    fractionalCounter.addStyleName("intake24-quantity-prompt-frac-counter");
    fractionalCounter.getElement().setId("intake24-quantity-prompt-frac-counter");

    panel.add(wholeCounter);
    wholeLabel = new Label(messages.quantity_wholeItemsLabel());
    wholeLabel.addStyleName("intake24-quantity-prompt-whole-label");
    panel.add(wholeLabel);
    panel.add(fractionalCounter);

    initWidget(panel);
  }
}
