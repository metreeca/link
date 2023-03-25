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

import com.metreeca.rest.jsonld.*;
import com.metreeca.rest.shacl.Optional;
import com.metreeca.rest.shacl.Required;

@Namespace(Resource.base+"/terms/")
@Namespace(prefix = "rdfs", value = "http://www.w3.org/2000/01/rdf-schema#")
@Namespace(prefix = "dct", value = "http://purl.org/dc/terms")
public abstract class Resource {

    static final String base="https://data.example.com";


    public static String id(final String id) {
        return id.startsWith("/") ? base+id : id;
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Id
    private String id;


    @Required
    @Property("rdfs:")
    private String label;

    @Optional
    @Property("rdfs:")
    private String comment;


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

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
