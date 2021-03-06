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
import com.metreeca.json.Values;

import org.eclipse.rdf4j.model.Value;

import java.util.*;

import static com.metreeca.json.Values.format;
import static com.metreeca.json.shapes.And.and;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableSet;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;


/**
 * Universal set values constraint.
 *
 * <p>States that the focus set includes all values from a given set of target values.</p>
 */
public final class All extends Shape {

	public static Shape all(final Object... values) {

		if ( values == null || Arrays.stream(values).anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null values");
		}

		return all(Arrays.stream(values).map(Values::value).collect(toList()));
	}

	public static Shape all(final Value... values) {

		if ( values == null || Arrays.stream(values).anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null values");
		}

		return all(asList(values));
	}

	public static Shape all(final Collection<? extends Value> values) {

		if ( values == null || values.stream().anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null values");
		}

		return values.isEmpty() ? and() : new All(values);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Set<Value> values;


	private All(final Collection<? extends Value> values) {
		this.values=new LinkedHashSet<>(values);
	}


	public Set<Value> values() {
		return unmodifiableSet(values);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public <T> T map(final Probe<T> probe) {

		if ( probe == null ) {
			throw new NullPointerException("null probe");
		}

		return probe.probe(this);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public boolean equals(final Object object) {
		return this == object || object instanceof All
				&& values.equals(((All)object).values);
	}

	@Override public int hashCode() {
		return values.hashCode();
	}

	@Override public String toString() {
		return "all("+(values.isEmpty() ? "" : values.stream()
				.map(v -> format(v).replace("\n", "\n\t"))
				.collect(joining(",\n\t", "\n\t", "\n"))
		)+")";
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

}
