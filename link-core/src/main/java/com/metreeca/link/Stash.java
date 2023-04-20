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
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.metreeca.link.Shape.*;

import static java.lang.String.format;
import static java.util.Collections.unmodifiableList;
import static java.util.Map.entry;
import static java.util.regex.Pattern.compile;
import static java.util.stream.Collectors.joining;

/**
 * Stashed collection metadata.
 *
 * @param <T> the type of elements in the host collection field.
 */
public abstract class Stash<T> extends AbstractList<T> implements Set<T> {

    /**
     * Value transform.
     */
    public static enum Transform {

        count(true) {
            @Override public Optional<Shape> apply(final Shape shape) {
                return Optional.of(shape(minCount(1), maxCount(1), clazz(Integer.class)));
            }
        },

        sum(true) {
            @Override public Optional<Shape> apply(final Shape shape) {
                return shape.clazz().map(clazz -> shape(maxCount(1), clazz(clazz)));
            }
        },

        min(true) {
            @Override public Optional<Shape> apply(final Shape shape) {
                return shape.clazz().map(clazz -> shape(maxCount(1), clazz(clazz)));
            }
        },

        max(true) {
            @Override public Optional<Shape> apply(final Shape shape) {
                return shape.clazz().map(clazz -> shape(maxCount(1), clazz(clazz)));
            }
        },

        avg(true) {
            @Override public Optional<Shape> apply(final Shape shape) {
                return shape.clazz().map(clazz -> shape(maxCount(1), clazz(clazz)));
            }
        },

        sample(true) {
            @Override public Optional<Shape> apply(final Shape shape) {
                return shape.clazz().map(clazz -> shape(maxCount(1), clazz(clazz)));
            }
        },


        abs(false) {
            @Override public Optional<Shape> apply(final Shape shape) {
                return Optional.of(shape);
            }
        };


        private final boolean aggregate;


        private Transform(final boolean aggregate) {
            this.aggregate=aggregate;
        }


        public boolean aggregate() {
            return aggregate;
        }

        public abstract Optional<Shape> apply(final Shape shape);

    }


    @Override public int size() {
        return 0;
    }

    @Override public T get(final int index) {
        throw new IndexOutOfBoundsException();
    }

    @Override public Spliterator<T> spliterator() {
        return super.spliterator();
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static final class Expression {

        private static final Pattern TransformPattern=compile("(?<name>\\w+):");

        private static final Pattern IdPattern=compile("\\w+");
        private static final Pattern EscapedPattern=compile("\\\\(?<char>.)");
        private static final Pattern ReservedPattern=compile("['\\\\]");
        private static final Pattern LabelPattern=compile("(?:[^"+ReservedPattern+"]|"+EscapedPattern+")*");

        private static final Pattern FieldPattern=compile("(?<id>"+IdPattern+")|\\['(?<label>"+LabelPattern+")']");
        private static final Pattern AliasPattern=compile("(?:"+FieldPattern+")=");


        public static Optional<Entry<String, String>> alias(final String alias) {

            if ( alias == null ) {
                throw new NullPointerException("null alias");
            }

            final Matcher matcher=AliasPattern.matcher(alias);

            return matcher.lookingAt() ?
                    Optional.of(entry(field(matcher), alias.substring(matcher.end()))) :
                    Optional.empty();
        }


        public static Expression expression(final String expression) {

            if ( expression == null ) {
                throw new NullPointerException("null expression");
            }

            final List<String> path=new ArrayList<>();
            final List<Transform> transforms=new ArrayList<>();

            int next=0;

            final int length=expression.length();

            for (
                    final Matcher matcher=TransformPattern.matcher(expression).region(next, length);
                    matcher.lookingAt();
                    matcher.region(next, length)
            ) {

                final String name=matcher.group("name");

                try {

                    transforms.add(Transform.valueOf(name));

                } catch ( final IllegalArgumentException ignored ) {
                    throw new IllegalArgumentException(format("unknown transform <%s>", name));
                }

                next=matcher.end();
            }

            for (
                    final Matcher matcher=FieldPattern.matcher(expression).region(next, length);
                    matcher.lookingAt();
                    matcher.region(next, length)
            ) {

                path.add(field(matcher));

                final int end=matcher.end();

                next=(end < length && expression.charAt(end) == '.') ? end+1 : end;

            }

            if ( next < length ) {
                throw new IllegalArgumentException(format("malformed expression <%s>", expression));
            }

            return expression(path, transforms);
        }

        public static Expression expression(final List<String> path, final List<Transform> transforms) {

            if ( path == null || path.stream().anyMatch(Objects::isNull) ) {
                throw new NullPointerException("null path");
            }

            if ( transforms == null || transforms.stream().anyMatch(Objects::isNull) ) {
                throw new NullPointerException("null transforms");
            }

            return new Expression(path, transforms);
        }


        private static String field(final Matcher matcher) {

            final String id=matcher.group("id");
            final String label=matcher.group("label");

            return id != null ? id : EscapedPattern.matcher(label).replaceAll("${char}");
        }


        private final List<String> path;
        private final List<Transform> transforms;


        private Expression(final List<String> path, final List<Transform> transforms) {
            this.path=unmodifiableList(path);
            this.transforms=unmodifiableList(transforms);
        }


        public boolean computed() {
            return !transforms.isEmpty();
        }

        public boolean aggregate() {
            return transforms.stream().anyMatch(Transform::aggregate);
        }


        public List<String> path() {
            return path;
        }

        public List<Transform> transforms() {
            return transforms;
        }


        @Override public boolean equals(final Object object) {
            return this == object || object instanceof Stash.Expression
                    && path.equals(((Expression)object).path)
                    && transforms.equals(((Expression)object).transforms);
        }

        @Override public int hashCode() {
            return path.hashCode()
                    ^transforms.hashCode();
        }

        @Override public String toString() {
            return Stream

                    .concat(

                            transforms.stream()
                                    .map(transform -> format("%s:", transform)),

                            path.stream()
                                    .map(step -> ReservedPattern.matcher(step).replaceAll("\\\\$0"))
                                    .map(step -> format(IdPattern.matcher(step).matches() ? ".%s" : "['%s']", step))

                    )

                    .collect(joining());
        }

    }

}
