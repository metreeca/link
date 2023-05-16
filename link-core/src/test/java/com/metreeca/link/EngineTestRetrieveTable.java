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

public abstract class EngineTestRetrieveTable {

    protected abstract Engine testbed();

    //     @Test void testEmptyResultSet() {
    //         exec(dataset(), () -> assertThat(relate(frame(resource), items(
    //
    //                 field(RDF.TYPE, filter(all(RDF.NIL)))
    //
    //         ))).isEmpty());
    //     }
    //
    //     @Test void testEmptyProjection() {
    //         exec(dataset(), () -> assertThat(relate(frame(employees), items(
    //
    //                 filter(clazz(Employee))
    //
    //         ))).hasValue(frame(employees).frames(Contains, resources.stream()
    //                 .filter(frame -> frame.value(RDF.TYPE).filter(Employee::equals).isPresent())
    //                 .map(frame -> frame(frame.focus()))
    //         )));
    //     }


    // @Nested final class RelateTerms {
    //
    //     private Frame query(
    //             final Frame frame, final Function<Frame, Stream<Value>> mapper
    //     ) {
    //         return query(frame, mapper, 0, 0);
    //     }
    //
    //     private Frame query(
    //             final Frame frame, final Function<Frame, Stream<Value>> mapper,
    //             final int offset, final int limit
    //     ) {
    //
    //         final Predicate<Frame> filter=f -> f.value(RDF.TYPE).filter(Employee::equals).isPresent();
    //
    //         final Map<Value, Frame> index=resources.stream().filter(filter).collect(toMap(
    //                 Frame::focus, f -> frame(f.focus())
    //                         .values(RDFS.LABEL, f.values(RDFS.LABEL))
    //                         .values(RDFS.COMMENT, f.values(RDFS.COMMENT))
    //         ));
    //
    //         return frame(frame.focus()).frames(Engine.terms, resources.stream()
    //
    //                 .filter(filter)
    //                 .flatMap(mapper)
    //
    //                 .collect(groupingBy(identity(), counting()))
    //                 .entrySet().stream()
    //
    //                 .sorted(Map.Entry.<Value, Long>comparingByValue().reversed()
    //                         .thenComparing(comparingByKey(Values::compare))
    //                 )
    //
    //                 .skip(offset)
    //                 .limit(limit == 0 ? Long.MAX_VALUE : limit)
    //
    //                 .map(entry -> new AbstractMap.SimpleImmutableEntry<>(
    //                         index.getOrDefault(entry.getKey(), frame(entry.getKey())),
    //                         entry.getValue()
    //                 ))
    //
    //                 .map(entry -> frame(bnode())
    //                         .frame(Engine.value, entry.getKey())
    //                         .integer(Engine.count, entry.getValue())
    //                 )
    //
    //         );
    //     }
    //
    //
    //     @Test void testEmptyResultSet() {
    //         exec(dataset(), () -> assertThat(relate(frame(employees), terms(
    //
    //                 field(RDF.TYPE, filter(all(RDF.NIL))), emptyList(), 0, 0
    //
    //         ))).isEmpty());
    //     }
    //
    //     @Test void testEmptyProjection() {
    //         exec(dataset(), () -> assertThat(relate(frame(employees), terms(
    //
    //                 filter(clazz(Employee)), emptyList(), 0, 0
    //
    //         ))).hasValueSatisfying(frame -> FrameAssert.assertThat(frame)
    //
    //                 .isIsomorphicTo(query(frame(employees), f -> Stream.of(f.focus()), 0, 0))
    //
    //         ));
    //     }
    //
    //
    //     @Test void testFiltered() {
    //         exec(dataset(), () -> assertThat(relate(frame(employees), terms(
    //
    //                 and(
    //                         filter(clazz(Employee)),
    //                         filter(field(seniority, minInclusive(literal(3)))),
    //                         field(seniority)
    //                 ),
    //
    //                 singletonList(seniority),
    //
    //                 0, 0
    //
    //         ))).hasValueSatisfying(frame -> FrameAssert.assertThat(frame)
    //
    //                 .isIsomorphicTo(query(frame(employees), f -> Optional.of(f)
    //                         .filter(v -> v.integer(seniority)
    //                                 .filter(s -> s.compareTo(BigInteger.valueOf(3)) >= 0)
    //                                 .isPresent()
    //                         )
    //                         .map(v -> v.values(seniority))
    //                         .orElseGet(Stream::empty)
    //                 ))
    //
    //         ));
    //
    //     }
    //
    //     @Test void testRootFiltered() {
    //         exec(dataset(), () -> assertThat(relate(frame(employees), terms(
    //
    //                 and(filter(all(resource)), field(subordinate)),
    //
    //                 singletonList(subordinate),
    //
    //                 0, 0
    //
    //         ))).hasValueSatisfying(frame -> FrameAssert.assertThat(frame)
    //
    //                 .isIsomorphicTo(query(frame(employees), f -> Optional.of(f)
    //                         .filter(v -> v.focus().equals(resource))
    //                         .map(v -> v.values(subordinate))
    //                         .orElseGet(Stream::empty)
    //                 ))
    //
    //         ));
    //     }
    //
    //     @Test void testTraversingLink() {
    //         exec(dataset(), () -> assertThat(relate(frame(employees), terms(
    //
    //                 and(
    //                         filter(clazz(Alias)),
    //                         link(OWL.SAMEAS, field(supervisor))
    //                 ),
    //
    //                 singletonList(supervisor),
    //
    //                 0, 0
    //
    //         ))).hasValueSatisfying(frame -> FrameAssert.assertThat(frame)
    //
    //                 .isIsomorphicTo(query(frame(employees), v -> v.values(supervisor)))
    //
    //         ));
    //     }
    //
    //
    //     @Test void testReportUnknownSteps() {
    //         exec(() -> {
    //
    //             assertThatIllegalArgumentException().isThrownBy(() -> relate(frame(employees), terms(
    //                     field(office),
    //                     singletonList(unknown),
    //                     0, 0
    //             )));
    //
    //             assertThatIllegalArgumentException().isThrownBy(() -> relate(frame(employees), terms(
    //                     field(office),
    //                     asList(office, unknown),
    //                     0, 0
    //             )));
    //
    //         });
    //     }
    //
    //     @Test void testReportFilteringSteps() {
    //         exec(() -> assertThatIllegalArgumentException().isThrownBy(() -> relate(frame(employees), terms(
    //
    //                 and(
    //                         filter(field(office)),
    //                         field(seniority)
    //                 ),
    //
    //                 singletonList(office),
    //
    //                 0, 0
    //         ))));
    //     }
    //
    //
    //     @Test void testSlice() {
    //         exec(dataset(), () -> assertThat(relate(frame(employees), terms(
    //
    //                 EmployeeShape, singletonList(title), 1, 3
    //
    //         ))).hasValueSatisfying(frame -> FrameAssert.assertThat(frame)
    //
    //                 .isIsomorphicTo(query(frame(employees), f -> f.values(title), 1, 3))
    //
    //         ));
    //     }
    //
    //
    // }


    // @Nested final class RelateStats {
    //
    //     private Frame query(final Frame frame, final Function<Frame, Stream<Value>> mapper) {
    //
    //         final Predicate<Frame> filter=f -> f.value(RDF.TYPE).filter(Employee::equals).isPresent();
    //
    //         final Map<Value, Frame> index=resources.stream().filter(filter).collect(toMap(
    //                 Frame::focus, f -> frame(f.focus())
    //                         .values(RDFS.LABEL, f.values(RDFS.LABEL))
    //                         .values(RDFS.COMMENT, f.values(RDFS.COMMENT))
    //         ));
    //
    //         final IRI all=iri();
    //
    //         final Map<IRI, Long> count=new HashMap<>();
    //         final Map<IRI, Value> min=new HashMap<>();
    //         final Map<IRI, Value> max=new HashMap<>();
    //
    //         resources.stream().filter(filter).flatMap(mapper).distinct().sequential().forEach(value -> {
    //
    //             final IRI type=type(value);
    //
    //             count.compute(all, (t, c) -> c == null ? 1L : c+1);
    //             min.compute(all, (t, m) -> m == null ? value : compare(m, value) <= 0 ? m : value);
    //             max.compute(all, (t, m) -> m == null ? value : compare(m, value) >= 0 ? m : value);
    //
    //             count.compute(type, (t, c) -> c == null ? 1L : c+1);
    //             min.compute(type, (t, m) -> m == null ? value : compare(m, value) <= 0 ? m : value);
    //             max.compute(type, (t, m) -> m == null ? value : compare(m, value) >= 0 ? m : value);
    //
    //         });
    //
    //         final Function<Value, Frame> _annotate=value -> index.getOrDefault(value, frame(value));
    //
    //         final Function<IRI, Optional<Frame>> _min=type -> Optional.ofNullable(min.get(type)).map(_annotate);
    //         final Function<IRI, Optional<Frame>> _max=type -> Optional.ofNullable(max.get(type)).map(_annotate);
    //
    //         final Frame stats=frame(frame.focus())
    //
    //                 .integer(Engine.count, count.getOrDefault(all, 0L))
    //                 .frame(Engine.min, _min.apply(all))
    //                 .frame(Engine.max, _max.apply(all));
    //
    //         return count.entrySet().stream()
    //
    //                 .filter(entry -> !entry.getKey().equals(all))
    //                 .max(comparingByValue())
    //                 .map(Map.Entry::getKey)
    //
    //                 .map(type -> stats.frame(Engine.stats, frame(type)
    //
    //                         .integer(Engine.count, count.getOrDefault(type, 0L))
    //                         .frame(Engine.min, _min.apply(type))
    //                         .frame(Engine.max, _max.apply(type))
    //
    //                 ))
    //
    //                 .orElse(stats);
    //     }
    //
    //
    //     @Test void testEmptyResultSet() {
    //         exec(dataset(), () -> assertThat(relate(frame(employees), stats(
    //
    //                 field(RDF.TYPE, filter(all(RDF.NIL))), emptyList(), 0, 0
    //
    //         ))).hasValue(frame(employees)
    //                 .integer(Engine.count, 0)
    //         ));
    //     }
    //
    //
    //     @Test void testEmptyProjection() {
    //         exec(dataset(), () -> assertThat(relate(frame(employees), stats(
    //
    //                 filter(clazz(Employee)), emptyList(), 0, 0
    //
    //         ))).hasValueSatisfying(frame -> FrameAssert.assertThat(frame)
    //
    //                 .isIsomorphicTo(query(frame(employees), f -> Stream.of(f.focus())))
    //
    //         ));
    //     }
    //
    //     @Test void testFiltered() {
    //         exec(dataset(), () -> assertThat(relate(frame(employees), stats(
    //
    //                 and(
    //                         filter(clazz(Employee)),
    //                         filter(field(seniority, minInclusive(literal(3)))),
    //                         field(seniority)
    //                 ),
    //
    //                 singletonList(seniority),
    //
    //                 0, 0
    //
    //         ))).hasValueSatisfying(frame -> FrameAssert.assertThat(frame)
    //
    //                 .isIsomorphicTo(query(frame(employees), f -> Optional.of(f)
    //                         .filter(v -> v.integer(seniority)
    //                                 .filter(s -> s.compareTo(BigInteger.valueOf(3)) >= 0)
    //                                 .isPresent()
    //                         )
    //                         .map(v -> v.values(seniority))
    //                         .orElseGet(Stream::empty)
    //                 ))
    //
    //         ));
    //     }
    //
    //     @Test void testRootFiltered() {
    //         exec(dataset(), () -> assertThat(relate(frame(employees), stats(
    //
    //                 and(filter(all(resource)), field(subordinate)),
    //
    //                 singletonList(subordinate),
    //
    //                 0, 0
    //
    //         ))).hasValueSatisfying(frame -> FrameAssert.assertThat(frame)
    //
    //                 .isIsomorphicTo(query(frame(employees), f -> Optional.of(f)
    //                         .filter(v -> v.focus().equals(resource))
    //                         .map(v -> v.values(subordinate))
    //                         .orElseGet(Stream::empty)
    //                 ))
    //
    //         ));
    //     }
    //
    //     @Test void testTraversingLink() {
    //         exec(dataset(), () -> assertThat(relate(frame(employees), stats(
    //
    //                 and(
    //                         filter(clazz(Alias)),
    //                         link(OWL.SAMEAS, field(supervisor))
    //                 ),
    //
    //                 singletonList(supervisor),
    //
    //                 0, 0
    //
    //         ))).hasValueSatisfying(frame -> FrameAssert.assertThat(frame)
    //
    //                 .isIsomorphicTo(query(frame(employees), v -> v.values(supervisor)))
    //
    //         ));
    //     }
    //
    //
    //     @Test void testReportUnknownSteps() {
    //         exec(() -> {
    //
    //             assertThatIllegalArgumentException().isThrownBy(() -> relate(frame(employees), stats(
    //                     field(office),
    //                     singletonList(unknown),
    //                     0, 0
    //             )));
    //
    //             assertThatIllegalArgumentException().isThrownBy(() -> relate(frame(employees), stats(
    //                     field(office),
    //                     asList(office, unknown),
    //                     0, 0
    //             )));
    //
    //         });
    //     }
    //
    //     @Test void testReportFilteringSteps() {
    //         exec(() -> assertThatIllegalArgumentException().isThrownBy(() -> relate(frame(employees), stats(
    //
    //                 and(
    //                         filter(field(office)),
    //                         field(seniority)
    //                 ),
    //
    //                 singletonList(office),
    //
    //                 0, 0
    //         ))));
    //     }
    //
    // }
    //

}
