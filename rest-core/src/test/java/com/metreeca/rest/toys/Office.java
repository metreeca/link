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

import com.metreeca.rest.shacl.Pattern;
import com.metreeca.rest.shacl.Required;


public final class Office extends Resource {

    @Required
    @Pattern("\\d{4}")
    private String code;

    @Required
    private String area;


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public String getCode() {
        return code;
    }

    public void setCode(final String code) {
        this.code=code;
    }


    public String getArea() {
        return area;
    }

    public void setArea(final String area) {
        this.area=area;
    }

}
