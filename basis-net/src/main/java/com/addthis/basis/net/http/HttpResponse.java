/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.addthis.basis.net.http;

import java.io.IOException;

import java.util.Collection;

import com.addthis.basis.util.Multidict;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.util.EntityUtils;

/**
 * encapsulation of an http response. fields can be set directly or it can be
 * used to wrap an apache httpclient method that has already been executed.
 */
public class HttpResponse {
    private static final ObjectMapper mapper = new ObjectMapper();
    private final int status;
    private final String reason;
    private final Multidict headers;
    private final byte[] body;

    public HttpResponse(CloseableHttpResponse response) throws IOException {
        HttpEntity entity = response.getEntity();
        this.status = response.getStatusLine().getStatusCode();
        this.reason = response.getStatusLine().getReasonPhrase();
        this.body = (entity == null) ? null : EntityUtils.toByteArray(entity);
        this.headers = new Multidict();
        for (Header header : response.getAllHeaders()) {
            headers.put(header.getName(), header.getValue());
        }
    }


    public int getStatus() {
        return status;
    }

    public String getReason() {
        return reason;
    }

    /**
     * get the value of the specified header.  @see getHeaders to get all values
     *
     * @param httpHeader
     * @return the first value found or null if missing.
     */
    public String getHeader(HttpHeader httpHeader) {
        return getHeaders().get(httpHeader.toString());
    }

    /**
     * return all values of the specified header.
     *
     * @param httpHeader
     * @return collection containing all values or null if missing.
     */
    public Collection<String> getHeaders(HttpHeader httpHeader) {
        return getHeaders().getAll(httpHeader.toString());
    }

    public Multidict getHeaders() {
        return headers;
    }

    public byte[] getBody() {
        return body;
    }

    /**
     * read the body as json and parse into an object
     *
     * @param <T>
     * @param type type of the object
     * @return
     * @throws JsonParseException
     * @throws JsonMappingException
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    public <T> T getBody(TypeReference<T> type) throws IOException {
        return mapper.readValue(new String(getBody()), type);
    }

    /**
     * read the body as json and parse into an object
     *
     * @param <T>
     * @param type type of the object
     * @return
     * @throws JsonParseException
     * @throws JsonMappingException
     * @throws IOException
     */
    public <T> T getBody(Class<T> type) throws IOException {
        return mapper.readValue(new String(getBody()), type);
    }
}
