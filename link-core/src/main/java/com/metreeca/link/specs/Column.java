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

package com.metreeca.link.specs;

import static java.lang.String.format;

/**
 * Table column.
 */
public final class Column {

    public static Column column(final String expression, final Object model) {

        if ( expression == null ) {
            throw new NullPointerException("null expression");
        }

        if ( model == null ) {
            throw new NullPointerException("null model");
        }

        return new Column(Expression.expression(expression), model);
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
