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

package com.addthis.basis.chars;

import org.openjdk.jol.info.ClassLayout;
import org.openjdk.jol.info.GraphLayout;
import org.openjdk.jol.util.VMSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ByteArrayCharBufJol {

    private static final Logger log = LoggerFactory.getLogger(ByteArrayCharBufJol.class);

    public static void main(String[] args) throws Exception {
        log.info("various vm properties visible via the unsafe");
        log.info(VMSupport.vmDetails());

        log.info("memory layout based on class object at runtime; should see 4 'free bytes' to spend");
        log.info(ClassLayout.parseClass(ByteArrayReadOnlyAsciiBuf.class).toPrintable());

        log.info("runtime footprint for the standard java string object for 'heylo friend'");
        String sample = "heylo friend";
        log.info(GraphLayout.parseInstance(sample).toFootprint());

        log.info("runtime footprint for a ByteArrayReadOnlyUtfBuf object for 'heylo friend'");
        ByteArrayReadOnlyUtfBuf utfBuf = new ByteArrayReadOnlyUtfBuf(sample);
        log.info(GraphLayout.parseInstance(utfBuf).toFootprint());
    }

}
