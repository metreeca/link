/*
 * Copyright © 2023-2024 Metreeca srl
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

import org.eclipse.rdf4j.model.IRI;

import java.util.*;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableMap;
import static java.util.Collections.unmodifiableSet;
import static java.util.stream.Collectors.*;

/**
 * Shape validation trace.
 */
public final class Trace {

    private static final Trace EMPTY=new Trace(Set.of(), Map.of());


    public static Trace trace(final String error) {

        if ( error == null ) {
            throw new NullPointerException("null error");
        }

        return error.isBlank() ? EMPTY : new Trace(Set.of(error.trim()), Map.of());
    }

    public static Trace trace(final IRI predicate, final Trace trace) {

        if ( predicate == null ) {
            throw new NullPointerException("null predicate");
        }

        if ( trace == null ) {
            throw new NullPointerException("null trace");
        }

        return trace.empty() ? EMPTY : new Trace(Set.of(), Map.of(predicate, trace));
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

        return traces.isEmpty() ? EMPTY : traces.size() == 1 ? traces.iterator().next() : new Trace(
                traces.stream().flatMap(t -> t.errors().stream()).distinct().collect(toSet()),
                traces.stream().flatMap(t -> t.entries().entrySet().stream()).collect(groupingBy(
                        Map.Entry::getKey, mapping(Map.Entry::getValue, collectingAndThen(toList(), Trace::trace))
                ))
        );
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private final Set<String> errors;
    private final Map<IRI, Trace> entries;


    private Trace(final Set<String> errors, final Map<IRI, Trace> entries) {
        this.errors=unmodifiableSet(errors);
        this.entries=unmodifiableMap(entries);
    }


    public boolean empty() {
        return errors.isEmpty() && entries.isEmpty();
    }


    public Set<String> errors() {
        return errors;
    }

    public Map<IRI, Trace> entries() {
        return entries;
    }


    @Override public String toString() {
        return Stream.concat(

                errors.stream(),

                entries.entrySet().stream().map(e -> format("<%s> : %s",
                        e.getKey(),
                        e.getValue().toString().replace("\n", "\n\t")
                ))

        ).collect(joining("\n\t", "{\n\t", "\n}"));
    }

}
