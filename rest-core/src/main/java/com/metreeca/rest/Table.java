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

import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;
import static java.util.stream.Collectors.toUnmodifiableMap;

/**
 * Analytical query results.
 */
public final class Table<T> extends Stash<T> {

    public static <T> Table<T> table(final Map<String, Column> columns) {

        if ( columns == null || columns.values().stream().anyMatch(Objects::isNull) ) {
            throw new NullPointerException("null columns");
        }

        if ( columns.keySet().stream().anyMatch(Objects::isNull) ) {
            throw new NullPointerException("null column labels");
        }

        return new Table<>(columns, List.of());
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private final Map<String, Column> columns;
    private final Map<String, Expression> expressions;

    private final List<Map<String, Object>> records;


    private Table(final Map<String, Column> columns, final List<Map<String, Object>> records) {

        this.columns=unmodifiableMap(columns);
        this.expressions=columns.entrySet().stream()
                .collect(toUnmodifiableMap(Map.Entry::getKey, entry -> entry.getValue().expression()));

        this.records=new ArrayList<>(records);
    }


    public Table<T> copy() {
        return new Table<>(columns, records);
    }


    public Map<String, Column> columns() {
        return columns;
    }

    public Map<String, Expression> expressions() {
        return expressions;
    }

    public List<Map<String, Object>> records() {
        return unmodifiableList(records);
    }


    public Table<T> append(final Map<String, Object> record) {

        if ( record == null ) {
            throw new NullPointerException("null record");
        }

        if ( record.keySet().stream().anyMatch(Objects::isNull) ) {
            throw new NullPointerException("null column labels");
        }

        if ( !columns.keySet().equals(record.keySet()) ) {
            throw new IllegalArgumentException("mismatched columns");
        }

        records.add(record);

        return this;
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static final class Column {

        public static Column column(final Expression expression, final Object template) {

            if ( expression == null ) {
                throw new NullPointerException("null expression");
            }

            if ( template == null ) {
                throw new NullPointerException("null template");
            }

            return new Column(expression, template);
        }


        private final Expression expression;
        private final Object template;


        private Column(final Expression expression, final Object template) {
            this.expression=expression;
            this.template=template;
        }


        public Expression expression() {
            return expression;
        }

        public Object template() {
            return template;
        }


        @Override public boolean equals(final Object object) {
            return this == object || object instanceof Column
                    && expression.equals(((Column)object).expression)
                    && template.equals(((Column)object).template);
        }

        @Override public int hashCode() {
            return expression.hashCode()
                    ^template.hashCode();
        }

        @Override public String toString() {
            return String.format("%s <%s>", expression, template);
        }

    }

}
