/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.sony.internal;

import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 *
 * @author Tim Roberts - Initial contribution
 *
 */
@NonNullByDefault
public class SonyMatcherUtils {
    public static boolean matches(List<? extends SonyMatcher> list1, List<? extends SonyMatcher> list2,
            Comparator<SonyMatcher> comparator) {
        return matches(list1, list2, comparator, true);
    }

    public static boolean matches(List<? extends SonyMatcher> list1, List<? extends SonyMatcher> list2,
            Comparator<SonyMatcher> comparator, boolean ignoreDeleted) {
        Objects.requireNonNull(list1, "list1 cannot be null");
        Objects.requireNonNull(list2, "list2 cannot be null");
        Objects.requireNonNull(comparator, "comparator cannot be null");

        final List<? extends SonyMatcher> e1 = list1.stream().filter(e->e != null).sorted(comparator).collect(Collectors.toList());
        final List<? extends SonyMatcher> e2 = list2.stream().filter(e -> e != null).sorted(comparator)
                .collect(Collectors.toList());

        // Special case - bother are empty so we return true!
        if (e1.size() == 0 && e2.size() == 0) {
            return true;
        }

        // If there are no elements in list1 but elements in list2, they are all 'deleted' and
        // return how the ignoreDeleted switch is
        if (e1.size() == 0 && e2.size() > 0) {
            return ignoreDeleted;
        }

        // If elements in list1 but none in list2, the all are 'inserted' - return false
        if (e1.size() > 0 && e2.size() == 0) {
            return false;
        }

        // At this point, we are insured atleast one record in both lists...
        //   loop through list1 and try to find a match in list2
        //   if found, then we have a match - continue on
        //   if not found, the record is 'inserted' - return false
        //   if, during finding, we skip over a list2 element *and* !ignoreDeleted - return false
        int i1 = 0, i2 = 0;
        for (; i1 < e1.size(); i1++) {
            final SonyMatcher o1 = e1.get(i1);
            while (!o1.matches(e2.get(i2++))) {
                // We ran out of elements - must be new - return false;
                if (i2 >= e2.size()) {
                    return false;
                }

                // We skipped a list2 element (deleted) - if we aren't
                // ignoring deleted - return false
                if (!ignoreDeleted) {
                    return false;
                }
            }
        }

        // At this point, we matched every element in list1 and either matched every element in
        // list2 or skipped over deleted ones (because ignoreDelete was true).
        // So return true!
        return true;
    }
}
