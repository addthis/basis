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

import static java.lang.System.out;

public class JolSample {

    public static void main(String[] args) throws Exception {
        out.println(VMSupport.vmDetails());
//        String sample = "heylo friend";
//        out.println(GraphLayout.parseInstance(sample).toFootprint());
//        ByteArrayReadOnlyAsciiBuf sample2 = new ByteArrayReadOnlyAsciiBuf(sample);
//        out.println(GraphLayout.parseInstance(sample2).toFootprint());
//        out.println(GraphLayout.parseInstance(sample.toCharArray()).toPrintable());
//        out.println(GraphLayout.parseInstance(sample.getBytes(StandardCharsets.UTF_8)).toPrintable());
//        out.println(ClassLayout.parseClass(byte[].class).toPrintable());
//        out.println(ClassLayout.parseClass(char[].class).toPrintable());
        out.println(ClassLayout.parseClass(boolean[].class).toPrintable());
        out.println("Instances");
        out.println(GraphLayout.parseInstance(new boolean[0]).toPrintable());
        out.println(GraphLayout.parseInstance(new boolean[1]).toPrintable());
        out.println(GraphLayout.parseInstance(new boolean[8]).toPrintable());
        out.println(GraphLayout.parseInstance(new boolean[32]).toPrintable());
//        out.println(GraphLayout.parseInstance(new byte[0]).toPrintable());
//        out.println(GraphLayout.parseInstance(new char[0]).toPrintable());
//        out.println(GraphLayout.parseInstance(new byte[1]).toPrintable());
//        out.println(GraphLayout.parseInstance(new byte[2]).toPrintable());
//        out.println(GraphLayout.parseInstance(new byte[8]).toPrintable());
//        out.println(GraphLayout.parseInstance(new byte[9]).toPrintable());

//        out.println(ClassLayout.parseClass(ByteArrayReadOnlyAsciiBuf.class).toPrintable());
    }

}
