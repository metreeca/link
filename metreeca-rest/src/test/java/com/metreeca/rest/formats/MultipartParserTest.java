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

package com.metreeca.rest.formats;

import com.metreeca.rest.*;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.*;
import static org.assertj.core.api.Assertions.assertThat;


final class MultipartParserTest {

	private InputStream content(final String content) {
		return new ByteArrayInputStream(content.replace("\n", "\r\n").getBytes(UTF_8));
	}

	private Collection<Message<?>> parts(final String content) throws IOException {

		final Map<String, Message<?>> parts=new LinkedHashMap<>();

		new MultipartParser(1000, 1000, content(content), "boundary", (headers, body) -> {

			final Map<String, List<String>> map=headers.stream().collect(groupingBy(
					Entry::getKey,
					LinkedHashMap::new,
					mapping(Entry::getValue, toList())
			));

			parts.put("part"+parts.size(), new Request()
					.headers(map)
					.body(InputFormat.input(), () -> body)
			);

		}).parse();

		return parts.values();
	}


	@Nested final class Splitting {

		private List<String> parts(final String content) throws IOException {
			return MultipartParserTest.this.parts(content).stream()
					.map(message -> message.body(TextFormat.text()).fold(e -> "", identity()))
					.collect(toList());
		}

		@Test void testIgnoreFrame() throws IOException {

			Assertions.assertThat(parts("")).isEmpty();

			Assertions.assertThat(parts("preamble\n--boundary--")).isEmpty();
			Assertions.assertThat(parts("--boundary--\nepilogue")).isEmpty();

		}

		@Test void testSplitParts() throws IOException {

			Assertions.assertThat(parts("--boundary\n\ncontent\n--boundary--"))
					.as("canonical")
					.containsExactly("content");

			Assertions.assertThat(parts("--boundary\n\ncontent\n\n\n--boundary--"))
					.as("trailing newlines")
					.containsExactly("content\r\n\r\n");

			Assertions.assertThat(parts("--boundary\n\ncontent"))
					.as("lenient missing closing boundary")
					.containsExactly("content");

			Assertions.assertThat(parts("--boundary\n\none\n--boundary\n\ntwo\n--boundary--"))
					.as("multiple parts")
					.containsExactly("one", "two");

		}

		@Test void testIgnorePreamble() throws IOException {

			Assertions.assertThat(parts("--boundary\n\ncontent\n--boundary--"))
					.as("missing")
					.containsExactly("content");

			Assertions.assertThat(parts("preamble\n--boundary\n\ncontent\n--boundary--"))
					.as("ignored")
					.containsExactly("content");

		}

		@Test void testIgnoreEpilogue() throws IOException {

			Assertions.assertThat(parts("--boundary\n\ncontent\n--boundary--"))
					.as("missing")
					.containsExactly("content");

			Assertions.assertThat(parts("--boundary\n\ncontent\n--boundary--\n trailing\ngarbage"))
					.as("ignored")
					.containsExactly("content");

		}

	}

	@Nested final class Headers {

		@Test void testParseHeaders() throws IOException {
			Assertions.assertThat(parts("--boundary\nsingle: value\nmultiple: one\nmultiple: two\n\ncontent"))
					.isNotEmpty()
					.hasOnlyOneElementSatisfying(message -> MessageAssert.assertThat(message)
							.hasHeaders("single", "value")
							.hasHeaders("multiple", "one", "two")
							.hasBody(TextFormat.text(), text -> assertThat(text).isEqualTo("content"))
					);
		}

		@Test void testHandleEmptyHeaders() throws IOException {
			Assertions.assertThat(parts("--boundary\nempty:\n\ncontent"))
					.isNotEmpty()
					.hasOnlyOneElementSatisfying(message -> MessageAssert.assertThat(message)
							.hasHeaders("empty")
							.hasBody(TextFormat.text(), text -> assertThat(text).isEqualTo("content"))
					);
		}

		@Test void testHandleEOFInHeaders() throws IOException {
			Assertions.assertThat(parts("--boundary\nsingle: value"))
					.isNotEmpty()
					.hasOnlyOneElementSatisfying(message -> MessageAssert.assertThat(message)
							.hasHeaders("single", "value")
					);
		}

		@Test void testRejectMalformedHeaders() {

			Assertions.assertThatExceptionOfType(MessageException.class)
					.as("spaces before colon")
					.isThrownBy(() -> parts("--boundary\nheader : value\n\ncontent"));

			Assertions.assertThatExceptionOfType(MessageException.class)
					.as("malformed name")
					.isThrownBy(() -> parts("--boundary\nhea der: value\n\ncontent"));

			Assertions.assertThatExceptionOfType(MessageException.class)
					.as("malformed value")
					.isThrownBy(() -> parts("--boundary\nhea der: val\rue\n\ncontent"));

		}

	}

	@Nested final class Limits {

		private MultipartParser parser(final int part, final int body, final String content) {
			return new MultipartParser(part, body, content(content), "boundary", (headers, _body) -> {});
		}


		@Test void testEnforceBodySizeLimits() {

			Assertions.assertThatExceptionOfType(MessageException.class)
					.as("body size exceeded")
					.isThrownBy(() -> parser(5, 25,
							"--boundary\n\none\n--boundary\n\ntwo\n--boundary--"
					).parse());

		}

		@Test void testEnforcePartSizeLimits() {

			Assertions.assertThatExceptionOfType(MessageException.class)
					.as("parts size exceeded")
					.isThrownBy(() -> parser(10, 1000,
							"--boundary\n\nshort\n--boundary\n\nlong content\n--boundary--"
					).parse());

		}

	}

}
