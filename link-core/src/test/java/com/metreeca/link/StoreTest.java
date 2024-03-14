/*
 * Copyright © 2023-2024 Metreeca srl
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

package com.metreeca.link;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.junit.jupiter.api.Nested;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.function.BiFunction;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.metreeca.link.Frame.integer;
import static com.metreeca.link.Frame.*;
import static com.metreeca.link.Shape.*;

import static java.lang.Integer.parseInt;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.unmodifiableCollection;
import static java.util.Comparator.comparing;
import static java.util.Objects.requireNonNull;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;

public abstract class StoreTest {

    private static final IRI SPACE=iri("https://data.example.com/");
    private static final IRI TERMS=iri(SPACE, "/terms/");

    static {

        final Logger logger=Logger.getLogger("com.metreeca");

        logger.setLevel(Level.ALL);

        final Handler handler=new ConsoleHandler();

        handler.setLevel(Level.ALL);

        logger.addHandler(handler);

    }


    public static final IRI OFFICE_TYPE=term("Office");
    public static final IRI EMPLOYEE_TYPE=term("Employee");

    public static final IRI CODE=term("code");
    public static final IRI SENIORITY=term("seniority");
    public static final IRI TITLE=term("title");
    public static final IRI EMAIL=term("email");

    public static final IRI FORENAME=term("forename");
    public static final IRI SURNAME=term("surname");
    public static final IRI BIRTHDATE=term("birthdate");

    public static final IRI ACTIVE=term("active");
    public static final IRI YTD=term("ytd");
    public static final IRI LAST=term("last");
    public static final IRI DELTA=term("delta");

    public static final IRI OFFICE=term("office");
    public static final IRI EMPLOYEE=term("employee");
    public static final IRI SUPERVISOR=term("supervisor");
    public static final IRI REPORT=term("report");


    public static final Collection<Frame> OFFICES=unmodifiableCollection(parse(StoreTest::Offices));
    public static final Collection<Frame> EMPLOYEES=unmodifiableCollection(parse(StoreTest::Employees));


    public static IRI item(final String name) {

        if ( name == null ) {
            throw new NullPointerException("null name");
        }

        return iri(SPACE, name);
    }

    public static IRI term(final String name) {

        if ( name == null ) {
            throw new NullPointerException("null name");
        }

        return iri(TERMS, name);
    }


    public static Shape Resource() {
        return shape(

                property("id", ID),
                property("type", TYPE),

                property(RDFS.LABEL, required(), datatype(XSD.STRING)),
                property(RDFS.COMMENT, optional(), datatype(XSD.STRING))

        );
    }

    public static Shape Resources() {
        return virtual(Resource(),

                property(RDFS.MEMBER, multiple(), datatype(RESOURCE))

        );
    }

    public static Shape Office() {
        return shape(Resource());
    }

    public static Shape Employee() {
        return shape(Resource(),

                clazz(EMPLOYEE_TYPE),

                property(FORENAME, shape(required(), datatype(XSD.STRING))),
                property(SURNAME, shape(required(), datatype(XSD.STRING))),
                property(BIRTHDATE, shape(required(), datatype(XSD.DATE), pattern("\\d{4}-\\d{2}-\\d{2}"))),

                property(CODE, shape(required(), datatype(XSD.STRING), pattern("\\d{4}"))),
                property(SENIORITY, shape(required(), minInclusive(literal(0)), maxInclusive(literal(5)))),
                property(TITLE, shape(required(), datatype(XSD.STRING))),
                property(EMAIL, shape(required(), datatype(XSD.STRING))), // !!! pattern
                property(ACTIVE, shape(required(), datatype(XSD.DATETIME))), // !!! pattern

                property(YTD, shape(optional(), datatype(XSD.DECIMAL), minInclusive(literal(0.0)))),
                property(LAST, shape(optional(), datatype(XSD.DECIMAL), minInclusive(literal(0.0)))),
                property(DELTA, shape(optional(), datatype(XSD.DECIMAL))),

                property(OFFICE, shape(required(), Resource())),
                property(SUPERVISOR, () -> shape(optional(), Employee())),
                property(REPORT, () -> shape(multiple(), Employee()))

        );
    }

    public static Shape Employees() {
        return virtual(Resource(),

                property(RDFS.MEMBER, StoreTest::Employee)

        );
    }


    public static Store populate(final Store store) {

        if ( store == null ) {
            throw new NullPointerException("null store");
        }

        Map.of(

                Office(), OFFICES,
                Employee(), EMPLOYEES

        ).forEach((shape, values) -> {

            values.forEach(value -> {

                if ( !store.create(value.id().orElseThrow(), shape, value) ) {

                    throw new RuntimeException(format(
                            "unable to create resource <%s>", value.id()
                    ));

                }

            });

        });

        return store;
    }


    public static Optional<Frame> Office(final Frame frame) {
        return OFFICES.stream()
                .filter(o -> o.id().flatMap(id -> frame.id().map(id::equals)).orElse(false))
                .findFirst();
    }

    public static Optional<Frame> Office(final IRI id) {
        return OFFICES.stream()
                .filter(o -> o.id().filter(id::equals).isPresent())
                .findFirst();
    }

    public static Optional<Frame> Employee(final Frame frame) {
        return frame.id()
                .flatMap(StoreTest::Employee);
    }

    public static Optional<Frame> Employee(final IRI id) {
        return EMPLOYEES.stream()
                .filter(e -> e.id().map(id::equals).orElse(false))
                .findFirst();
    }


    private static Collection<Frame> Offices(final List<String> header, final Collection<List<String>> records) {
        return records.stream().map(record -> {

            final String code=record.get(header.indexOf("office"));
            final String city=record.get(header.indexOf("cityName"));
            final String country=record.get(header.indexOf("countryName"));

            return frame(
                    field(ID, item(format("/offices/%s", code))),
                    field(TYPE, OFFICE_TYPE),
                    field(RDFS.LABEL, literal(format("%s (%s)", city, country)))
            );

        }).collect(toList());
    }

    private static Collection<Frame> Employees(final List<String> header, final Collection<List<String>> records) {
        return records.stream().map(record -> {

            final String code=record.get(header.indexOf("code"));
            final String forename=record.get(header.indexOf("forename"));
            final String surname=record.get(header.indexOf("surname"));

            return frame(

                    field(ID, item(format("/employees/%s", code))),
                    field(TYPE, EMPLOYEE_TYPE),

                    field(RDFS.LABEL, literal(format("%s %s", forename, surname))),

                    field(CODE, literal(code)),
                    field(SENIORITY, literal(integer(parseInt(record.get(header.indexOf("seniority")))))),
                    field(TITLE, literal(record.get(header.indexOf("title")))),
                    field(EMAIL, literal(record.get(header.indexOf("email")))),

                    field(FORENAME, literal(forename)),
                    field(SURNAME, literal(surname)),
                    field(BIRTHDATE, literal(LocalDate.parse(record.get(header.indexOf("birthdate"))))),

                    field(ACTIVE, literal(OffsetDateTime.parse(record.get(header.indexOf("active"))))),

                    field(YTD, Optional.of(record.get(header.indexOf("ytd")))
                            .filter(not(String::isEmpty))
                            .map(BigDecimal::new)
                            .map(Frame::literal)
                    ),

                    field(LAST, Optional.of(record.get(header.indexOf("last")))
                            .filter(not(String::isEmpty))
                            .map(BigDecimal::new)
                            .map(Frame::literal)
                    ),

                    field(DELTA, Optional.of(record.get(header.indexOf("delta")))
                            .filter(not(String::isEmpty))
                            .map(BigDecimal::new)
                            .map(Frame::literal)
                    ),

                    field(OFFICE, frame(
                            field(ID, item(format("/offices/%s", record.get(header.indexOf("office")))))
                    )),

                    field(SUPERVISOR, Optional.of(record.get(header.indexOf("supervisor")))
                            .map(s -> frame(
                                    field(ID, item(format("/employees/%s", s)))
                            ))
                    ),

                    field(REPORT, records.stream()
                            .filter(r -> code.equals(r.get(header.indexOf("supervisor"))))
                            .map(r -> frame(
                                    field(ID, item(format("/employees/%s", r.get(header.indexOf("code")))))
                            ))
                            .collect(toList())
                    )

            );

        }).collect(toList());
    }


    private static Collection<Frame> parse(
            final BiFunction<? super List<String>, ? super List<List<String>>, Collection<Frame>> mapper
    ) {
        try ( final BufferedReader reader=new BufferedReader(new InputStreamReader(
                requireNonNull(StoreTest.class.getResourceAsStream(StoreTest.class.getSimpleName()+".tsv")), UTF_8
        )) ) {

            final List<List<String>> records=reader.lines()
                    .map(line -> List.of(line.split("\t")))
                    .collect(toList());

            final List<String> header=records.get(0);

            final Collection<Frame> frames=mapper.apply(header, records.subList(1, records.size()));

            return frames.stream().collect(toCollection(() -> new TreeSet<>(comparing(frame ->
                    frame.id().map(Value::stringValue).orElse(null)
            ))));

        } catch ( final IOException e ) {

            throw new UncheckedIOException(e);

        }
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    protected abstract Store store();


    @Nested
    final class Retrieve extends StoreTestRetrieve {

        @Override public Store store() {
            return StoreTest.this.store();
        }

    }

    @Nested
    final class RetrieveIndex extends StoreTestRetrieveIndex {

        @Override public Store store() {
            return StoreTest.this.store();
        }

    }

    @Nested
    final class RetrieveTable extends StoreTestRetrieveTable {

        @Override public Store store() {
            return StoreTest.this.store();
        }

    }
    
    @Nested
    final class Create extends StoreTestCreate {

        @Override public Store store() {
            return StoreTest.this.store();
        }

    }

    @Nested
    final class Update extends StoreTestUpdate {

        @Override public Store store() {
            return StoreTest.this.store();
        }

    }

    @Nested
    final class Delete extends StoreTestDelete {

        @Override public Store store() {
            return StoreTest.this.store();
        }

    }

}