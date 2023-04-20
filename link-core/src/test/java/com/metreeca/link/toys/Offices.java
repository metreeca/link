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


import java.util.Set;

import static com.metreeca.link.Frame.with;

public final class Offices extends Resources<Office> {

    public static final Set<Office> Offices=Set.of(

            with(new Office(), office -> {

                office.setId(id("/offices/1"));
                office.setLabel("San Francisco (USA)");
                office.setCode("1");

            }),

            with(new Office(), office -> {

                office.setId(id("/offices/2"));
                office.setLabel("Boston (USA)");
                office.setCode("2");

            }),

            with(new Office(), office -> {

                office.setId(id("/offices/3"));
                office.setLabel("NYC (USA)");
                office.setCode("3");

            }),

            with(new Office(), office -> {

                office.setId(id("/offices/4"));
                office.setLabel("Paris (France)");
                office.setCode("4");

            }),

            with(new Office(), office -> {

                office.setId(id("/offices/5"));
                office.setLabel("Paris (France)");
                office.setCode("5");

            }),

            with(new Office(), office -> {

                office.setId(id("/offices/5"));
                office.setLabel("Tokyo (Japan)");
                office.setCode("5");
                office.setArea("Japan");

            }),

            with(new Office(), office -> {

                office.setId(id("/offices/6"));
                office.setLabel("Sydney (Australia)");
                office.setCode("6");
                office.setArea("APAC");

            }),

            with(new Office(), office -> {

                office.setId(id("/offices/7"));
                office.setLabel("London (UK)");
                office.setCode("7");
                office.setArea("EMEA");

            })

    );

}
