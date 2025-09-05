package com.xiongdwm.ai_demo.utils.http;




import org.springframework.stereotype.Component;
import okhttp3.OkHttpClient;

@Component
public class HttpClientManager {
    private final OkHttpClient httpClient;

    public enum HttpMethod {
        GET,
        POST,
        PUT,
        DELETE,
        PATCH
    }

    public HttpClientManager() {
        this.httpClient = new OkHttpClient();
    }
  

    public HttpRequestBuilder post(ServerChosen server, String path) {
        return new HttpRequestBuilder(httpClient, server.getBaseurl() + path, HttpMethod.POST);
    }

    public HttpRequestBuilder get(ServerChosen server, String path) {
        return new HttpRequestBuilder(httpClient, server.getBaseurl() + path, HttpMethod.GET);
    }

    public HttpRequestBuilder put(ServerChosen server, String path) {
        return new HttpRequestBuilder(httpClient, server.getBaseurl() + path, HttpMethod.PUT);
    }

    public HttpRequestBuilder delete(ServerChosen server, String path) {
        return new HttpRequestBuilder(httpClient, server.getBaseurl() + path, HttpMethod.DELETE);
    }
    public HttpRequestBuilder patch(ServerChosen server, String path) {
        return new HttpRequestBuilder(httpClient, server.getBaseurl() + path, HttpMethod.PATCH);
    }

}
