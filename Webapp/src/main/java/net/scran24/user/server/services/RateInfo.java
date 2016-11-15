package net.scran24.user.server.services;

public class RateInfo {
  public final int requestCount;
  public final long lastRequestTime;

  public RateInfo(int requestCount, long lastRequestTime) {
    this.requestCount = requestCount;
    this.lastRequestTime = lastRequestTime;
  }
}
