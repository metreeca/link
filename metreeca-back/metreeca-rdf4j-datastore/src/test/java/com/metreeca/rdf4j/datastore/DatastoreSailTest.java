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

import com.google.cloud.datastore.DatastoreOptions;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.UUID;


final class DatastoreSailTest {

	@Test void test() throws IOException {

		final String host="localhost:8910";
		final String project="test-"+UUID.randomUUID();

		// !!! share process
		// !!! test availability

		final Process process=Runtime.getRuntime().exec(
				"gcloud beta emulators datastore start"
						+" --no-store-on-disk"
						+" --host-port "+host
						+" --project "+project
		);

		try {

			final Repository repository=new SailRepository(new DatastoreSail(DatastoreOptions.newBuilder()
					.setProjectId(project)
					.setHost(host)
					.build()
					.getService()
			));

			repository.init();

			try ( final RepositoryConnection connection=repository.getConnection() ) {

				connection.setNamespace("test", "test:namespace");

				System.out.println(connection.getNamespace("test"));
			}

			repository.shutDown();

		} finally {

			process.destroy();

		}

	}

}
