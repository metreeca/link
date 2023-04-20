/*
 * Copyright © 2023 Metreeca srl
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.metreeca.link;

import java.util.*;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;
import static java.util.stream.Collectors.*;

/**
 * Shape validation trace.
 */
public final class Trace {

    public static Trace trace(final String error) {

        if ( error == null ) {
            throw new NullPointerException("null error");
        }

        return new Trace(error.isBlank() ? List.of() : List.of(error.trim()), Map.of());
    }

    public static Trace trace(final String field, final Trace trace) {

        if ( field == null ) {
            throw new NullPointerException("null field");
        }

        if ( trace == null ) {
            throw new NullPointerException("null trace");
        }

        return new Trace(List.of(), Map.of(field, trace));
    }


    public static Trace trace(final Trace... traces) {

        if ( traces == null || Arrays.stream(traces).anyMatch(Objects::isNull) ) {
            throw new NullPointerException("null errors");
        }

        return trace(asList(traces));
    }

    public static Trace trace(final Collection<Trace> traces) {

        if ( traces == null || traces.stream().anyMatch(Objects::isNull) ) {
            throw new NullPointerException("null errors");
        }

        return new Trace(
                traces.stream().flatMap(t -> t.errors().stream()).distinct().collect(toList()),
                traces.stream().flatMap(t -> t.entries().entrySet().stream()).collect(groupingBy(
                        Map.Entry::getKey, mapping(Map.Entry::getValue, collectingAndThen(toList(), Trace::trace))
                ))
        );
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private final List<String> errors;
    private final Map<String, Trace> entries;


    private Trace(final List<String> errors, final Map<String, Trace> entries) {
        this.errors=unmodifiableList(errors);
        this.entries=unmodifiableMap(entries);
    }


    public boolean empty() {
        return errors.isEmpty() && entries.isEmpty();
    }


    public List<String> errors() {
        return errors;
    }

    public Map<String, Trace> entries() {
        return entries;
    }

}
