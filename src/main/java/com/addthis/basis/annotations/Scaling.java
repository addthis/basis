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
package com.addthis.basis.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Marker annotation that suggests how often a method/ constructor should be called or how many objects
 * of a given type should be kept around at the same time. These are merely guidelines for downstream users.
 */
@Documented
@Target({ElementType.ANNOTATION_TYPE, ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR})
public @interface Scaling {
    
    public Scaling.Scale value();

    public enum Scale {
        /** Methods should have an imperceivable cost, and objects should cost no more than their pointers. **/
        FREE,

        /** The smallest perceivable cost, and usually only belonging to an enclosing piece of DATA. ~O(1) **/
        ATOM,

        /**
         * Either intended to represent the core concept of "data" in the application or appropriate to call
         * on or from such representations. For example, leaves in a tree, or elements in a large list. If
         * they are likely to be highly transient, EVENT may be more appropriate.
         *
         * Memory footprint, batch, and repeated operations were a primary consideration in design.
         */
        DATA,

        /**
         * Similar to DATA, but more likely to occur and exist transiently throughout an application's
         * lifetime. For example, logging events, incoming messages, and so on.
         *
         * Memory footprint may not have been a high priority, but construction probably was (perhaps an
         * object recycler was used). Likewise, methods should be called a relatively constant number of
         * times per event.
         */
        EVENT,

        /**
         * Intended to be invoked as part of setting up processing/ logic for data/ events. Should not be called
         * more often than setup needs to be reconfigured, but a good rule of thumb might be something like an
         * order of magnitude (or two) less than those of data/ events.
         */
        SETUP,

        /**
         * Appropriate for a relatively small (possibly one) number per task/ thread. In this context, there
         * should be a constant number of "threads" in the application, and so there should be roughly somewhere
         * between two and sixty times more of these relative to the subsequent APPLICATION category.
         */
        THREAD,

        /**
         * There should be a small number of objects/ calls of this type per application (although the definition
         * of application is certainly up to the user). The most common case is probably one, but without the
         * strictness implied by SINGLETON. Anything more than half a dozen or so might be better classified as
         * either THREAD or SETUP.
         */
        APPLICATION,

        /**
         * There should really be one and only one of these objects or calls to this method. There may possibly
         * be very transiently two if something monolithic is being reconfigured, or some testing is being done.
         * Developers should in no way rely on this annotation to enforce singleton semantics. However, if you
         * find yourself with more than a few of these, it is a strong hint that you may need to rethink your
         * approach or modify the library source.
         */
        SINGLETON
    }
}
