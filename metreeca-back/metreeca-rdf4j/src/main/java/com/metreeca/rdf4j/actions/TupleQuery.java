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

package com.metreeca.rdf4j.actions;

import com.metreeca.rdf4j.assets.Graph;
import com.metreeca.rest.Context;
import com.metreeca.rest.assets.Logger;

import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Operation;

import java.util.function.Function;
import java.util.stream.Stream;

import static com.metreeca.rest.assets.Logger.time;
import static org.eclipse.rdf4j.common.iteration.Iterations.asList;
import static org.eclipse.rdf4j.query.QueryLanguage.SPARQL;

/**
 * SPARQL tuple query action.
 *
 * <p>Executes SPARQL tuple queries against the {@linkplain #graph(Graph) target graph}.</p>
 */
public final class TupleQuery extends Action<TupleQuery> implements Function<String, Stream<BindingSet>> {

	private final Logger logger=Context.asset(Logger.logger());

	
	/**
	 * Executes a SPARQL tuple query.
	 *
	 * @param query the graph query to be executed
	 *
	 * @return a stream of binding sets produced by executing {@code query} against the {@linkplain #graph(Graph)
	 * target graph} after {@linkplain #configure(Operation) configuring} it; null or empty queries are silently ignored
	 */
	@Override public Stream<BindingSet> apply(final String query) {
		return query == null || query.isEmpty() ? Stream.empty() : graph().exec(connection -> {
			return time(() -> // bindings must be retrieved inside txn

					asList(configure(connection.prepareTupleQuery(SPARQL, query, base())).evaluate()).parallelStream()

			).apply((t, v) ->

					logger.info(this, String.format("executed in <%,d> ms", t))

			);
		});
	}

}
