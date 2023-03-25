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

import org.eclipse.rdf4j.model.vocabulary.XSD;

import static com.metreeca.link.Shape.*;

/**
 * Value transform.
 */
public enum Transform {

    COUNT(true) {
        @Override public Shape apply(final Shape shape) {
            return (shape(minCount(1), maxCount(1), clazz(XSD.INTEGER)));
        }
    },

    MIN(true) {
        @Override public Shape apply(final Shape shape) {
            return shape(maxCount(1), shape.clazz().map(Shape::clazz).orElseGet(Shape::shape));
        }
    },

    MAX(true) {
        @Override public Shape apply(final Shape shape) {
            return shape(maxCount(1), shape.clazz().map(Shape::clazz).orElseGet(Shape::shape));
        }
    },

    SUM(true) {
        @Override public Shape apply(final Shape shape) {
            return shape(maxCount(1), shape.clazz().map(Shape::clazz).orElseGet(Shape::shape));
        }
    },

    AVG(true) {
        @Override public Shape apply(final Shape shape) { // !!! integer -> decimal
            return shape(maxCount(1), shape.clazz().map(Shape::clazz).orElseGet(Shape::shape));
        }
    },


    ABS(false) {
        @Override public Shape apply(final Shape shape) {
            return shape;
        }
    },

    ROUND(false) { // !!! decimal -> integer?

        @Override public Shape apply(final Shape shape) {
            return shape;
        }
    },

    YEAR(false) {
        @Override public Shape apply(final Shape shape) {
            return clazz(XSD.INTEGER);
        }
    };


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
