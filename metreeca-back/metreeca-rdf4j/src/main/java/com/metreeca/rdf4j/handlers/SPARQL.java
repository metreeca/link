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

package com.metreeca.rdf4j.handlers;

import com.metreeca.rdf4j.assets.Graph;
import com.metreeca.rest.Response;
import com.metreeca.rest.handlers.Router;

import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.*;
import org.eclipse.rdf4j.query.impl.SimpleDataset;
import org.eclipse.rdf4j.query.resultio.*;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.*;

import java.util.*;

import static com.metreeca.rest.Message.types;
import static com.metreeca.rest.MessageException.status;
import static com.metreeca.rest.Response.*;
import static com.metreeca.rest.formats.OutputFormat.output;


/**
 * SPARQL 1.1 Query/Update endpoint handler.
 *
 * <p>Provides a standard SPARQL 1.1 Query/Update endpoint exposing the contents of the shared {@linkplain Graph
 * graph}.</p>
 *
 * <p>Both {@linkplain #query(Collection) query} and {@linkplain #update(Collection) update} operations are disabled,
 * unless otherwise specified.</p>
 *
 * @see <a href="http://www.w3.org/TR/sparql11-protocol/">SPARQL 1.1 Protocol</a>
 */
public final class SPARQL extends Endpoint<SPARQL> {

	/**
	 * Creates a SPARQL endpoint
	 *
	 * @return a new SPARQL endpoint
	 */
	public static SPARQL sparql() {
		return new SPARQL();
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private SPARQL() {
		delegate(Router.router()
				.get(this::process)
				.post(this::process)
		);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private com.metreeca.rest.Future<com.metreeca.rest.Response> process(final com.metreeca.rest.Request request) {
		return consumer -> graph().exec(connection -> {
			try {

				final Operation operation=operation(request, connection);

				if ( operation == null ) { // !!! return void description for GET

					request.reply(status(BadRequest, "missing query/update parameter")).accept(consumer);

				} else if ( operation instanceof Query && !queryable(request.roles())
						|| operation instanceof Update && !updatable(request.roles())
				) {

					request.reply(response -> response.status(com.metreeca.rest.Response.Unauthorized)).accept(consumer);

				} else if ( operation instanceof BooleanQuery ) {

					process(request, (BooleanQuery)operation).accept(consumer);

				} else if ( operation instanceof TupleQuery ) {

					process(request, (TupleQuery)operation).accept(consumer);

				} else if ( operation instanceof GraphQuery ) {

					process(request, (GraphQuery)operation).accept(consumer);

				} else if ( operation instanceof Update ) {

					process(request, (Update)operation).accept(consumer);

				} else {

					request.reply(status(NotImplemented, operation.getClass().getName())).accept(consumer);

				}

			} catch ( final MalformedQueryException|IllegalArgumentException e ) {

				request.reply(status(BadRequest, e)).accept(consumer);

			} catch ( final UnsupportedOperationException e ) {

				request.reply(status(NotImplemented, e)).accept(consumer);

			} catch ( final RuntimeException e ) {

				// !!! fails for QueryInterruptedException (timeout) ≫ response is already committed

				request.reply(status(InternalServerError, e)).accept(consumer);

			}
		});
	}

	private Operation operation(final com.metreeca.rest.Request request, final RepositoryConnection connection) {

		final Optional<String> query=request.parameter("query");
		final Optional<String> update=request.parameter("update");
		final Optional<String> infer=request.parameter("infer");

		final Collection<String> basics=request.parameters("default-graph-uri");
		final Collection<String> nameds=request.parameters("named-graph-uri");

		final Operation operation=query.isPresent() ? connection.prepareQuery(query.get())
				: update.map(connection::prepareUpdate).orElse(null);

		if ( operation != null ) {

			final ValueFactory factory=connection.getValueFactory();
			final SimpleDataset dataset=new SimpleDataset();

			basics.stream().distinct().forEachOrdered(basic -> dataset.addDefaultGraph(factory.createIRI(basic)));
			nameds.stream().distinct().forEachOrdered(named -> dataset.addNamedGraph(factory.createIRI(named)));

			operation.setDataset(dataset);
			operation.setMaxExecutionTime(timeout());
			operation.setIncludeInferred(infer.map(Boolean::parseBoolean).orElse(true));

		}

		return operation;

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private com.metreeca.rest.Future<com.metreeca.rest.Response> process(final com.metreeca.rest.Request request, final BooleanQuery query) {

		final boolean result=query.evaluate();

		final String accept=request.header("Accept").orElse("");

		final BooleanQueryResultWriterFactory factory=com.metreeca.rdf.formats.RDFFormat.service(
				BooleanQueryResultWriterRegistry.getInstance(), BooleanQueryResultFormat.SPARQL, types(accept));

		return request.reply(response -> response.status(com.metreeca.rest.Response.OK)
				.header("Content-Type", factory.getBooleanQueryResultFormat().getDefaultMIMEType())
				.body(output(), output -> factory.getWriter(output).handleBoolean(result))
		);
	}

	private com.metreeca.rest.Future<com.metreeca.rest.Response> process(final com.metreeca.rest.Request request, final TupleQuery query) {

		final TupleQueryResult result=query.evaluate();

		final String accept=request.header("Accept").orElse("");

		final TupleQueryResultWriterFactory factory=com.metreeca.rdf.formats.RDFFormat.service(
				TupleQueryResultWriterRegistry.getInstance(), TupleQueryResultFormat.SPARQL, types(accept));

		return request.reply(response -> response.status(com.metreeca.rest.Response.OK)
				.header("Content-Type", factory.getTupleQueryResultFormat().getDefaultMIMEType())
				.body(output(), output -> {
					try {

						final TupleQueryResultWriter writer=factory.getWriter(output);

						writer.startDocument();
						writer.startQueryResult(result.getBindingNames());

						while ( result.hasNext() ) { writer.handleSolution(result.next());}

						writer.endQueryResult();

					} finally {
						result.close();
					}
				}));
	}

	private com.metreeca.rest.Future<com.metreeca.rest.Response> process(final com.metreeca.rest.Request request, final GraphQuery query) {

		final GraphQueryResult result=query.evaluate();

		final String accept=request.header("Accept").orElse("");

		final RDFWriterFactory factory=com.metreeca.rdf.formats.RDFFormat.service(
				RDFWriterRegistry.getInstance(), RDFFormat.NTRIPLES, types(accept));

		return request.reply(response -> response.status(com.metreeca.rest.Response.OK)
				.header("Content-Type", factory.getRDFFormat().getDefaultMIMEType())
				.body(output(), output -> {

					final RDFWriter writer=factory.getWriter(output);

					writer.startRDF();

					for (final Map.Entry<String, String> entry : result.getNamespaces().entrySet()) {
						writer.handleNamespace(entry.getKey(), entry.getValue());
					}

					try {
						while ( result.hasNext() ) { writer.handleStatement(result.next());}
					} finally {
						result.close();
					}

					writer.endRDF();

				}));
	}

	private com.metreeca.rest.Future<com.metreeca.rest.Response> process(final com.metreeca.rest.Request request, final Update update) {

		update.execute();

		final String accept=request.header("Accept").orElse("");

		final BooleanQueryResultWriterFactory factory=com.metreeca.rdf.formats.RDFFormat.service(
				BooleanQueryResultWriterRegistry.getInstance(), BooleanQueryResultFormat.SPARQL, types(accept));

		return request.reply(response -> response.status(Response.OK)
				.header("Content-Type", factory.getBooleanQueryResultFormat().getDefaultMIMEType())
				.body(output(), output -> factory.getWriter(output).handleBoolean(true))
		);
	}

}
