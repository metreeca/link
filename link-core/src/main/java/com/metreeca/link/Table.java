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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;

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

    private final List<Map<String, Object>> records;


    private Table(final Map<String, Column> columns, final List<Map<String, Object>> records) {
        this.columns=unmodifiableMap(columns);
        this.records=new ArrayList<>(records);
    }


    public Table<T> copy() { // !!! remove mutator
        return new Table<>(columns, records);
    }


    public Map<String, Column> columns() {
        return columns;
    }

    public List<Map<String, Object>> records() {
        return unmodifiableList(records);
    }


    public Table<T> append(final Map<String, Object> record) { // !!! remove mutator

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

        public static Column column(final Expression expression, final Object model) {

            if ( expression == null ) {
                throw new NullPointerException("null expression");
            }

            if ( model == null ) {
                throw new NullPointerException("null model");
            }

            return new Column(expression, model);
        }


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
            return String.format("%s <%s>", expression, model);
        }

    }

}
