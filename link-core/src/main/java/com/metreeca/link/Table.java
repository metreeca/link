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

import static java.lang.String.format;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;
import static java.util.stream.Collectors.toList;

/**
 * Analytical query results.
 */
public final class Table<T> extends Stash<T> {

    @SafeVarargs public static <T> Table<T> table(final Entry<String, Column>... columns) {

        if ( columns == null || Arrays.stream(columns).anyMatch(Objects::isNull) ) {
            throw new NullPointerException("null columns");
        }

        if ( Arrays.stream(columns).map(Entry::getKey).anyMatch(Objects::isNull) ) {
            throw new NullPointerException("null column alias");
        }

        if ( Arrays.stream(columns).map(Entry::getValue).anyMatch(Objects::isNull) ) {
            throw new NullPointerException("null column");
        }

        final Map<String, Column> map=new LinkedHashMap<>();

        for (final Entry<String, Column> column : columns) {
            map.put(column.getKey(), column.getValue());
        }

        return new Table<>(map, List.of());
    }

    public static <T> Table<T> table(final Map<String, Column> columns) {

        if ( columns == null || columns.values().stream().anyMatch(Objects::isNull) ) {
            throw new NullPointerException("null columns");
        }

        if ( columns.keySet().stream().anyMatch(Objects::isNull) ) {
            throw new NullPointerException("null column labels");
        }

        return new Table<>(columns, List.of());
    }

    public static <T> Table<T> table(final Map<String, Column> columns, final List<Map<String, Object>> records) {

        if ( columns == null || columns.values().stream().anyMatch(Objects::isNull) ) {
            throw new NullPointerException("null columns");
        }

        if ( columns.keySet().stream().anyMatch(Objects::isNull) ) {
            throw new NullPointerException("null column labels");
        }

        if ( records == null || records.stream().anyMatch(Objects::isNull) ) {
            throw new NullPointerException("null records");
        }

        for (final Map<String, Object> record : records) {

            if ( record.keySet().stream().anyMatch(Objects::isNull) ) {
                throw new NullPointerException("null record labels");
            }

            if ( !columns.keySet().equals(record.keySet()) ) {
                throw new IllegalArgumentException("mismatched record labels");
            }

        }

        return new Table<>(columns, records.stream()
                .map(record -> unmodifiableMap(new LinkedHashMap<>(record)))
                .collect(toList())
        );
    }


    public static Column column(final String expression, final Object model) {

        if ( expression == null ) {
            throw new NullPointerException("null expression");
        }

        if ( model == null ) {
            throw new NullPointerException("null model");
        }

        return new Column(expression(expression), model);
    }

    public static Column column(final Expression expression, final Object model) {

        if ( expression == null ) {
            throw new NullPointerException("null expression");
        }

        if ( model == null ) {
            throw new NullPointerException("null model");
        }

        return new Column(expression, model);
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private final Map<String, Column> columns;
    private final List<Map<String, Object>> records;


    private Table(final Map<String, Column> columns, final List<Map<String, Object>> records) {
        this.columns=unmodifiableMap(columns);
        this.records=unmodifiableList(records);
    }


    public Map<String, Column> columns() {
        return columns;
    }

    public List<Map<String, Object>> records() {
        return records;
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static final class Column {

        private final Expression expression;
        private final Object model;


        private Column(final Expression expression, final Object model) {
            this.expression=expression;
            this.model=model;
        }


        public Expression expression() {
            return expression;
        }

        public Object model() {
            return model;
        }


        @Override public boolean equals(final Object object) {
            return this == object || object instanceof Column
                    && expression.equals(((Column)object).expression)
                    && model.equals(((Column)object).model);
        }

        @Override public int hashCode() {
            return expression.hashCode()
                    ^model.hashCode();
        }

        @Override public String toString() {
            return format("%s <%s>", expression, model);
        }

    }

}
