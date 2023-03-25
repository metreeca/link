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

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.base.AbstractIRI;

import static com.metreeca.link.Frame.iri;


/**
 * Value probe.
 */
public final class Probe extends AbstractIRI {

    private static final long serialVersionUID=6977348557453593592L;


    public static Probe probe(final String label, final Expression expression) {

        if ( label == null ) {
            throw new NullPointerException("null label");
        }

        if ( expression == null ) {
            throw new NullPointerException("null expression");
        }

        return new Probe(label, expression);
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private final IRI property=iri();

    private final String label;
    private final Expression expression;


    private Probe(final String label, final Expression expression) {
        this.label=label;
        this.expression=expression;
    }


    public String label() {
        return label;
    }

    public Expression expression() {
        return expression;
    }


    @Override public String stringValue() {
        return property.stringValue();
    }

    @Override public String getNamespace() {
        return property.getNamespace();
    }

    @Override public String getLocalName() {
        return property.getLocalName();
    }

}
