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

package com.metreeca.rest.toys;


import com.metreeca.rest.jsonld.Virtual;

import java.util.Set;

import static com.metreeca.rest.Frame.with;

@Virtual
public final class Employees extends Resources<Employee> {

    public static final Set<Employee> Employees=Set.of(

            with(new Employee(), e -> {
                e.setId(id("/employees/1002"));
                e.setLabel("Diane Murphy");
                e.setCode("1002");
                e.setForename("Diane");
                e.setSurname("Murphy");
                e.setEmail("dmurphy@example.com");
                e.setTitle("President");
                e.setSeniority(5);
                e.setOffice(with(new Office(), o -> o.setId(id("/offices/1"))));
                e.setReports(Set.of(
                        with(new Employee(), r -> r.setId(id("/employees/1056"))),
                        with(new Employee(), r -> r.setId(id("/employees/1076")))
                ));
            }),

            with(new Employee(), e -> {
                e.setId(id("/employees/1056"));
                e.setLabel("Mary Patterson");
                e.setCode("1056");
                e.setForename("Mary");
                e.setSurname("Patterson");
                e.setEmail("mpatterso@example.com");
                e.setTitle("VP Sales");
                e.setSeniority(4);
                e.setOffice(with(new Office(), o -> o.setId(id("/offices/1"))));
                e.setSupervisor(with(new Employee(), s -> s.setId(id("/employees/1002"))));
                e.setReports(Set.of(
                        with(new Employee(), r -> r.setId(id("/employees/1088"))),
                        with(new Employee(), r -> r.setId(id("/employees/1102"))),
                        with(new Employee(), r -> r.setId(id("/employees/1143"))),
                        with(new Employee(), r -> r.setId(id("/employees/1621")))
                ));
            }),

            with(new Employee(), e -> {
                e.setId(id("/employees/1076"));
                e.setLabel("Jeff Firrelli");
                e.setCode("1076");
                e.setForename("Jeff");
                e.setSurname("Firrelli");
                e.setEmail("jfirrelli@example.com");
                e.setTitle("VP Marketing");
                e.setSeniority(4);
                e.setOffice(with(new Office(), o -> o.setId(id("/offices/1"))));
                e.setSupervisor(with(new Employee(), s -> s.setId(id("/employees/1002"))));
            }),

            with(new Employee(), e -> {
                e.setId(id("/employees/1088"));
                e.setLabel("William Patterson");
                e.setCode("1088");
                e.setForename("William");
                e.setSurname("Patterson");
                e.setEmail("wpatterson@example.com");
                e.setTitle("Sales Manager (APAC)");
                e.setSeniority(3);
                e.setOffice(with(new Office(), o -> o.setId(id("/offices/6"))));
                e.setSupervisor(with(new Employee(), s -> s.setId(id("/employees/1056"))));
                e.setReports(Set.of(
                        with(new Employee(), r -> r.setId(id("/employees/1611"))),
                        with(new Employee(), r -> r.setId(id("/employees/1612"))),
                        with(new Employee(), r -> r.setId(id("/employees/1619")))
                ));
            }),

            with(new Employee(), e -> {
                e.setId(id("/employees/1102"));
                e.setLabel("Gerard Bondur");
                e.setCode("1102");
                e.setForename("Gerard");
                e.setSurname("Bondur");
                e.setEmail("gbondur@example.com");
                e.setTitle("Sale Manager (EMEA)");
                e.setSeniority(4);
                e.setOffice(with(new Office(), o -> o.setId(id("/offices/4"))));
                e.setSupervisor(with(new Employee(), s -> s.setId(id("/employees/1056"))));
                e.setReports(Set.of(
                        with(new Employee(), r -> r.setId(id("/employees/1337"))),
                        with(new Employee(), r -> r.setId(id("/employees/1370"))),
                        with(new Employee(), r -> r.setId(id("/employees/1401"))),
                        with(new Employee(), r -> r.setId(id("/employees/1501"))),
                        with(new Employee(), r -> r.setId(id("/employees/1504"))),
                        with(new Employee(), r -> r.setId(id("/employees/1702")))
                ));
            }),

            with(new Employee(), e -> {
                e.setId(id("/employees/1143"));
                e.setLabel("Anthony Bow");
                e.setCode("1143");
                e.setForename("Anthony");
                e.setSurname("Bow");
                e.setEmail("abow@example.com");
                e.setTitle("Sales Manager (NA)");
                e.setSeniority(3);
                e.setOffice(with(new Office(), o -> o.setId(id("/offices/1"))));
                e.setSupervisor(with(new Employee(), s -> s.setId(id("/employees/1056"))));
                e.setReports(Set.of(
                        with(new Employee(), r -> r.setId(id("/employees/1165"))),
                        with(new Employee(), r -> r.setId(id("/employees/1166"))),
                        with(new Employee(), r -> r.setId(id("/employees/1188"))),
                        with(new Employee(), r -> r.setId(id("/employees/1216"))),
                        with(new Employee(), r -> r.setId(id("/employees/1286"))),
                        with(new Employee(), r -> r.setId(id("/employees/1323")))
                ));
            }),

            with(new Employee(), e -> {
                e.setId(id("/employees/1165"));
                e.setLabel("Leslie Jennings");
                e.setCode("1165");
                e.setForename("Leslie");
                e.setSurname("Jennings");
                e.setEmail("ljennings@example.com");
                e.setTitle("Sales Rep");
                e.setSeniority(1);
                e.setOffice(with(new Office(), o -> o.setId(id("/offices/1"))));
                e.setSupervisor(with(new Employee(), s -> s.setId(id("/employees/1143"))));
            }),

            with(new Employee(), e -> {
                e.setId(id("/employees/1166"));
                e.setLabel("Leslie Thompson");
                e.setCode("1166");
                e.setForename("Leslie");
                e.setSurname("Thompson");
                e.setEmail("lthompson@example.com");
                e.setTitle("Sales Rep");
                e.setSeniority(1);
                e.setOffice(with(new Office(), o -> o.setId(id("/offices/1"))));
                e.setSupervisor(with(new Employee(), s -> s.setId(id("/employees/1143"))));
            }),

            with(new Employee(), e -> {
                e.setId(id("/employees/1188"));
                e.setLabel("Julie Firrelli");
                e.setCode("1188");
                e.setForename("Julie");
                e.setSurname("Firrelli");
                e.setEmail("jfirrelli@example.com");
                e.setTitle("Sales Rep");
                e.setSeniority(1);
                e.setOffice(with(new Office(), o -> o.setId(id("/offices/2"))));
                e.setSupervisor(with(new Employee(), s -> s.setId(id("/employees/1143"))));
            }),

            with(new Employee(), e -> {
                e.setId(id("/employees/1216"));
                e.setLabel("Steve Patterson");
                e.setCode("1216");
                e.setForename("Steve");
                e.setSurname("Patterson");
                e.setEmail("spatterson@example.com");
                e.setTitle("Sales Rep");
                e.setSeniority(2);
                e.setOffice(with(new Office(), o -> o.setId(id("/offices/2"))));
                e.setSupervisor(with(new Employee(), s -> s.setId(id("/employees/1143"))));
            }),

            with(new Employee(), e -> {
                e.setId(id("/employees/1286"));
                e.setLabel("Foon Yue Tseng");
                e.setCode("1286");
                e.setForename("Foon Yue");
                e.setSurname("Tseng");
                e.setEmail("ftseng@example.com");
                e.setTitle("Sales Rep");
                e.setSeniority(1);
                e.setOffice(with(new Office(), o -> o.setId(id("/offices/3"))));
                e.setSupervisor(with(new Employee(), s -> s.setId(id("/employees/1143"))));
            }),

            with(new Employee(), e -> {
                e.setId(id("/employees/1323"));
                e.setLabel("George Vanauf");
                e.setCode("1323");
                e.setForename("George");
                e.setSurname("Vanauf");
                e.setEmail("gvanauf@example.com");
                e.setTitle("Sales Rep");
                e.setSeniority(1);
                e.setOffice(with(new Office(), o -> o.setId(id("/offices/3"))));
                e.setSupervisor(with(new Employee(), s -> s.setId(id("/employees/1143"))));
            }),

            with(new Employee(), e -> {
                e.setId(id("/employees/1337"));
                e.setLabel("Loui Bondur");
                e.setCode("1337");
                e.setForename("Loui");
                e.setSurname("Bondur");
                e.setEmail("lbondur@example.com");
                e.setTitle("Sales Rep");
                e.setSeniority(1);
                e.setOffice(with(new Office(), o -> o.setId(id("/offices/4"))));
                e.setSupervisor(with(new Employee(), s -> s.setId(id("/employees/1102"))));
            }),

            with(new Employee(), e -> {
                e.setId(id("/employees/1370"));
                e.setLabel("Gerard Hernandez");
                e.setCode("1370");
                e.setForename("Gerard");
                e.setSurname("Hernandez");
                e.setEmail("ghernande@example.com");
                e.setTitle("Sales Rep");
                e.setSeniority(2);
                e.setOffice(with(new Office(), o -> o.setId(id("/offices/4"))));
                e.setSupervisor(with(new Employee(), s -> s.setId(id("/employees/1102"))));
            }),

            with(new Employee(), e -> {
                e.setId(id("/employees/1401"));
                e.setLabel("Pamela Castillo");
                e.setCode("1401");
                e.setForename("Pamela");
                e.setSurname("Castillo");
                e.setEmail("pcastillo@example.com");
                e.setTitle("Sales Rep");
                e.setSeniority(2);
                e.setOffice(with(new Office(), o -> o.setId(id("/offices/4"))));
                e.setSupervisor(with(new Employee(), s -> s.setId(id("/employees/1102"))));
            }),

            with(new Employee(), e -> {
                e.setId(id("/employees/1501"));
                e.setLabel("Larry Bott");
                e.setCode("1501");
                e.setForename("Larry");
                e.setSurname("Bott");
                e.setEmail("lbott@example.com");
                e.setTitle("Sales Rep");
                e.setSeniority(2);
                e.setOffice(with(new Office(), o -> o.setId(id("/offices/7"))));
                e.setSupervisor(with(new Employee(), s -> s.setId(id("/employees/1102"))));
            }),

            with(new Employee(), e -> {
                e.setId(id("/employees/1504"));
                e.setLabel("Barry Jones");
                e.setCode("1504");
                e.setForename("Barry");
                e.setSurname("Jones");
                e.setEmail("bjones@example.com");
                e.setTitle("Sales Rep");
                e.setSeniority(1);
                e.setOffice(with(new Office(), o -> o.setId(id("/offices/7"))));
                e.setSupervisor(with(new Employee(), s -> s.setId(id("/employees/1102"))));
            }),

            with(new Employee(), e -> {
                e.setId(id("/employees/1611"));
                e.setLabel("Andy Fixter");
                e.setCode("1611");
                e.setForename("Andy");
                e.setSurname("Fixter");
                e.setEmail("afixter@example.com");
                e.setTitle("Sales Rep");
                e.setSeniority(1);
                e.setOffice(with(new Office(), o -> o.setId(id("/offices/6"))));
                e.setSupervisor(with(new Employee(), s -> s.setId(id("/employees/1088"))));
            }),

            with(new Employee(), e -> {
                e.setId(id("/employees/1612"));
                e.setLabel("Peter Marsh");
                e.setCode("1612");
                e.setForename("Peter");
                e.setSurname("Marsh");
                e.setEmail("pmarsh@example.com");
                e.setTitle("SalesRep");
                e.setSeniority(1);
                e.setOffice(with(new Office(), o -> o.setId(id("/offices/6"))));
                e.setSupervisor(with(new Employee(), s -> s.setId(id("/employees/1088"))));
            }),

            with(new Employee(), e -> {
                e.setId(id("/employees/1619"));
                e.setLabel("TomKing");
                e.setCode("1619");
                e.setForename("Tom");
                e.setSurname("King");
                e.setEmail("tking@example.com");
                e.setTitle("Sales Rep");
                e.setSeniority(2);
                e.setOffice(with(new Office(), o -> o.setId(id("/offices/6"))));
                e.setSupervisor(with(new Employee(), s -> s.setId(id("/employees/1088"))));
            }),

            with(new Employee(), e -> {
                e.setId(id("/employees/1621"));
                e.setLabel("Mami Nishi");
                e.setCode("1621");
                e.setForename("Mami");
                e.setSurname("Nishi");
                e.setEmail("mnishi@example.com");
                e.setTitle("Sales Rep");
                e.setSeniority(2);
                e.setOffice(with(new Office(), o -> o.setId(id("/offices/5"))));
                e.setSupervisor(with(new Employee(), s -> s.setId(id("/employees/1056"))));
                e.setReports(Set.of(
                        with(new Employee(), r -> r.setId(id("/employees/1625")))
                ));
            }),


            with(new Employee(), e -> {
                e.setId(id("/employees/1625"));
                e.setLabel("Yoshimi Kato");
                e.setCode("1625");
                e.setForename("Yoshimi");
                e.setSurname("Kato");
                e.setEmail("ykato@example.com");
                e.setTitle("Sales Rep");
                e.setSeniority(2);
                e.setOffice(with(new Office(), o -> o.setId(id("/offices/5"))));
                e.setSupervisor(with(new Employee(), s -> s.setId(id("/employees/1621"))));
            }),

            with(new Employee(), e -> {
                e.setId(id("/employees/1702"));
                e.setLabel("Martin Gerard");
                e.setCode("1702");
                e.setForename("Martin");
                e.setSurname("Gerard");
                e.setEmail("mgerard@example.com");
                e.setTitle("Sales Rep");
                e.setSeniority(2);
                e.setOffice(with(new Office(), o -> o.setId(id("/offices/4"))));
                e.setSupervisor(with(new Employee(), s -> s.setId(id("/employees/1102"))));
            })

    );

}
