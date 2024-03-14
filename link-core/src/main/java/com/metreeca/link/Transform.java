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

import static com.metreeca.link.Shape.*;

/**
 * Value transform.
 */
public enum Transform {

    COUNT(true) {
        @Override public Shape apply(final Shape shape) {
            return (shape(required(), integer()));
        }
    },

    MIN(true) {
        @Override public Shape apply(final Shape shape) {
            return shape(optional(), datatype(shape));
        }
    },

    MAX(true) {
        @Override public Shape apply(final Shape shape) {
            return shape(optional(), datatype(shape));
        }
    },

    SUM(true) {
        @Override public Shape apply(final Shape shape) {
            return shape(optional(), datatype(shape));
        }
    },

    AVG(true) {
        @Override public Shape apply(final Shape shape) {
            return shape(required(), decimal());
        }
    },


    ABS(false) {
        @Override public Shape apply(final Shape shape) {
            return shape(optional(), datatype(shape));
        }
    },

    ROUND(false) {
        @Override public Shape apply(final Shape shape) {
            return shape(optional(), integer());
        }
    },

    YEAR(false) {
        @Override public Shape apply(final Shape shape) {
            return shape(optional(), integer());
        }
    };


    private static Shape datatype(final Shape shape) {
        return shape.datatype().map(Shape::datatype).orElseGet(Shape::shape);
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private final boolean aggregate;


    private Transform(final boolean aggregate) {
        this.aggregate=aggregate;
    }


    public boolean aggregate() {
        return aggregate;
    }


    public abstract Shape apply(final Shape shape);

}
