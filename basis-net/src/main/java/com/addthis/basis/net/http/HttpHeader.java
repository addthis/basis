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

/**
 * Enumeration of supported HTTP headers. taken from
 * http://en.wikipedia.org/wiki/List_of_HTTP_headers
 */
public enum HttpHeader {

    // common
    ACCEPT_RANGES("Accept-Ranges"),
    CACHE_CONTROL("Cache-Control"),
    CONTENT_LENGTH("Content-Length"),
    CONTENT_TYPE("Content-Type"),
    DATE("Date"),
    PRAGMA("Pragma"),
    VIA("Via"),

    // request
    ACCEPT("Accept"),
    ACCEPT_CHARSET("Accept-Charset"),
    ACCEPT_ENCODING("Accept-Encoding"),
    ACCEPT_LANGUAGE("Accept-Language"),
    AUTHORIZATION("Authorization"),
    COOKIE("Cookie"),
    CONNECTION("Connection"),
    EXPECT("Expect"),
    FROM("From"),
    HOST("Host"),
    IF_MATCH("If-Match"),
    IF_MODIFIED_SINCE("If-Modified-Since"),
    IF_NONE_MATCH("If-None-Match"),
    IF_UNMODIFIED_SINCE("If-Unmodified-Since"),
    MAX_FORWARDS("Max-Forwards"),
    PROXY_AUTHORIZATION("Proxy-Authorization"),
    RANGE("Range"),
    REFERER("Referer"),
    TE("TE"),
    UPGRADE("Upgrade"),
    USER_AGENT("User-Agent"),
    WARN("Warn"),

    // response
    AGE("Age"),
    ALLOW("Allow"),
    CONTENT_ENCODING("Content-Encoding"),
    CONTENT_LANGUAGE("Content-Language"),
    CONTENT_LOCATION("Content-Location"),
    CONTENT_DISPOSITION("Content-Disposition"),
    CONTENT_MD5("Content-MD5"),
    CONTENT_RANGE("Content-Range"),
    ETAG("ETag"),
    EXPIRES("Expires"),
    KEEP_ALIVE("Keep-Alive"),
    LAST_MODIFIED("Last-Modified"),
    LOCATION("Location"),
    P3P("P3P"),
    PROXY_AUTHENTICATE("Proxy-Authenticate"),
    RETRY_AFTER("Retry-After"),
    SERVER("Server"),
    SET_COOKIE("Set-Cookie"),
    TRAILER("Trailer"),
    TRANSFER_ENCODING("Transfer-Encoding"),
    VARY("Vary"),
    WARNING("Warning"),
    WWW_AUTHENTICATE("WWW-Authenticate"),
    RATE_LIMIT("X-Rate-Limit"),
    RATE_REMAINING("X-Rate-Remaining"),
    RATE_WINDOW("X-Rate-NextWindow");

    private String label;

    private HttpHeader(String label) {
        this.label = label;
    }

    public String toString() {
        return label;
    }
}
