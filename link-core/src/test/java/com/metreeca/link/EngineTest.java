/*
 * Copyright © 2023 Metreeca srl
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

import com.metreeca.link.jsonld.*;
import com.metreeca.link.shacl.Optional;
import com.metreeca.link.shacl.Pattern;
import com.metreeca.link.shacl.Required;

import org.junit.jupiter.api.Nested;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static com.metreeca.link.Frame.with;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.*;

public abstract class EngineTest {

    private static final String Base="https://data.example.com";

    public static final List<Employee> Employees=Employees();


    public static String id(final String id) {
        return id.startsWith("/") ? Base+id : id;
    }


    private static List<Employee> Employees() {
        try ( final BufferedReader reader=new BufferedReader(new InputStreamReader(
                requireNonNull(EngineTest.class.getResourceAsStream("EngineTest.tsv")), UTF_8
        )) ) {

            final List<List<String>> records=reader.lines()
                    .map(line -> List.of(line.split("\t")))
                    .collect(toList());

            final List<String> header=records.get(0);

            final List<Employee> employees=records.stream().skip(1)

                    .map(record -> with(new Employee(), employee -> {

                        final String code=record.get(header.indexOf("code"));
                        final String forename=record.get(header.indexOf("forename"));
                        final String surname=record.get(header.indexOf("surname"));

                        employee.setId(format("%s/employees/%s", Base, code));
                        employee.setLabel(format("%s %s", forename, surname));

                        employee.setCode(code);
                        employee.setForename(forename);
                        employee.setSurname(surname);
                        // employee.setBirthdate(LocalDate.parse(record.get(header.indexOf("birthdate"))));
                        employee.setSeniority(Integer.parseInt(record.get(header.indexOf("seniority"))));
                        employee.setTitle(record.get(header.indexOf("title")));
                        employee.setEmail(record.get(header.indexOf("email")));

                        employee.setSupervisor(java.util.Optional.of(record.get(header.indexOf("supervisor")))
                                .filter(not(String::isEmpty))
                                .map(c -> with(new Employee(), s -> s.setCode(c)))
                                .orElse(null)
                        );

                        // employee.setActive(Instant.parse(record.get(header.indexOf("active"))));

                    }))

                    .collect(toUnmodifiableList());

            employees.forEach(employee -> {

                employee.setSupervisor(java.util.Optional.ofNullable(employee.getSupervisor())
                        .map(Employee::getCode)
                        .flatMap(c -> employees.stream()
                                .filter(e -> c.equals(e.getCode()))
                                .findFirst()
                        )
                        .orElse(null)
                );


                employee.setReports(java.util.Optional

                        .of(employees.stream()

                                .filter(e -> java.util.Optional.ofNullable(e.getSupervisor())
                                        .filter(s -> s.getCode().equals(employee.getCode()))
                                        .isPresent()
                                )

                                .collect(toSet())
                        )

                        .filter(not(Set::isEmpty))
                        .orElse(null)

                );

            });

            return employees;

        } catch ( final IOException e ) {

            throw new UncheckedIOException(e);

        }
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    protected abstract Engine engine();

    protected Engine testbed() {

        final Engine engine=engine();

        Employees.forEach(employee -> {

            engine.create(employee).orElseThrow(() -> new RuntimeException(format(
                    "unable to create employee <%s>", employee.getId()
            )));

        });

        return engine;
    }


    @Nested
    final class Retrieve extends EngineTestRetrieve {

        @Override public Engine testbed() {
            return EngineTest.this.testbed();
        }

    }

    @Nested
    final class RetrieveQuery extends EngineTestRetrieveQuery {

        @Override public Engine testbed() {
            return EngineTest.this.testbed();
        }

    }

    @Nested
    final class RetrieveTable extends EngineTestRetrieveTable {

        @Override public Engine testbed() {
            return EngineTest.this.testbed();
        }

    }

    @Nested
    final class RetrieveXform extends EngineTestRetrieveXform {

        @Override public Engine testbed() {
            return EngineTest.this.testbed();
        }

    }

    @Nested
    final class Create extends EngineTestCreate {

        @Override public Engine testbed() {
            return EngineTest.this.testbed();
        }

    }

    @Nested
    final class Update extends EngineTestUpdate {

        @Override public Engine testbed() {
            return EngineTest.this.testbed();
        }

    }

    @Nested
    final class Delete extends EngineTestDelete {

        @Override public Engine testbed() {
            return EngineTest.this.testbed();
        }

    }

    @Nested
    final class RetrieveXcode extends EngineTestXcode {

        @Override protected Engine engine() {
            return EngineTest.this.engine();
        }

    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Namespace(Base+"/terms/")
    @Namespace(prefix="rdfs", value="http://www.w3.org/2000/01/rdf-schema#")
    @Namespace(prefix="dct", value="http://purl.org/dc/terms")
    public abstract static class Resource {

        @Id
        private String id;


        @Required
        @Property("rdfs:")
        private String label;

        @Optional
        @Property("rdfs:")
        private String comment;


        public String getId() {
            return id;
        }

        public void setId(final String id) {
            this.id=id;
        }


        public String getLabel() {
            return label;
        }

        public void setLabel(final String label) {
            this.label=label;
        }


        public String getComment() {
            return comment;
        }

        public void setComment(final String comment) {
            this.comment=comment;
        }

    }

    public abstract static class Resources<T extends Resource> extends Resource {

        @Property("rdfs:member")
        private Collection<T> members;


        public Collection<T> getMembers() {
            return members;
        }

        public void setMembers(final Collection<T> members) {
            this.members=members;
        }

    }


    public static final class Reference extends Resource { }


    @Type
    public static final class Employee extends Resource {

        @Required
        @Pattern("\\d{4}")
        private String code;


        @Required
        private String forename;

        @Required
        private String surname;

        @Required
        private LocalDate birthdate;


        @Required
        private String email;

        @Required
        private String title;

        @Required
        //@MinExclusive(integer=1)
        //@MaxInclusive(integer=5)
        private Integer seniority;


        @Optional
        private Employee supervisor;

        @Optional
        @Property("report")
        private Set<Employee> reports;


        @Required
        private Instant active;


        public String getCode() {
            return code;
        }

        public void setCode(final String code) {
            this.code=code;
        }


        public String getForename() {
            return forename;
        }

        public void setForename(final String forename) {
            this.forename=forename;
        }


        public String getSurname() {
            return surname;
        }

        public void setSurname(final String surname) {
            this.surname=surname;
        }


        // public LocalDate getBirthdate() {
        //     return birthdate;
        // }
        //
        // public void setBirthdate(final LocalDate birthdate) {
        //     this.birthdate=birthdate;
        // }


        public String getEmail() {
            return email;
        }

        public void setEmail(final String email) {
            this.email=email;
        }


        public String getTitle() {
            return title;
        }

        public void setTitle(final String title) {
            this.title=title;
        }


        public Integer getSeniority() {
            return seniority;
        }

        public void setSeniority(final Integer seniority) {
            this.seniority=seniority;
        }


        public Employee getSupervisor() {
            return supervisor;
        }

        public void setSupervisor(final Employee supervisor) {
            this.supervisor=supervisor;
        }


        public Set<Employee> getReports() {
            return reports;
        }

        public void setReports(final Set<Employee> reports) {
            this.reports=reports;
        }


        // public Instant getActive() {
        //     return active;
        // }
        //
        // public void setActive(final Instant active) {
        //     this.active=active;
        // }

    }

    @Virtual
    public static final class Employees extends Resources<Employee> { }

}