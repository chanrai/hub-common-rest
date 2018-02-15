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
package com.blackducksoftware.integration.hub.request;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;

import com.blackducksoftware.integration.hub.rest.HttpMethod;
import com.blackducksoftware.integration.util.Stringable;
import com.google.gson.Gson;

public class Request extends Stringable {
    private final String uri;
    private final HttpMethod method;
    private final String mimeType;
    private final Charset bodyEncoding;
    private final Map<String, String> queryParameters;
    private final Map<String, String> additionalHeaders;
    private final BodyContent bodyContent;

    public static class Builder {
        private String uri;
        private HttpMethod method;
        private String mimeType;
        private Charset bodyEncoding;
        private Map<String, String> queryParameters;
        private Map<String, String> additionalHeaders;
        private BodyContent bodyContent;

        public Builder(final String uri) {
            this.uri = uri;
            this.method = HttpMethod.GET;
            this.mimeType = ContentType.APPLICATION_JSON.getMimeType();
            this.bodyEncoding = StandardCharsets.UTF_8;
        }

        public Builder() {
            this(null);
        }

        public Builder uri(final String uri) {
            this.uri = uri;
            return this;
        }

        public Builder method(final HttpMethod method) {
            this.method = method;
            return this;
        }

        public Builder mimeType(final String mimeType) {
            this.mimeType = mimeType;
            return this;
        }

        public Builder bodyEncoding(final Charset bodyEncoding) {
            this.bodyEncoding = bodyEncoding;
            return this;
        }

        public Builder queryParameters(final Map<String, String> queryParameters) {
            this.queryParameters = queryParameters;
            return this;
        }

        public Builder addQueryParameter(final String key, final String value) {
            if (this.queryParameters == null) {
                this.queryParameters = new HashMap<>();
            }
            this.queryParameters.put(key, value);
            return this;
        }

        public Builder additionalHeaders(final Map<String, String> additionalHeaders) {
            this.additionalHeaders = additionalHeaders;
            return this;
        }

        public Builder addAdditionalHeader(final String key, final String value) {
            if (this.additionalHeaders == null) {
                this.additionalHeaders = new HashMap<>();
            }
            this.additionalHeaders.put(key, value);
            return this;
        }

        public Builder bodyContent(final BodyContent bodyContent) {
            this.bodyContent = bodyContent;
            return this;
        }

        public Request build() {
            return new Request(this);
        }
    }

    private Request(final Builder builder) {
        this.uri = builder.uri;
        this.method = builder.method;
        this.mimeType = builder.mimeType;
        this.bodyEncoding = builder.bodyEncoding;
        this.queryParameters = builder.queryParameters;
        this.additionalHeaders = builder.additionalHeaders;
        this.bodyContent = builder.bodyContent;
    }

    public Request(final String uri, final HttpMethod method, final String mimeType, final Charset bodyEncoding, final Map<String, String> queryParameters, final Map<String, String> additionalHeaders, final BodyContent bodyContent) {
        super();
        this.uri = uri;
        this.method = method;
        this.mimeType = mimeType;
        this.bodyEncoding = bodyEncoding;
        this.queryParameters = queryParameters;
        this.additionalHeaders = additionalHeaders;
        this.bodyContent = bodyContent;
    }

    public HttpEntity createHttpEntity(final Gson gson) {
        if (bodyContent == null) {
            return null;
        }
        return bodyContent.createEntity(this, gson);
    }

    public String getUri() {
        return uri;
    }

    public Map<String, String> getPopulatedQueryParameters() {
        final Map<String, String> populatedQueryParameters = new HashMap<>();
        if (getQueryParameters() != null && !getQueryParameters().isEmpty()) {
            populatedQueryParameters.putAll(getQueryParameters());
        }
        return populatedQueryParameters;
    }

    public HttpMethod getMethod() {
        return method;
    }

    public String getMimeType() {
        return mimeType;
    }

    public Charset getBodyEncoding() {
        return bodyEncoding;
    }

    public Map<String, String> getQueryParameters() {
        return queryParameters;
    }

    public Map<String, String> getAdditionalHeaders() {
        return additionalHeaders;
    }

    public BodyContent getBodyContent() {
        return bodyContent;
    }

}
