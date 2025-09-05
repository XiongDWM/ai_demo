package com.xiongdwm.ai_demo.utils.http;

public enum ServerChosen {
    FIBER_SERVER_DEV_2("http://192.168.0.77:18081"),
    FIBER_SERVER_DEV_1("http://192.168.0.66:18888"),
    FIBER_SERVER("http://192.168.0.251:8300");
    private final String baseurl;
    ServerChosen(String baseurl) { this.baseurl = baseurl; }
    public String getBaseurl() { return baseurl; }
}
