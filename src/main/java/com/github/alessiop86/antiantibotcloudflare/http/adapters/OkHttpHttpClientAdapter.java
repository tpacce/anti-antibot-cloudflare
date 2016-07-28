package com.github.alessiop86.antiantibotcloudflare.http.adapters;

import com.github.alessiop86.antiantibotcloudflare.http.HttpRequest;
import com.github.alessiop86.antiantibotcloudflare.http.HttpResponse;
import com.github.alessiop86.antiantibotcloudflare.http.UserAgents;
import com.github.alessiop86.antiantibotcloudflare.http.exceptions.HttpException;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class OkHttpHttpClientAdapter implements HttpClientAdapter {

    private static final String USER_AGENT_HEADER = "User-Agent";
    private static final String SERVER_HEADER = "Server";
    private static final String SERVER_HEADER_CHALLENGE_VALUE = "cloudflare-nginx";
    private static final int HTTP_STATUS_CODE_CHALLENGE = 503;

    public HttpResponse getUrl(String url) throws HttpException {
        HttpRequest request = HttpRequest.Builder.withUrl(url)
                .addHeader(USER_AGENT_HEADER, UserAgents.getRandom())
                .build();
        return executeRequest(request);
    }

    public HttpResponse executeRequest(HttpRequest requestAbstraction) throws HttpException {
        OkHttpClient client = new OkHttpClient();
        try {
            HttpUrl.Builder httpUrlBuilder = getHttpUrlBuilder(requestAbstraction);
            addParams(httpUrlBuilder,requestAbstraction.getParams()); //only for GET

            Request.Builder requestBuilder = new Request.Builder();
            addHeaders(requestBuilder,requestAbstraction.getHeaders());
            requestBuilder.url(httpUrlBuilder.build());

            Response response = client.newCall(requestBuilder.build()).execute();
            return new HttpResponse(isChallenge(response), response.body().string(),
                    response.request().url().toString());
        }
        catch(IOException e) {
            throw new HttpException(e);
        }
    }

    private HttpUrl.Builder getHttpUrlBuilder(HttpRequest requestAbstraction) {
        HttpUrl url = HttpUrl.parse(requestAbstraction.getUrl());
        return url.newBuilder();
    }

    private void addParams(HttpUrl.Builder builder, HashMap<String, String> params) {
        for (Map.Entry<String,String> param : params.entrySet()) {
            builder.addQueryParameter(param.getKey(),param.getValue());
        }
    }

    private void addHeaders(Request.Builder builder, HashMap<String, String> headers) {
        for (Map.Entry<String,String> header : headers.entrySet()) {
            builder.addHeader(header.getKey(), header.getValue());
        }
    }

    private boolean isChallenge(Response response) {
        return expectedServerHeader(response.header(SERVER_HEADER))
                && expectedHttpStatusCode(response.code());
    }

    private boolean expectedServerHeader(String serverHeader) {
        return (serverHeader != null && serverHeader.equals(SERVER_HEADER_CHALLENGE_VALUE));
    }

    private boolean expectedHttpStatusCode(int httpStatusCode) {
        return httpStatusCode == HTTP_STATUS_CODE_CHALLENGE;
    }
}
