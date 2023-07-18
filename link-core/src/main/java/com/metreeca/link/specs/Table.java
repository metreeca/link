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

import java.util.AbstractMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static java.util.Collections.unmodifiableMap;

/**
 * Collection analytical model.
 */
public final class Table extends AbstractMap<String, Column> {

    public static Table table(final Map<String, Column> columns) {

        if ( columns == null ) {
            throw new NullPointerException("null columns");
        }

        if ( columns.keySet().stream().anyMatch(Objects::isNull) ) {
            throw new NullPointerException("null column labels");
        }

        if ( columns.values().stream().anyMatch(Objects::isNull) ) {
            throw new NullPointerException("null column values");
        }

        return new Table(columns);
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private final Map<String, Column> columns;


    private Table(final Map<String, Column> columns) {
        this.columns=unmodifiableMap(columns);
    }


    @Override public Set<Entry<String, Column>> entrySet() {
        return columns.entrySet();
    }

}
