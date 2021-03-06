/*
 * Copyright © 2013-2020 Metreeca srl
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

package com.metreeca.json.shapes;

import com.metreeca.json.Shape;

import static com.metreeca.json.shapes.And.and;
import static com.metreeca.json.shapes.Or.or;


/**
 * Conditional logical constraint.
 *
 * <p>States that the focus set is consistent either with a {@linkplain #pass() positive} shape, if consistent also
 * with a {@linkplain #test() test} shape, or with a {@linkplain #fail() negative} shape, otherwise.</p>
 *
 *
 * <p><strong>Warning</strong> / Test shapes are currently limited to non-filtering constraints, that is to parametric
 * {@linkplain Guard guards}, logical operators and annotations: full conditional shape matching will be evaluated for
 * future releases.</p>
 */
public final class When extends Shape {

	public static Shape when(final Shape test, final Shape pass) {

		if ( test == null ) {
			throw new NullPointerException("null test shape");
		}

		if ( pass == null ) {
			throw new NullPointerException("null pass shape");
		}

		return when(test, pass, and());
	}

	public static Shape when(final Shape test, final Shape pass, final Shape fail) {

		if ( test == null ) {
			throw new NullPointerException("null test shape");
		}

		if ( pass == null ) {
			throw new NullPointerException("null pass shape");
		}

		if ( fail == null ) {
			throw new NullPointerException("null fail shape");
		}

		return test.equals(and()) ? pass
				: test.equals(or()) ? fail
				: pass.equals(fail) ? pass
				: new When(test, pass, fail);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Shape test;
	private final Shape pass;
	private final Shape fail;


	private When(final Shape test, final Shape pass, final Shape fail) {
		this.test=test;
		this.pass=pass;
		this.fail=fail;
	}


	public Shape test() {
		return test;
	}

	public Shape pass() {
		return pass;
	}

	public Shape fail() {
		return fail;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public <V> V map(final Probe<V> probe) {

		if ( probe == null ) {
			throw new NullPointerException("null probe");
		}

		return probe.probe(this);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public boolean equals(final Object object) {
		return this == object || object instanceof When
				&& test.equals(((When)object).test)
				&& pass.equals(((When)object).pass)
				&& fail.equals(((When)object).fail);
	}

	@Override public int hashCode() {
		return test.hashCode()^pass.hashCode()^fail.hashCode();
	}

	@Override public String toString() {
		return "when(\n\t"
				+test.toString().replace("\n", "\n\t")+",\n\t"
				+pass.toString().replace("\n", "\n\t")
				+(fail.equals(and()) ? "" : ",\n\t"+fail.toString().replace("\n", "\n\t"))
				+"\n)";
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static final class FilteringProbe extends Probe<Boolean> {

		@Override public Boolean probe(final Shape shape) { return true; }


		@Override public Boolean probe(final Meta meta) { return false; }

		@Override public Boolean probe(final Guard guard) { return false; }


		@Override public Boolean probe(final Field field) {
			return field.shape().map(this);
		}

		@Override public Boolean probe(final And and) {
			return and.shapes().stream().anyMatch(shape -> shape.map(this));
		}

		@Override public Boolean probe(final Or or) {
			return or.shapes().stream().anyMatch(shape -> shape.map(this));
		}

		@Override public Boolean probe(final When when) {
			return when.test().map(this)
					|| when.pass().map(this)
					|| when.fail().map(this);
		}

	}

}
