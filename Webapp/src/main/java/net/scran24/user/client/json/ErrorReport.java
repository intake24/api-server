package net.scran24.user.client.json;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ErrorReport {
  @JsonProperty
  public final String userName;
  @JsonProperty
  public final String surveyId;
  @JsonProperty
  public final String exceptionClassName;
  @JsonProperty
  public final String exceptionMessage;
  @JsonProperty
  public final List<StackTraceElement> stackTrace;
  
  @JsonCreator
  public ErrorReport(@JsonProperty("userName") String userName, @JsonProperty("surveyId") String surveyId,
      @JsonProperty("exceptionClassName") String exceptionClassName, @JsonProperty("exceptionMessage") String exceptionMessage,
      @JsonProperty("stackTrace") List<StackTraceElement> stackTrace) {
    this.userName = userName;
    this.surveyId = surveyId;
    this.exceptionClassName = exceptionClassName;
    this.exceptionMessage = exceptionMessage;
    this.stackTrace = stackTrace;
  }
}
