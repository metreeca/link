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

package com.metreeca.link.toys;

import com.metreeca.link.jsonld.Property;
import com.metreeca.link.jsonld.Type;
import com.metreeca.link.shacl.*;

import java.util.Set;


@Type
public final class Employee extends Resource {

    @Required
    @Pattern("\\d{4}")
    private String code;


    @Required
    private String forename;

    @Required
    private String surname;


    @Required
    private String email;

    @Required
    private String title;

    @Required
    //@MinExclusive(integer=1)
    //@MaxInclusive(integer=5)
    private int seniority;


    @Required
    private Office office;

    @Optional
    private Employee supervisor;

    @Optional
    @Property("report")
    private Set<Employee> reports;


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

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


    public int getSeniority() {
        return seniority;
    }

    public void setSeniority(final int seniority) {
        this.seniority=seniority;
    }


    public Office getOffice() {
        return office;
    }

    public void setOffice(final Office office) {
        this.office=office;
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

}
