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

import com.addthis.basis.net.http.HttpResponse;
import com.addthis.basis.util.AdjustableSemaphore;

import org.apache.http.client.methods.HttpRequestBase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.ServiceUnavailableException;

import java.io.IOException;

/**
 * This class is a wrapper class for HttpUtil that gates the execute method with an adjustable semaphore.
 * When limit is reached, any attempts to send request result in <link>ServiceUnavailableException</link> being thrown.
 */
public abstract class ConstrainedHttpClient {

    private static final Logger logger = LoggerFactory.getLogger(ConstrainedHttpClient.class);

    protected AdjustableSemaphore semaphore;

    public ConstrainedHttpClient(int maxConcurrentRequests) {
        semaphore = new AdjustableSemaphore(maxConcurrentRequests);
    }

    /**
     * Executes the http method specified while protecting the http client with a semaphore. If the maximum number
     * of outstanding requests has been reached it will throw a ServiceUnavailableException. The other exceptions are
     * thrown by the standard http client executeMethod.
     *
     * @return
     * @throws ServiceUnavailableException If the limit of concurrent connections has been reached.
     * @throws IOException                 If an I/O (transport) error occurs. Some transport exceptions cannot be recovered from.
     */
    public HttpResponse execute(HttpRequestBase request, int timeoutms) throws ServiceUnavailableException, IOException {
        boolean semaphoreAcquired = false;
        HttpResponse response = null;
        try {
            semaphoreAcquired = semaphore.tryAcquire();
            if (!semaphoreAcquired) {
                throw new ServiceUnavailableException("Reached limit of " + getCurrentRequests() + " concurrent requests");
            } else {
                logger.trace("Semaphore was acquired. Remaining: {} ", semaphore.availablePermits());
                response = HttpUtil.execute(request, timeoutms);
            }
        } finally {
            if (semaphoreAcquired) {
                semaphore.release();
            }
        }
        return response;
    }

    /**
     * @return Returns the number of outstanding requests that have not returned yet from http client.
     */
    public int getCurrentRequests() {
        return semaphore.maxPermits() - semaphore.availablePermits();
    }

    /**
     * Resets the maximum allowed concurrent requests
     *
     * @param maxConcurrentRequests - the new maximum
     */
    public void setMaxConcurrentRequests(int maxConcurrentRequests) {
        semaphore.setMaxPermits(maxConcurrentRequests);
    }
}
