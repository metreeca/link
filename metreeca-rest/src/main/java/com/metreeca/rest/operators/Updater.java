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

package com.metreeca.rest.operators;

import com.metreeca.json.Shape;
import com.metreeca.json.shapes.Guard;
import com.metreeca.rest.Handler;
import com.metreeca.rest.Request;
import com.metreeca.rest.assets.Engine;
import com.metreeca.rest.formats.JSONLDFormat;
import com.metreeca.rest.handlers.Delegator;

import org.eclipse.rdf4j.model.IRI;

import static com.metreeca.json.shapes.Guard.*;
import static com.metreeca.rest.Context.asset;
import static com.metreeca.rest.Wrapper.wrapper;
import static com.metreeca.rest.assets.Engine.*;


/**
 * Model-driven resource updater.
 *
 * <p>Performs:</p>
 *
 * <ul>
 *
 * <li>{@linkplain Guard#Role role}-based request shape redaction and shape-based
 * {@linkplain Engine#throttler(Object, Object...) authorization}, considering shapes enabled by the
 * {@linkplain Guard#Update} task and the {@linkplain Guard#Target} area, when operating on
 * {@linkplain Request#collection() collections}, or the {@linkplain Guard#Detail} area, when operating on other
 * resources;</li>
 *
 * <li>engine-assisted request payload {@linkplain JSONLDFormat#validate(IRI, Shape, javax.json.JsonObject) validation
 * };</li>
 *
 * <li>engine assisted resource {@linkplain Engine#update(Request) updating}.</li>
 *
 * </ul>
 *
 * <p>All operations are executed inside a single {@linkplain Engine engine transaction}.</p>
 */
public final class Updater extends Delegator {

	/**
	 * Creates a resource updater.
	 *
	 * @return a new resource updater
	 */
	public static Updater updater() {
		return new Updater();
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Updater() {

		final Engine engine=asset(engine());

		delegate(engine.wrap(((Handler)engine::update)

				.with(wrapper(Request::collection,
						throttler(Update, Target),
						throttler(Update, Detail)
				))

				.with(validator())

		));
	}

}
