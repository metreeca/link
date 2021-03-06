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

package com.metreeca.rdf4j.assets;


import com.metreeca.rest.Request;
import com.metreeca.rest.Response;
import com.metreeca.rest.formats.JSONLDFormat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.metreeca.json.ModelAssert.assertThat;
import static com.metreeca.json.Shape.required;
import static com.metreeca.json.ValuesTest.*;
import static com.metreeca.json.shapes.Field.field;
import static com.metreeca.json.shapes.Guard.convey;
import static com.metreeca.rdf4j.assets.GraphTest.exec;
import static com.metreeca.rdf4j.assets.GraphTest.model;
import static com.metreeca.rest.Response.NotFound;
import static com.metreeca.rest.ResponseAssert.assertThat;
import static com.metreeca.rest.formats.JSONLDFormat.jsonld;


final class GraphUpdaterTest {

	@Nested final class Holder {

		@Test void testNotImplemented() {
			exec(() -> new GraphUpdater()

					.handle(new Request()
							.path("/employees/")
							.body(jsonld(), decode("</employees/> rdfs:label 'Updated!'."))
					)

					.accept(response -> assertThat(response)
							.hasStatus(Response.InternalServerError)
					)
			);
		}

	}

	@Nested final class Member {

		@Test void testUpdate() {
			exec(model(small()), () -> new GraphUpdater()

					.handle(new Request()
							.base(Base)
							.path("/employees/1370").attribute(JSONLDFormat.shape(), convey().then(
									field(term("forename"), required()),
									field(term("surname"), required()),
									field(term("email"), required()),
									field(term("title"), required()),
									field(term("seniority"), required())
							))
							.body(jsonld(), decode("</employees/1370>"
									+":forename 'Tino';"
									+":surname 'Faussone';"
									+":email 'tfaussone@example.com';"
									+":title 'Sales Rep' ;"
									+":seniority 5 ." // outside salesman envelope
							))
					)

					.accept(response -> {

						assertThat(response)
								.hasStatus(Response.NoContent)
								.doesNotHaveBody();

						assertThat(model())

								.as("updated values inserted")
								.hasSubset(decode("</employees/1370>"
										+":forename 'Tino';"
										+":surname 'Faussone';"
										+":email 'tfaussone@example.com';"
										+":title 'Sales Rep' ;"
										+":seniority 5 ."
								))

								.as("previous values removed")
								.doesNotHaveSubset(decode("</employees/1370>"
										+":forename 'Gerard';"
										+":surname 'Hernandez'."
								));

					}));
		}

		@Test void testRejectMissing() {
			exec(() -> new GraphUpdater()

					.handle(new Request()
							.base(Base)
							.path("/employees/9999")
							.body(jsonld(), decode(""))
					)

					.accept(response -> assertThat(response)
							.hasStatus(NotFound)
							.doesNotHaveBody()
					)
			);

		}

	}

}
