/**
 * hub-common-rest
 *
 * Copyright (C) 2018 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.blackducksoftware.integration.hub.rest;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.NTCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;

import com.blackducksoftware.integration.exception.EncryptionException;
import com.blackducksoftware.integration.exception.IntegrationException;
import com.blackducksoftware.integration.hub.proxy.ProxyInfo;
import com.blackducksoftware.integration.hub.rest.exception.IntegrationRestException;
import com.blackducksoftware.integration.log.IntLogger;
import com.blackducksoftware.integration.log.LogLevel;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;

/**
 * The parent class of all Hub connections.
 */
public abstract class RestConnection {
    private static final String ERROR_MSG_PROXY_INFO_NULL = "A RestConnection's proxy information cannot be null";

    public static final String JSON_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSX";
    public static final String X_CSRF_TOKEN = "X-CSRF-TOKEN";

    public final Gson gson = new GsonBuilder().setDateFormat(JSON_DATE_FORMAT).create();
    public final JsonParser jsonParser = new JsonParser();
    public final Map<String, String> commonRequestHeaders = new HashMap<>();
    public final URL hubBaseUrl;
    public int timeout = 120;
    private final ProxyInfo proxyInfo;
    public boolean alwaysTrustServerCertificate;
    public IntLogger logger;

    private final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
    private final HttpClientBuilder clientBuilder = HttpClientBuilder.create();
    private final RequestConfig.Builder defaultRequestConfigBuilder = RequestConfig.custom();

    private HttpClient client;

    public static Date parseDateString(final String dateString) throws ParseException {
        final SimpleDateFormat sdf = new SimpleDateFormat(JSON_DATE_FORMAT);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.parse(dateString);
    }

    public static String formatDate(final Date date) {
        final SimpleDateFormat sdf = new SimpleDateFormat(JSON_DATE_FORMAT);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(date);
    }

    public RestConnection(final IntLogger logger, final URL hubBaseUrl, final int timeout, final ProxyInfo proxyInfo) {
        this.logger = logger;
        this.hubBaseUrl = hubBaseUrl;
        this.timeout = timeout;
        this.proxyInfo = proxyInfo;
    }

    public void connect() throws IntegrationException {
        addBuilderConnectionTimes();
        addBuilderProxyInformation();
        addBuilderAuthentication();
        assembleClient();
        setClient(clientBuilder.build());
        clientAuthenticate();
    }

    public abstract void addBuilderAuthentication() throws IntegrationException;

    public abstract void clientAuthenticate() throws IntegrationException;

    private void addBuilderConnectionTimes() {
        defaultRequestConfigBuilder.setConnectTimeout(timeout);
        defaultRequestConfigBuilder.setSocketTimeout(timeout);
        defaultRequestConfigBuilder.setConnectionRequestTimeout(timeout);
    }

    private void assembleClient() {
        clientBuilder.setDefaultCredentialsProvider(credentialsProvider);
        clientBuilder.setDefaultRequestConfig(defaultRequestConfigBuilder.build());
    }

    private void addBuilderProxyInformation() throws IntegrationException {
        if (this.proxyInfo == null) {
            throw new IllegalStateException(ERROR_MSG_PROXY_INFO_NULL);
        }

        if (this.proxyInfo.shouldUseProxyForUrl(hubBaseUrl)) {
            defaultRequestConfigBuilder.setProxy(getProxyHttpHost());
            try {
                addProxyCredentials();
            } catch (IllegalArgumentException | EncryptionException ex) {
                throw new IntegrationException(ex);
            }
        }
    }

    public HttpHost getProxyHttpHost() {
        final HttpHost httpHost = new HttpHost(this.proxyInfo.getHost(), this.proxyInfo.getPort());
        return httpHost;
    }

    public void addProxyCredentials() throws IllegalArgumentException, EncryptionException {
        final org.apache.http.auth.Credentials creds = new NTCredentials(this.proxyInfo.getUsername(), this.proxyInfo.getDecryptedPassword(), this.proxyInfo.getNtlmWorkstation(), this.proxyInfo.getNtlmDomain());
        credentialsProvider.setCredentials(new AuthScope(this.proxyInfo.getHost(), this.proxyInfo.getPort()), creds);
    }

    // public RequestBody createJsonRequestBody(final String content) {
    // return createJsonRequestBody("application/json", content);
    // }
    //
    // public RequestBody createJsonRequestBody(final String mediaType, final String content) {
    // return RequestBody.create(MediaType.parse(mediaType), content);
    // }
    //
    // public RequestBody createFileRequestBody(final String mediaType, final File file) {
    // return RequestBody.create(MediaType.parse(mediaType), file);
    // }
    //
    // public RequestBody createEncodedFormBody(final Map<String, String> content) {
    // final FormBody.Builder builder = new FormBody.Builder();
    // for (final Entry<String, String> contentEntry : content.entrySet()) {
    // builder.add(contentEntry.getKey(), contentEntry.getValue());
    // }
    // return builder.build();
    // }
    //
    // public Request createGetRequest(final HttpUrl httpUrl) {
    // return createGetRequest(httpUrl, "application/json");
    // }
    //
    // public Request createGetRequest(final HttpUrl httpUrl, final String mediaType) {
    // final Map<String, String> headers = new HashMap<>();
    // headers.put("Accept", mediaType);
    // return createGetRequest(httpUrl, headers);
    // }
    //
    // public Request createGetRequest(final HttpUrl httpUrl, final Map<String, String> headers) {
    // return getRequestBuilder(headers).url(httpUrl).get().build();
    // }
    //
    // public Request createPostRequest(final HttpUrl httpUrl, final RequestBody body) {
    // return getRequestBuilder().url(httpUrl).post(body).build();
    // }
    //
    // public Request createPostRequest(final HttpUrl httpUrl, final Map<String, String> headers, final RequestBody body) {
    // return getRequestBuilder(headers).url(httpUrl).post(body).build();
    // }
    //
    // public Request createPutRequest(final HttpUrl httpUrl, final RequestBody body) {
    // return getRequestBuilder().url(httpUrl).put(body).build();
    // }
    //
    // public Request createDeleteRequest(final HttpUrl httpUrl) {
    // return getRequestBuilder().url(httpUrl).delete().build();
    // }
    //
    //

    public RequestBuilder getRequestBuilder(final HttpMethod method) {
        return getRequestBuilder(method, null);
    }

    public RequestBuilder getRequestBuilder(final HttpMethod method, final Map<String, String> additionalHeaders) {
        final RequestBuilder requestBuilder = RequestBuilder.create(method.name());

        final Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.putAll(commonRequestHeaders);
        if (additionalHeaders != null && !additionalHeaders.isEmpty()) {
            requestHeaders.putAll(additionalHeaders);
        }
        for (final Entry<String, String> header : requestHeaders.entrySet()) {
            requestBuilder.addHeader(header.getKey(), header.getValue());
        }
        return requestBuilder;
    }

    public HttpResponse createResponse(final HttpUriRequest request) throws IntegrationException {
        final long start = System.currentTimeMillis();
        logMessage(LogLevel.TRACE, "starting request: " + request.getURI().toString());
        try {
            return handleExecuteClientCall(request, 0);
        } finally {
            final long end = System.currentTimeMillis();
            logMessage(LogLevel.TRACE, String.format("completed request: %s (%d ms)", request.getURI().toString(), end - start));
        }
    }

    private HttpResponse handleExecuteClientCall(final HttpUriRequest request, final int retryCount) throws IntegrationException {
        if (client != null) {
            try {
                final URI uri = request.getURI();
                final String urlString = request.getURI().toString();
                if (alwaysTrustServerCertificate && uri.getScheme().equalsIgnoreCase("https") && logger != null) {
                    logger.debug("Automatically trusting the certificate for " + urlString);
                }
                logRequestHeaders(request);
                final HttpResponse response = client.execute(request);
                final int statusCode = response.getStatusLine().getStatusCode();
                final String statusMessage = response.getStatusLine().getReasonPhrase();
                if (statusCode < 200 || statusCode > 299) {
                    if (statusCode == 401 && retryCount < 2) {
                        connect();
                        final HttpUriRequest newRequest = RequestBuilder.copy(request).build();
                        return handleExecuteClientCall(newRequest, retryCount + 1);
                    } else {
                        throw new IntegrationRestException(statusCode, statusMessage,
                                String.format("There was a problem trying to %s this item: %s. Error: %s %s", request.getMethod(), urlString, statusCode, statusMessage));
                    }
                }
                logResponseHeaders(response);
                return response;
            } catch (final IOException e) {
                throw new IntegrationException(e.getMessage(), e);
            }
        } else {
            connect();
            final HttpUriRequest newRequest = RequestBuilder.copy(request).build();
            return handleExecuteClientCall(newRequest, retryCount);
        }
    }

    private void logMessage(final LogLevel level, final String txt) {
        if (logger != null) {
            if (level == LogLevel.ERROR) {
                logger.error(txt);
            } else if (level == LogLevel.WARN) {
                logger.warn(txt);
            } else if (level == LogLevel.INFO) {
                logger.info(txt);
            } else if (level == LogLevel.DEBUG) {
                logger.debug(txt);
            } else if (level == LogLevel.TRACE) {
                logger.trace(txt);
            }
        }
    }

    private boolean isDebugLogging() {
        return logger != null && logger.getLogLevel() == LogLevel.TRACE;
    }

    protected void logRequestHeaders(final HttpUriRequest request) {
        if (isDebugLogging()) {
            final String requestName = request.getClass().getSimpleName();
            logMessage(LogLevel.TRACE, requestName + " : " + request.toString());
            logHeaders(requestName, request.getAllHeaders());
        }
    }

    protected void logResponseHeaders(final HttpResponse response) {
        if (isDebugLogging()) {
            final String responseName = response.getClass().getSimpleName();
            logMessage(LogLevel.TRACE, responseName + " : " + response.toString());
            logHeaders(responseName, response.getAllHeaders());
        }
    }

    private void logHeaders(final String requestOrResponseName, final Header[] headers) {
        if (headers != null && headers.length > 0) {
            logMessage(LogLevel.TRACE, requestOrResponseName + " headers : ");
            for (final Header header : headers) {
                if (header.getElements() != null && header.getElements().length > 0) {
                    for (final HeaderElement headerElement : header.getElements()) {
                        logMessage(LogLevel.TRACE, String.format("Header %s : %s", headerElement.getName(), headerElement.getValue()));
                    }
                } else {
                    logMessage(LogLevel.TRACE, String.format("Header %s : %s", header.getName(), header.getValue()));
                }
            }
        } else {
            logMessage(LogLevel.TRACE, requestOrResponseName + " does not have any headers.");
        }
    }

    @Override
    public String toString() {
        return "RestConnection [baseUrl=" + hubBaseUrl + "]";
    }

    public HttpClient getClient() {
        return client;
    }

    public void setClient(final HttpClient client) {
        this.client = client;
    }

    public ProxyInfo getProxyInfo() {
        return proxyInfo;
    }
}
