/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.user.client.survey.prompts;

import org.workcraft.gwt.shared.client.Callback1;
import org.workcraft.gwt.shared.client.Function1;

import com.google.gwt.core.shared.GWT;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;

import net.scran24.user.client.survey.SurveyStageInterface;
import net.scran24.user.client.survey.flat.Prompt;
import net.scran24.user.client.survey.flat.Survey;
import net.scran24.user.client.survey.flat.SurveyOperation;
import net.scran24.user.client.survey.prompts.messages.PromptMessages;
import net.scran24.user.shared.Meal;

public class ConfirmTimeGapPrompt implements Prompt<Survey, SurveyOperation> {

  public static interface TimeGap {
    public static interface Visitor<T> {
      public T visitBeforeMeal(int mealIndex);
      public T visitAfterMeal(int mealIndex);
      public T visitBetweenMeals(int mealIndex1, int mealIndex2);
    }

    public static class AfterMeal implements TimeGap {
      public final int mealIndex;

      public AfterMeal(int mealIndex) {
        this.mealIndex = mealIndex;
      }

      @Override
      public <T> T accept(Visitor<T> visitor) {
        return visitor.visitAfterMeal(mealIndex);
      }
    }

    public static class BeforeMeal implements TimeGap {
      public final int mealIndex;

      public BeforeMeal(int mealIndex) {
        this.mealIndex = mealIndex;
      }

      @Override
      public <T> T accept(Visitor<T> visitor) {
        return visitor.visitBeforeMeal(mealIndex);
      }
    }

    public static class BetweenMeals implements TimeGap {
      public final int mealIndex1;
      public final int mealIndex2;

      public BetweenMeals(int mealIndex1, int mealIndex2) {
        this.mealIndex1 = mealIndex1;
        this.mealIndex2 = mealIndex2;
      }

      @Override
      public <T> T accept(Visitor<T> visitor) {
        return visitor.visitBetweenMeals(mealIndex1, mealIndex2);
      }
    }

    public <T> T accept(Visitor<T> visitor);
  }

  private final PromptMessages messages = GWT.create(PromptMessages.class);
  private final TimeGap gap;
  private Survey survey;

  public ConfirmTimeGapPrompt(Survey survey, TimeGap gap) {
    this.survey = survey;
    this.gap = gap;
  }

  @Override
  public SurveyStageInterface getInterface(final Callback1<SurveyOperation> onComplete,
      final Callback1<Function1<Survey, Survey>> onIntermediateStateChange) {

    FlowPanel content = new FlowPanel();

    final SafeHtml promptText = gap.accept(new TimeGap.Visitor<SafeHtml>() {
      @Override
      public SafeHtml visitBeforeMeal(int mealIndex) {
        Meal meal = survey.meals.get(mealIndex);
        return SafeHtmlUtils
          .fromSafeConstant(messages.timeGap_promptText_beforeMeal(meal.name.toLowerCase(), meal.time.getOrDie()
            .toString()));
      }

      @Override
      public SafeHtml visitAfterMeal(int mealIndex) {
        Meal meal = survey.meals.get(mealIndex);
        return SafeHtmlUtils
          .fromSafeConstant(messages.timeGap_promptText_afterMeal(meal.name.toLowerCase(), meal.time.getOrDie()
            .toString()));
      }

      @Override
      public SafeHtml visitBetweenMeals(int mealIndex1, int mealIndex2) {
        Meal meal1 = survey.meals.get(mealIndex1);
        Meal meal2 = survey.meals.get(mealIndex2);
        return SafeHtmlUtils
          .fromSafeConstant(messages.timeGap_promptText_betweenMeals(meal1.name.toLowerCase(), meal1.time.getOrDie()
            .toString(), meal2.name.toLowerCase(),
              meal2.time.getOrDie()
                .toString()));
      }
    });

    content.add(new YesNoQuestion(promptText, messages.timeGap_addMealButtonLabel(),
        messages.timeGap_confirmTimeGapButtonLabel(), new YesNoQuestion.ResultHandler() {
          @Override
          public void handleYes() {
            onComplete.call(SurveyOperation.addMealRequest(0));
          }

          @Override
          public void handleNo() {
            onComplete.call(SurveyOperation.update(new Function1<Survey, Survey>() {
              @Override
              public Survey apply(final Survey argument) {
                return gap.accept(new TimeGap.Visitor<Survey>() {
                  @Override
                  public Survey visitBeforeMeal(int mealIndex) {
                    return argument.updateMeal(mealIndex, argument.meals.get(mealIndex)
                      .markNoMealsBefore());
                  }

                  @Override
                  public Survey visitAfterMeal(int mealIndex) {
                    return argument.updateMeal(mealIndex, argument.meals.get(mealIndex)
                      .markNoMealsAfter());
                  }

                  @Override
                  public Survey visitBetweenMeals(int mealIndex1, int mealIndex2) {
                    return argument.updateMeal(mealIndex1, argument.meals.get(mealIndex1)
                      .markNoMealsAfter())
                      .updateMeal(mealIndex2, argument.meals.get(mealIndex2)
                        .markNoMealsBefore());
                  }
                });
              }
            }, true));
          }
        }));

    return new SurveyStageInterface.Aligned(content, HasHorizontalAlignment.ALIGN_LEFT, HasVerticalAlignment.ALIGN_TOP,
        SurveyStageInterface.DEFAULT_OPTIONS);
  }

  @Override
  public String toString() {
    return "Confirm survey completion prompt";
  }
}