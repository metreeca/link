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

public abstract class StoreTestDelete {

    protected abstract Store store();


    // @Test void testDelete() {
    //
    //     final Store store=store();
    //
    //     final Employee delete=with(new Employee(), o -> o.setId(id("/employees/1702")));
    //
    //     assertThat(store.delete(delete))
    //             .hasValueSatisfying(employee -> assertThat(employee.getId())
    //                     .isEqualTo(delete.getId())
    //             );
    //
    //     assertThat(store.retrieve(with(new Employee(), employee -> employee.setId(delete.getId())), false))
    //             .isEmpty();
    //
    //
    // }


    // @Test void testReportUnknownResources() {
    //
    //     final Store store=store();
    //
    //     final Employee delete=with(new Employee(), office -> office.setId(id("/employees/999")));
    //
    //     assertThat(store.delete(delete))
    //             .isEmpty();
    //
    // }

    // @Test void testReportMissingId() {
    //
    //     final Store store=store();
    //
    //     assertThatIllegalArgumentException()
    //             .isThrownBy(() -> store.delete(new Employee()));
    // }

}
