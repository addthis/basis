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
package com.addthis.basis.net;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import java.net.URI;
import java.net.URISyntaxException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.addthis.basis.net.http.HttpResponse;

import com.google.common.base.Optional;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

@SuppressWarnings("unused")
public class HttpUtil {
    /**
     * HTTP GET an url and return the response body
     *
     * @param url       target url
     * @param timeoutms request timeout (use 0 to ignore)
     * @return response body
     * @throws IOException, URISyntaxException
     */
    public static HttpResponse httpGet(String url, int timeoutms)
            throws IOException, URISyntaxException {
        return httpGet(url, null, timeoutms);
    }

    /**
     * HTTP GET an url and return the response body
     *
     * @param url            target url
     * @param requestHeaders input request headers
     * @param timeoutms      request timeout (use 0 to ignore)
     * @return response body, null if the request fails
     * @throws IOException, URISyntaxException
     */
    public static HttpResponse httpGet(String url, Map<String, String> requestHeaders, int timeoutms)
            throws IOException, URISyntaxException {
        HttpGet get = new HttpGet(new URI(url)); // need this so httpclient doesn't reject non-standard hydra query urls
        if (requestHeaders != null) {
            for (Map.Entry<String, String> entry : requestHeaders.entrySet()) {
                get.addHeader(entry.getKey(), entry.getValue());
            }
        }
        return execute(get, timeoutms, Optional.absent());
    }

    /**
     * HTTP GET an url, read the response body and then return the contents as an input stream
     *
     * @param url       target url
     * @param timeoutms timeout request (use 0 to ignore)
     * @return contents of the response as an input stream
     * @throws IOException, URISyntaxException
     */
    public static InputStream httpGetAsInputStream(String url, int timeoutms)
            throws IOException, URISyntaxException {
        byte[] data = httpGet(url, null, timeoutms).getBody();
        return data == null ? null : new ByteArrayInputStream(data);
    }


    /**
     * HTTP POST to an url and return the response body
     *
     * @param url         target url
     * @param contentType request content type. null value is valid.
     * @param content     request body
     * @param timeoutms   request timeout (use 0 to ignore)
     * @return response body
     * @throws IOException
     */
    public static HttpResponse httpPost(String url, String contentType, byte[] content, int timeoutms)
            throws IOException {
        return execute(makePost(url, contentType, content), timeoutms, Optional.absent());
    }

    /**
     * HTTP POST to an url and return the response body
     *
     * @param url       target url
     * @param charset   request character set. null value is valid.
     * @param content   request body
     * @param timeoutms request timeout (use 0 to ignore)
     * @return response body
     * @throws IOException
     */
    public static HttpResponse httpPost(String url, String charset, Map<String, String> content, int timeoutms)
            throws IOException {
        return execute(makePost(url, charset, content), timeoutms, Optional.absent());
    }

    /**
     * convenience method to make a PostMethod object from some common parameters
     *
     * @param url         target url
     * @param contentType type of {@code content}. null value is valid.
     * @param content     body of post request
     * @return http post request
     */
    public static HttpPost makePost(String url, String contentType, byte[] content) {
        HttpPost post = new HttpPost(url);
        ContentType type = (contentType == null) ? null : ContentType.parse(contentType);
        post.setEntity(new ByteArrayEntity(content, type));
        return post;
    }

    /**
     * convenience method to make a PostMethod object from some common parameters
     *
     * @param url     target url
     * @param charset character set of {@code content}. null value is valid.
     * @param content key value pairs of post request
     * @return http post request
     */
    public static HttpPost makePost(String url, String charset, Map<String, String> content)
            throws UnsupportedEncodingException {
        HttpPost post = new HttpPost(url);
        List<NameValuePair> pairs = new ArrayList<>(content.size());
        for (Map.Entry<String, String> entry : content.entrySet()) {
            pairs.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
        }
        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(pairs, charset);
        post.setEntity(entity);
        return post;
    }


    /**
     * HTTP GET a list of urls and return a list of the response bodies
     * uses a thread pool to get the urls in parallel
     * gets that threw errors will have null responses
     *
     * @param urls       urls to fetch
     * @param numThreads number of threads to use
     * @param timeoutms  request timeout (use 0 to ignore)
     * @return list of response bodies (in the same order as the requests)
     */
    public static List<byte[]> httpGet(List<String> urls, int numThreads, int timeoutms) {
        ArrayList<HttpGet> methods = new ArrayList<>();
        for (String url : urls) {
            methods.add(new HttpGet(url));
        }
        List<Method> responses = execute(methods, numThreads, timeoutms);
        return extractResponses(responses);
    }

    /**
     * Execute an http method and return the response body
     *
     * @param request   method to execute
     * @param timeoutms request timeout (use 0 to ignore)
     * @return completed request
     * @throws IOException
     */
    public static HttpResponse execute(HttpUriRequest request, int timeoutms)
            throws IOException {
        HttpClientBuilder builder = HttpClientBuilder.create();
        CloseableHttpResponse response = null;
        try (CloseableHttpClient client = makeCloseableHttpClient(timeoutms, 0, Optional.absent())) {
            response = client.execute(request);
            return new HttpResponse(response);
        } finally {
            if (response != null) {
                response.close();
            }

        }
    }

    /**
     * Execute an http method and return the response body
     *
     * @param request   method to execute
     * @param timeoutms request timeout (use 0 to ignore)
     * @param credentials credentials Provider to build with Http Client. Useful for endpoints that require api tokens
     * @return completed request
     * @throws IOException
     */
    public static HttpResponse execute(HttpUriRequest request, int timeoutms, Optional<CredentialsProvider> credentials)
            throws IOException {
        CloseableHttpResponse response = null;
        try (CloseableHttpClient client = makeCloseableHttpClient(timeoutms, 0, credentials)) {
            response = client.execute(request);
            return new HttpResponse(response);
        } finally {
            if (response != null) {
                response.close();
            }

        }
    }

    /**
     * Execute an http method and return the response body
     *
     * @param request    method to execute
     * @param timeoutms  request timeout (use 0 to ignore)
     * @param numRetries number of times to retry request
     * @return completed request
     * @throws IOException
     */
    public static HttpResponse execute(HttpUriRequest request, int timeoutms, int numRetries)
            throws IOException {
        CloseableHttpResponse response = null;
        try (CloseableHttpClient client = makeCloseableHttpClient(timeoutms, numRetries, Optional.absent())) {
            response = client.execute(request);
            return new HttpResponse(response);
        } finally {
            if (response != null) {
                response.close();
            }

        }
    }


    /**
     * Execute a list of http methods and return a list of the response bodies
     * uses a thread pool to execute the methods in parallel
     * methods that threw errors will have null responses
     *
     * @param requests   methods to execute
     * @param numThreads number of threads to use
     * @param timeoutms  request timeout
     * @return completed requests (for convenience)
     */
    public static List<Method> execute(List<? extends HttpRequestBase> requests, int numThreads, int timeoutms) {
        return execute(requests, numThreads, timeoutms, -1);
    }

    /**
     * Execute a list of http methods and return a list of the response bodies
     * uses a thread pool to execute the methods in parallel
     * methods that threw errors will have null responses
     *
     * @param requests   methods to execute
     * @param numThreads number of threads to use
     * @param timeoutms  request timeout
     * @param numRetries of retries
     * @return completed requests (for convenience)
     */
    public static List<Method> execute(List<? extends HttpRequestBase> requests,
                                       int numThreads, int timeoutms, int numRetries) {
        List<Method> responses = new ArrayList<>();
        ExecutorService executor = Executors.newFixedThreadPool(Math.min(numThreads, requests.size()));
        CloseableHttpClient client = makeCloseableHttpClient(timeoutms, numRetries, Optional.absent());
        try {
            for (int i = 0; i < requests.size(); i++) {
                Method method = new Method(client, requests.get(i), timeoutms);
                responses.add(i, method);
                executor.execute(method);
            }
            executor.shutdown();
            try {  // wait for the executor to finish
                if (timeoutms > 0) { // interrupt if it's taking abnormally long
                    executor.awaitTermination(2 * timeoutms * requests.size() / numThreads, TimeUnit.MILLISECONDS);
                } else {
                    executor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
                }
            } catch (InterruptedException ignored) {
            }
            if (!executor.isTerminated()) {
                executor.shutdownNow();
                executor = null;
            }
            return responses;
        } finally {
            if (executor != null) {
                executor.shutdownNow();
            }
            try {
                client.close();
            } catch (IOException ignored) {
            }
        }
    }

    /**
     * Internal function to create http clients for fetching
     * @param timeoutms The timeout, which is applied to the connect, connection request, and socket timeouts. Ignored if <=0
     * @param numRetries The number of retries. Ignored if <0
     * @return A CloseableHttpClient with the specified parameters set
     */
    private static CloseableHttpClient makeCloseableHttpClient(int timeoutms, int numRetries, Optional<CredentialsProvider> credentials) {
        HttpClientBuilder builder = HttpClientBuilder.create();
        if(credentials.isPresent()) { builder.setDefaultCredentialsProvider(credentials.get()); }
        if (numRetries >= 0) {
            DefaultHttpRequestRetryHandler retryHandler =
                    new DefaultHttpRequestRetryHandler(numRetries, false);
            builder = builder.setRetryHandler(retryHandler);
        }
        if (timeoutms > 0) {
            RequestConfig config = RequestConfig.custom()
                    .setConnectTimeout(timeoutms)
                    .setConnectionRequestTimeout(timeoutms)
                    .setSocketTimeout(timeoutms).build();
            builder.setDefaultRequestConfig(config);
        }
        return builder.build();
    }

    /**
     * internal helper class for use with executor
     */
    private static class Method implements Runnable {
        private final CloseableHttpClient client;
        private final HttpContext context;
        public final HttpRequestBase request;
        public int status;
        public byte[] body;


        public Method(CloseableHttpClient client, HttpRequestBase request, int timeoutms) {
            this.client = client;
            this.request = request;
            this.context = HttpClientContext.create();
        }

        @Override
        public void run() {
            CloseableHttpResponse response = null;
            try {
                response = client.execute(request, context);
                HttpEntity entity = response.getEntity();
                status = response.getStatusLine().getStatusCode();
                body = entity == null ? null : EntityUtils.toByteArray(entity); // need this to make method keep response body for later
            } catch (Exception ignored) {
            } finally {
                try {
                    if (response != null) {
                        response.close();
                    }
                } catch (Exception ignored) {
                }
            }
        }
    }

    /**
     * convenience method to convert a list of HttpMethods into a list of their response bodies
     */
    public static List<byte[]> extractResponses(List<Method> methods) {
        byte[][] responses = new byte[methods.size()][];
        int i = 0;
        for (Method method : methods) {
            if (method.status < 400) {
                responses[i] = method.body;
            }
            i++;
        }
        return Arrays.asList(responses);
    }
}