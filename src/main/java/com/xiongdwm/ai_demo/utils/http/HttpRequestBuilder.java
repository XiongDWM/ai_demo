package com.xiongdwm.ai_demo.utils.http;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import com.xiongdwm.ai_demo.utils.JacksonUtil;
import com.xiongdwm.ai_demo.utils.http.exception.QueryFailedException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class HttpRequestBuilder {
    private final OkHttpClient client;
    private final String url;
    private final HttpClientManager.HttpMethod method;
    private Map<String, Object> param = new HashMap<>();
    private Map<String, String> headers = new HashMap<>();

    public HttpRequestBuilder(OkHttpClient client, String url, HttpClientManager.HttpMethod method) {
        this.client = client;
        this.url = url;
        this.method = method;
    }

    public HttpRequestBuilder param(Map<String, Object> param) {
        if (param != null)
            this.param.putAll(param);
        return this;
    }

    public HttpRequestBuilder header(Map<String, String> headers) {
        if (headers != null)
            this.headers.putAll(headers);
        return this;
    }

    public <T> T retrieve(Class<T> clazz) {
        try (Response response = executeRequest()) {
            String body = response.body().string();
            return JacksonUtil.fromJsonString(body, clazz).orElseThrow(() -> new QueryFailedException("响应解析失败"));
        } catch (Exception e) {
            throw new QueryFailedException("请求失败: " + e.getLocalizedMessage());
        }
    }

    public String retrieve() {  
        try (Response response = executeRequest()) {
            return response.body().string();
        } catch (Exception e) {
            throw new QueryFailedException("请求失败: " + e.getLocalizedMessage());
        }
    }

    private Response executeRequest() {
        Request.Builder builder = new Request.Builder().url(url);
        headers.forEach(builder::addHeader);

        switch (method) {
            case POST -> {
                String jsonBody = JacksonUtil.toJsonString(param)
                        .orElseThrow(() -> new QueryFailedException("无法将参数转换为JSON"));
                RequestBody requestBody = RequestBody.create(jsonBody.getBytes(StandardCharsets.UTF_8),
                        MediaType.parse("application/json"));
                builder.post(requestBody);
            }
            case GET -> builder.get();
            case PUT -> {
                String jsonBody = JacksonUtil.toJsonString(param)
                        .orElseThrow(() -> new QueryFailedException("无法将参数转换为JSON"));
                RequestBody requestBody = RequestBody.create(jsonBody.getBytes(StandardCharsets.UTF_8),
                        MediaType.parse("application/json"));
                builder.put(requestBody);
            }
            case DELETE -> builder.delete();
            case PATCH -> {
                String jsonBody = JacksonUtil.toJsonString(param)
                        .orElseThrow(() -> new QueryFailedException("无法将参数转换为JSON"));
                RequestBody requestBody = RequestBody.create(jsonBody.getBytes(StandardCharsets.UTF_8),
                        MediaType.parse("application/json"));
                builder.patch(requestBody);
            }
        }

        try (Response response = client.newCall(builder.build()).execute()) {
            if (!response.isSuccessful())
                throw new QueryFailedException("请求失败");
            return response;
        } catch (Exception e) {
            throw new QueryFailedException("请求失败: " + e.getLocalizedMessage());
        }
    }
}
