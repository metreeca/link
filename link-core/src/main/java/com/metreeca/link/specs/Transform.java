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

import com.metreeca.link.Shape;

import java.math.BigInteger;
import java.util.Optional;

import static com.metreeca.link.Shape.*;

/**
 * Value transform.
 */
public enum Transform {

    count(true) {
        @Override public Optional<Shape> apply(final Shape shape) {
            return Optional.of(shape(minCount(1), maxCount(1), clazz(BigInteger.class)));
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
            return shape.clazz().map(clazz -> shape(maxCount(1), clazz(clazz))); // !!! integer -> decimal
        }
    },


    abs(false) {
        @Override public Optional<Shape> apply(final Shape shape) {
            return Optional.of(shape);
        }
    },

    round(false) {
        @Override public Optional<Shape> apply(final Shape shape) {
            return Optional.of(shape);
        }
    },

    year(false) {
        @Override public Optional<Shape> apply(final Shape shape) {
            return Optional.of(clazz(BigInteger.class));
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


    public abstract Optional<Shape> apply(final Shape shape);

}
