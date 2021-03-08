/*
 * Copyright © 2013-2021 Metreeca srl
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

package com.metreeca.rdf4j.datastore;

import com.google.cloud.datastore.*;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.extensiblestore.*;
import org.eclipse.rdf4j.sail.extensiblestore.valuefactory.ExtensibleStatement;

import java.util.Iterator;
import java.util.Optional;

public final class DatastoreSail extends ExtensibleStore<DataStructureInterface, NamespaceStoreInterface> {

	public DatastoreSail() {
		this(DatastoreOptions.getDefaultInstance().getService());
	}

	public DatastoreSail(final Datastore datastore) {

		if ( datastore == null ) {
			throw new NullPointerException("null datastore");
		}

		dataStructure=new DataStructure(datastore);
		namespaceStore=new NamespaceStore(datastore);
	}


	@Override public boolean isWritable() throws SailException {
		return true;
	}

	@Override protected NotifyingSailConnection getConnectionInternal() throws SailException {
		return new ExtensibleStoreConnection<DatastoreSail>(this) {};
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static class DataStructure implements DataStructureInterface {

		DataStructure(final Datastore datastore) {

		}

		@Override public void init() {}

		@Override public CloseableIteration<? extends ExtensibleStatement, SailException> getStatements(
				final Resource subject, final IRI predicate, final Value object,
				final boolean inferred, final Resource... context
		) {
			throw new UnsupportedOperationException("to be implemented"); // !!! tbi
		}

		@Override public void addStatement(final ExtensibleStatement statement) {
			throw new UnsupportedOperationException("to be implemented"); // !!! tbi
		}

		@Override public void removeStatement(final ExtensibleStatement statement) {
			throw new UnsupportedOperationException("to be implemented"); // !!! tbi
		}

		@Override public void flushForReading() {}

		@Override public void flushForCommit() {}

	}

	private static class NamespaceStore implements NamespaceStoreInterface {

		private final Datastore datastore;


		NamespaceStore(final Datastore datastore) {
			this.datastore=datastore;
		}


		private Key key(final String prefix) {
			return datastore.newKeyFactory().setKind("name").newKey(prefix);
		}


		@Override public void init() {}

		@Override public Iterator<SimpleNamespace> iterator() {
			throw new UnsupportedOperationException("to be implemented"); // !!! tbi
		}

		@Override public String getNamespace(final String prefix) {

			if ( prefix == null ) {
				throw new NullPointerException("null prefix");
			}

			return Optional.ofNullable(datastore.get(key(prefix)))
					.map(entity -> entity.getString("namespace"))
					.orElse(null);
		}

		@Override public void setNamespace(final String prefix, final String namespace) {

			if ( prefix == null ) {
				throw new NullPointerException("null prefix");
			}

			if ( namespace == null ) {
				throw new NullPointerException("null namespace");
			}

			datastore.put(Entity
					.newBuilder(key(prefix))
					.set("namespace", namespace)
					.build()
			);

		}

		@Override public void removeNamespace(final String prefix) {
			throw new UnsupportedOperationException("to be implemented"); // !!! tbi
		}

		@Override public void clear() {
			throw new UnsupportedOperationException("to be implemented"); // !!! tbi
		}

	}

}
