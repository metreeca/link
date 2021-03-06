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

import com.metreeca.json.Query;
import com.metreeca.json.Shape;
import com.metreeca.json.queries.*;
import com.metreeca.json.shapes.All;
import com.metreeca.rest.*;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.LDP;

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.metreeca.json.Values.iri;
import static com.metreeca.json.queries.Items.items;
import static com.metreeca.json.shapes.And.and;
import static com.metreeca.json.shapes.Field.field;
import static com.metreeca.rdf4j.assets.Graph.graph;
import static com.metreeca.rest.Context.asset;
import static com.metreeca.rest.MessageException.status;
import static com.metreeca.rest.Response.*;
import static com.metreeca.rest.formats.JSONLDFormat.*;


final class GraphRelator extends GraphProcessor {

	private static final Pattern RepresentationPattern=Pattern
			.compile("\\s*return\\s*=\\s*representation\\s*;\\s*include\\s*=\\s*\"(?<representation>[^\"]*)\"\\s*");


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Graph graph=asset(graph());


	Future<Response> handle(final Request request) {
		return request.reply(response -> {

			final boolean resource=!request.collection();
			final boolean minimal=include(request, LDP.PREFER_MINIMAL_CONTAINER);
			final boolean filtered=!request.query().isEmpty();

			if ( resource || minimal ) {

				final IRI item=iri(request.item());
				final Shape shape=resource ? detail(request.attribute(shape())) : target(request.attribute(shape()));

				return filtered

						? response.map(status(NotImplemented, "resource filtered retrieval not supported"))

						: query(iri(request.item()), and(All.all(item), shape), request.query()).fold(

						response::map, query -> graph.exec(connection -> {

							final Collection<Statement> model=fetch(connection, item, query);

							// containers are currently virtual and respond always with 200 OK even if not described in
							// the graph

							final Message<Response> message=response

									// !!! 404 NotFound or 410 Gone if previously known for non-virtual containers

									.status(OK)

									.header("+Preference-Applied",
											minimal ? include(LDP.PREFER_MINIMAL_CONTAINER) : ""
									);

							// !!! 404 NotFound or 410 Gone if previously known for non-virtual containers
							// !!! add ldp:contains if items.path is not empty

							return resource && model.isEmpty() ? response.status(NotFound) :
									message.attribute(shape(), query.map(new Query.Probe<Shape>() {

										@Override public Shape probe(final Items items) {
											return items.shape(); // !!! add ldp:contains if items.path is not empty
										}

										@Override public Shape probe(final Stats stats) {
											return GraphEngine.StatsShape;
										}

										@Override public Shape probe(final Terms terms) {
											return GraphEngine.TermsShape;
										}

									}))

											.body(jsonld(), model);

						})

				);

			} else {

				final IRI item=iri(request.item());

				final Shape holder=target(request.attribute(shape()));
				final Shape digest=digest(request.attribute(shape()));

				// containers are currently virtual and respond always with 200 OK even if not described in the graph

				return query(iri(request.item()), digest, request.query())

						.fold(response::map, query -> graph.exec(connection -> {

							final Collection<Statement> matches=fetch(connection, item, query);

							if ( filtered ) { // matches only

								return response
										.status(OK)
										.attribute(shape(), query.map(new Query.Probe<Shape>() {

													@Override public Shape probe(final Items items) {
														return field(LDP.CONTAINS, items.shape());
													}

													@Override public Shape probe(final Stats stats) {

														return GraphEngine.StatsShape;
													}

													@Override public Shape probe(final Terms terms) {
														return GraphEngine.TermsShape;
													}

												}))
												.body(jsonld(), matches);

									} else { // include container description

										// !!! 404 NotFound or 410 Gone if previously known for non-virtual containers

										matches.addAll(fetch(connection, item, items(holder)));

										return response
												.status(OK)
												.attribute(shape(), and(holder, field(LDP.CONTAINS, digest)))
												.body(jsonld(), matches);

									}

								})

						);

			}

		});
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private String include(final IRI include) {
		return "return=representation; include=\""+include+"\"";
	}

	private boolean include(final Request request, final IRI include) {

		// !!! handle multiple uris in include parameter
		// !!! handle omit parameter (with multiple uris)
		// !!! handle other representation values in https://www.w3.org/TR/ldp/#prefer-parameters

		final String representation=request
				.headers("Prefer")
				.stream()
				.map(value -> {

					final Matcher matcher=RepresentationPattern.matcher(value);

					return matcher.matches() ? matcher.group("representation") : "";

				})
				.filter(value -> !value.isEmpty())
				.findFirst()
				.orElse("");

		return representation.equals(include.stringValue());

	}

}
