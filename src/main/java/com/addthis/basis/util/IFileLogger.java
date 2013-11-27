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
package com.addthis.basis.util;


/**
 * Interface for a trivially simple "logger" that just writes lines
 * and can be turned on and off. No concept of log levels or other
 * nice things.  In general, log4j, slf4j, etc should be preferred to
 * this.
 */
public interface IFileLogger {

    public boolean isLogging();

    public void startLogging();

    public void stopLogging();

    public void writeLine(String line);
}
