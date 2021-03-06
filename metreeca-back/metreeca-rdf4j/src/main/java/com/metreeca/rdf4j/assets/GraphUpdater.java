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


import com.metreeca.json.Shape;
import com.metreeca.rest.*;

import org.eclipse.rdf4j.model.IRI;

import java.util.Optional;

import static com.metreeca.json.Values.iri;
import static com.metreeca.json.queries.Items.items;
import static com.metreeca.rdf4j.assets.Graph.graph;
import static com.metreeca.rest.MessageException.status;
import static com.metreeca.rest.Response.InternalServerError;
import static com.metreeca.rest.formats.JSONLDFormat.jsonld;
import static com.metreeca.rest.formats.JSONLDFormat.shape;


final class GraphUpdater extends GraphProcessor {

	private final Graph graph=Context.asset(graph());


	Future<Response> handle(final Request request) {
		return request.collection() ? holder(request) : member(request);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Future<Response> holder(final Request request) {
		return request.reply(status(InternalServerError, new UnsupportedOperationException("holder PUT method")));
	}

	private Future<Response> member(final Request request) {
		return request.body(jsonld()).fold(

				request::reply, model -> request.reply(response -> graph.exec(connection -> {

					final IRI item=iri(request.item());
					final Shape shape=request.attribute(shape());

					return Optional

							.of(fetch(connection, item, items(shape)))

							.filter(current -> !current.isEmpty())

							.map(current -> {

								connection.remove(current);
								connection.add(model);

								return response.status(Response.NoContent);

							})

							.orElseGet(() ->

									response.status(Response.NotFound) // !!! 410 Gone if previously known

							);

				}))

		);
	}

}

