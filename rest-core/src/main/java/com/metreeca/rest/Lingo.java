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

package com.metreeca.rest;


import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.String.format;
import static java.util.function.Predicate.not;

/**
 * Vocabulary utility.
 */
final class Lingo {

    private static final String DefaultSpace="app:#";

    private static final Pattern SchemaPattern=Pattern.compile("(?<schema>[a-zA-Z0-9][-+.a-zA-Z0-9]*)");
    private static final Pattern BasePattern=Pattern.compile("(?<base>"+SchemaPattern+":(?://[^/\\s]*/)?)");
    private static final Pattern AbsolutePattern=Pattern.compile(BasePattern+"(?<path>\\S*[/#](?<label>\\w+)|\\S+)");
    private static final Pattern CompactPattern=Pattern.compile("(?:(?<prefix>"+SchemaPattern+"):)?(?<name>\\w+)?");


    static Optional<Matcher> compact(final String iri) {
        return Optional.ofNullable(iri)

                .map(CompactPattern::matcher)
                .filter(Matcher::matches);
    }

    static Optional<Matcher> absolute(final String iri) {
        return Optional.ofNullable(iri)
                .map(AbsolutePattern::matcher)
                .filter(Matcher::matches);
    }


    static Optional<String> base(final String iri) {
        return absolute(iri).map(matcher -> Optional
                .ofNullable(matcher.group("base"))
                .orElseGet(() -> matcher.group("schema"))
        );
    }

    static Optional<String> path(final String iri) {
        return absolute(iri).flatMap(matcher -> Optional
                .ofNullable(matcher.group("path"))
                .map(path -> "/"+path)
        );
    }

    static Optional<String> name(final String iri) {
        return absolute(iri).flatMap(matcher -> Optional
                .ofNullable(matcher.group("label"))
        );
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private final Map<String, String> namespaces=new HashMap<>();


    void set(final String prefix, final String value) {


        if ( !prefix.isEmpty() && !SchemaPattern.matcher(prefix).matches() ) {
            throw new IllegalArgumentException(format("malformed namespace prefix <%s>", prefix));
        }

        if ( !AbsolutePattern.matcher(value).matches() ) {
            throw new IllegalArgumentException(format("malformed namespace value <%s>", value));
        }

        if ( namespaces.put(prefix, value) != null ) {
            throw new IllegalArgumentException(format(
                    "multiple definitions for namespace prefix <%s>", prefix
            ));
        }

    }


    Optional<String> expand(final String iri, final String label) {
        return compact(iri)

                .flatMap(matcher -> {

                    final String prefix=Optional.ofNullable(matcher.group("prefix")).orElse("");
                    final Optional<String> value=Optional.ofNullable(namespaces.get(prefix));

                    final String base=prefix.isEmpty()
                            ? value.orElse(DefaultSpace)
                            : value.orElseThrow(() -> new IllegalArgumentException(format(
                            "undefined namespace prefix <%s>", prefix
                    )));

                    return Optional.ofNullable(matcher.group("name"))
                            .or(() -> Optional.of(label).filter(not(String::isEmpty)))
                            .map(name -> base+name);


                })

                .or(() -> absolute(iri)
                        .map(Matcher::group)
                );
    }

}
