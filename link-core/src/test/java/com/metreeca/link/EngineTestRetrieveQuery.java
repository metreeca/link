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

public abstract class EngineTestRetrieveQuery {

    protected abstract Engine engine();


    // @Nested
    // final class RelateItems {
    //
    //     @Test void testRelateResource() {
    //         exec(dataset(), () -> assertThat(relate(frame(resource), items(
    //
    //                 EmployeeShape
    //
    //         ))).hasValue(resources.stream()
    //                 .filter(frame -> frame.focus().equals(resource))
    //                 .findFirst()
    //                 .orElse(null)
    //         ));
    //     }
    //
    //     @Test void testRelateContainer() {
    //         exec(dataset(), () -> assertThat(relate(frame(employees), items(
    //
    //                 EmployeeShape
    //
    //         ))).hasValue(frame(employees).frames(Contains, resources.stream()
    //                 .filter(frame -> frame.value(RDF.TYPE).filter(Employee::equals).isPresent())
    //         )));
    //     }
    //
    //
    //     @Test void testReportUnknown() {
    //         exec(() -> assertThat(relate(frame(unknown), items(EmployeeShape))).isEmpty());
    //     }
    //
    //
    //     @Test void testEmptyShape() {
    //         exec(dataset(), () -> assertThat(relate(frame(resource), items(
    //
    //                 and()
    //
    //         ))).isEmpty());
    //     }
    //
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
    //
    //
    //     @Test void testFilter() {
    //         exec(dataset(), () -> assertThat(relate(frame(employees), items(
    //
    //                 and(EmployeeShape, filter(field(title, all(literal("Sales Rep")))))
    //
    //         ))).hasValue(frame(employees).frames(Contains, resources.stream()
    //                 .filter(frame -> frame.string(title).filter("Sales Rep"::equals).isPresent())
    //         )));
    //     }
    //
    //
    //     @Test void testSortingDefault() {
    //         exec(dataset(), () -> Assertions.assertThat(relate(frame(employees), items(EmployeeShape)).map(frame ->
    //
    //                 frame.frames(Contains).map(Frame::focus).collect(toList())
    //
    //         )).hasValue(resources.stream()
    //                 .filter(frame -> frame.value(RDF.TYPE).filter(Employee::equals).isPresent())
    //                 .sorted(comparing(Frame::focus, Values::compare))
    //                 .map(Frame::focus)
    //                 .collect(toList())
    //         ));
    //     }
    //
    //     @Test void testSortingCustomOnResource() {
    //         exec(dataset(), () -> Assertions.assertThat(relate(frame(employees), items(
    //
    //                 EmployeeShape, singletonList(decreasing())
    //
    //         )).map(frame ->
    //
    //                 frame.frames(Contains).map(Frame::focus).collect(toList())
    //
    //         )).hasValue(resources.stream()
    //                 .filter(frame -> frame.value(RDF.TYPE).filter(Employee::equals).isPresent())
    //                 .sorted(comparing(Frame::focus, Values::compare).reversed())
    //                 .map(Frame::focus)
    //                 .collect(toList())
    //         ));
    //     }
    //
    //     @Test void testSortingCustomIncreasing() {
    //         exec(dataset(), () -> Assertions.assertThat(relate(frame(employees), items(
    //
    //                 EmployeeShape, singletonList(increasing(RDFS.LABEL))
    //
    //         )).map(frame ->
    //
    //                 frame.frames(Contains).map(Frame::focus).collect(toList())
    //
    //         )).hasValue(resources.stream()
    //                 .filter(frame -> frame.value(RDF.TYPE).filter(Employee::equals).isPresent())
    //                 .sorted(comparing(frame -> frame.string(RDFS.LABEL).orElse("")))
    //                 .map(Frame::focus)
    //                 .collect(toList())
    //         ));
    //     }
    //
    //     @Test void testSortingCustomDecreasing() {
    //         exec(dataset(), () -> Assertions.assertThat(relate(frame(employees), items(
    //
    //                 EmployeeShape, singletonList(decreasing(RDFS.LABEL))
    //
    //         )).map(frame ->
    //
    //                 frame.frames(Contains).map(Frame::focus).collect(toList())
    //
    //         )).hasValue(resources.stream()
    //                 .filter(frame -> frame.value(RDF.TYPE).filter(Employee::equals).isPresent())
    //                 .sorted(Comparator.<Frame, String>comparing(frame -> frame.string(RDFS.LABEL).orElse("")).reversed())
    //                 .map(Frame::focus)
    //                 .collect(toList())
    //         ));
    //     }
    //
    //     @Test void testSortingCustomMultiple() {
    //         exec(dataset(), () -> Assertions.assertThat(relate(frame(employees), items(
    //
    //                 EmployeeShape, asList(increasing(office), increasing(RDFS.LABEL))
    //
    //         )).map(frame ->
    //
    //                 frame.frames(Contains).map(Frame::focus).collect(toList())
    //
    //         )).hasValue(resources.stream()
    //                 .filter(frame -> frame.value(RDF.TYPE).filter(Employee::equals).isPresent())
    //                 .sorted(Comparator
    //                         .<Frame, Value>comparing(frame -> frame.value(office).orElse(null), Values::compare)
    //                         .thenComparing(frame -> frame.string(RDFS.LABEL).orElse(""))
    //                 )
    //                 .map(Frame::focus)
    //                 .collect(toList())
    //         ));
    //     }
    //
    //     @Test void testSortingWithLinks() {
    //         exec(dataset(), () -> Assertions.assertThat(relate(frame(employees), items(
    //
    //                 and(filter(clazz(Alias)), link(OWL.SAMEAS, field(code))), singletonList(decreasing(code))
    //
    //         )).map(frame ->
    //
    //                 frame.frames(Contains).map(f -> frame(f.focus())).collect(toList())
    //
    //         )).hasValue(resources.stream()
    //                 .filter(frame -> frame.value(RDF.TYPE).filter(Alias::equals).isPresent())
    //                 .sorted(comparing(Frame::focus, Values::compare).reversed())
    //                 .map(frame -> frame(frame.focus()))
    //                 .collect(toList())
    //         ));
    //     }
    //
    // }
    //
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
    //
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
    // ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // @Nested final class RelateFilters {
    //
    //     @Test void testHandlePatternFilters() {
    //         exec(dataset(), () -> assertThat(relate(frame(employees), items(and(
    //
    //                 filter(clazz(Employee)),
    //
    //                 field(RDFS.LABEL, convey(localized("en")))
    //
    //         )))).hasValue(frame(employees).frames(Contains, resources.stream()
    //                 .filter(frame -> frame.values(RDF.TYPE).anyMatch(Employee::equals))
    //                 .map(frame -> frame(frame.focus())
    //                         .values(RDFS.LABEL, frame.values(RDFS.LABEL)
    //                                 .filter(value -> lang(value).equals("en"))
    //                         )
    //                 )
    //         )));
    //     }
    //
    //     @Test void testUseIndependentPatternsAndFilters() {
    //         exec(dataset(), () -> assertThat(relate(frame(employees), items(and(
    //
    //                 filter(field(subordinate, any(
    //                         iri(employees, "1002"),
    //                         iri(employees, "1188")
    //                 ))),
    //
    //                 field(subordinate)
    //
    //         )))).hasValue(frame(employees).frames(Contains, resources.stream()
    //                 .filter(frame -> frame.values(subordinate).anyMatch(value ->
    //                         value.equals(iri(employees, "1002")) || value.equals(iri(employees, "1188"))
    //                 ))
    //                 .map(frame -> frame(frame.focus())
    //                         .values(subordinate, frame.values(subordinate))
    //                 )
    //         )));
    //     }
    //
    //     @Test void testIgnoreNonConveyPatternFilters() {
    //         exec(dataset(), () -> assertThat(relate(frame(employees), items(and(
    //
    //                 filter(clazz(Employee)),
    //                 field(seniority, minInclusive(literal(10)))
    //
    //         )))).hasValue(frame(employees).frames(Contains, resources.stream()
    //                 .filter(frame -> frame.values(RDF.TYPE).anyMatch(Employee::equals))
    //                 .map(frame -> frame(frame.focus())
    //                         .values(seniority, frame.values(seniority))
    //                 )
    //         )));
    //     }
    //
    // }
    //
    // @Nested final class RelateAnchors {
    //
    //     @Test void testAnchorBasicContainer() {
    //         exec(dataset(), () -> assertThat(relate(frame(container), items(
    //
    //                 convey(field(RDFS.LABEL))
    //
    //         ))).hasValue(frame(container).frames(Contains, resources.stream()
    //                 .filter(frame -> frame.values(RDF.TYPE).anyMatch(Employee::equals))
    //                 .map(frame -> frame(frame.focus())
    //                         .values(RDFS.LABEL, frame.values(RDFS.LABEL))
    //                 )
    //         )));
    //     }
    //
    //     @Test void testResolveReferencesToTarget() {
    //         exec(dataset(), () -> assertThat(relate(frame(container), items(and(
    //
    //                 filter(field(inverse(LDP.CONTAINS), all(focus()))),
    //                 convey(field(RDFS.LABEL))
    //
    //         )))).hasValue(frame(container).frames(Contains, resources.stream()
    //                 .filter(frame -> frame.values(RDF.TYPE).anyMatch(Employee::equals))
    //                 .map(frame -> frame(frame.focus())
    //                         .values(RDFS.LABEL, frame.values(RDFS.LABEL))
    //                 )
    //         )));
    //     }
    //
    // }
    //
    // @Nested final class RelateValueConstraints {
    //
    //     @Test void testDatatype() {
    //         exec(dataset(), () -> {
    //
    //             assertThat(relate(frame(employees), items(
    //
    //                     field(code, filter(datatype(XSD.INTEGER)))
    //
    //             ))).isEmpty();
    //
    //             assertThat(relate(frame(employees), items(
    //
    //                     field(code, filter(datatype(XSD.STRING)))
    //
    //             ))).hasValue(frame(employees).frames(Contains, resources.stream()
    //                     .filter(frame -> frame.values(RDF.TYPE).anyMatch(Employee::equals))
    //                     .filter(frame -> frame.values(code).anyMatch(value -> type(value).equals(XSD.STRING)))
    //                     .map(frame -> frame(frame.focus())
    //                             .values(code, frame.values(code))
    //                     )
    //             ));
    //
    //         });
    //     }
    //
    //     @Test void testClazz() {
    //         exec(dataset(), () -> assertThat(relate(frame(employees), items(and(
    //
    //                 field(RDF.TYPE), filter(clazz(Employee))
    //
    //         )))).hasValue(frame(employees).frames(Contains, resources.stream()
    //                 .filter(frame -> frame.values(RDF.TYPE).anyMatch(Employee::equals))
    //                 .map(frame -> frame(frame.focus())
    //                         .values(RDF.TYPE, frame.values(RDF.TYPE))
    //                 ))
    //         ));
    //     }
    //
    //
    //     @Test void testMinExclusiveConstraint() {
    //         exec(dataset(), () -> assertThat(relate(frame(employees), items(
    //
    //                 field(seniority, filter(minExclusive(literal(3)))))
    //
    //         )).hasValue(frame(employees).frames(Contains, resources.stream()
    //                 .filter(frame -> frame.values(RDF.TYPE).anyMatch(Employee::equals))
    //                 .filter(frame -> frame.integers(seniority).anyMatch(s -> s.compareTo(BigInteger.valueOf(3)) > 0))
    //                 .map(frame -> frame(frame.focus())
    //                         .values(seniority, frame.values(seniority))
    //                 ))
    //         ));
    //     }
    //
    //     @Test void testMaxExclusiveConstraint() {
    //         exec(dataset(), () -> assertThat(relate(frame(employees), items(
    //
    //                 field(seniority, filter(maxExclusive(literal(3)))))
    //
    //         )).hasValue(frame(employees).frames(Contains, resources.stream()
    //                 .filter(frame -> frame.values(RDF.TYPE).anyMatch(Employee::equals))
    //                 .filter(frame -> frame.integers(seniority).anyMatch(s -> s.compareTo(BigInteger.valueOf(3)) < 0))
    //                 .map(frame -> frame(frame.focus())
    //                         .values(seniority, frame.values(seniority))
    //                 ))
    //         ));
    //     }
    //
    //     @Test void testMinInclusiveConstraint() {
    //         exec(dataset(), () -> assertThat(relate(frame(employees), items(
    //
    //                 field(seniority, filter(minInclusive(literal(3)))))
    //
    //         )).hasValue(frame(employees).frames(Contains, resources.stream()
    //                 .filter(frame -> frame.values(RDF.TYPE).anyMatch(Employee::equals))
    //                 .filter(frame -> frame.integers(seniority).anyMatch(s -> s.compareTo(BigInteger.valueOf(3)) >= 0))
    //                 .map(frame -> frame(frame.focus())
    //                         .values(seniority, frame.values(seniority))
    //                 ))
    //         ));
    //     }
    //
    //     @Test void testMaxInclusiveConstraint() {
    //         exec(dataset(), () -> assertThat(relate(frame(employees), items(
    //
    //                 field(seniority, filter(maxInclusive(literal(3)))))
    //
    //         )).hasValue(frame(employees).frames(Contains, resources.stream()
    //                 .filter(frame -> frame.values(RDF.TYPE).anyMatch(Employee::equals))
    //                 .filter(frame -> frame.integers(seniority).anyMatch(s -> s.compareTo(BigInteger.valueOf(3)) <= 0))
    //                 .map(frame -> frame(frame.focus())
    //                         .values(seniority, frame.values(seniority))
    //                 ))
    //         ));
    //     }
    //
    //
    //     @Test void testMinLength() {
    //         exec(dataset(), () -> assertThat(relate(frame(employees), items(
    //
    //                 field(forename, filter(minLength(5)))
    //
    //         ))).hasValue(frame(employees).frames(Contains, resources.stream()
    //                 .filter(frame -> frame.values(RDF.TYPE).anyMatch(Employee::equals))
    //                 .filter(frame -> frame.strings(forename).anyMatch(s -> s.length() >= 5))
    //                 .map(frame -> frame(frame.focus())
    //                         .values(forename, frame.values(forename))
    //                 ))
    //         ));
    //     }
    //
    //     @Test void testMaxLength() {
    //         exec(dataset(), () -> assertThat(relate(frame(employees), items(
    //
    //                 field(forename, filter(maxLength(5)))
    //
    //         ))).hasValue(frame(employees).frames(Contains, resources.stream()
    //                 .filter(frame -> frame.values(RDF.TYPE).anyMatch(Employee::equals))
    //                 .filter(frame -> frame.strings(forename).anyMatch(s -> s.length() <= 5))
    //                 .map(frame -> frame(frame.focus())
    //                         .values(forename, frame.values(forename))
    //                 ))
    //         ));
    //     }
    //
    //     @Test void testPattern() {
    //         exec(dataset(), () -> assertThat(relate(frame(employees), items(
    //
    //                 field(forename, filter(pattern("\\bgerard\\b", "i")))
    //
    //         ))).hasValue(frame(employees).frames(Contains, resources.stream()
    //                 .filter(frame -> frame.values(RDF.TYPE).anyMatch(Employee::equals))
    //                 .filter(frame -> frame.strings(forename).anyMatch(s -> s.matches("(?i:\\bgerard\\b)")))
    //                 .map(frame -> frame(frame.focus())
    //                         .values(forename, frame.values(forename))
    //                 ))
    //         ));
    //     }
    //
    //     @Test void testLike() {
    //         exec(dataset(), () -> assertThat(relate(frame(employees), items(
    //
    //                 field(RDFS.LABEL, filter(like("ger bo", true)))
    //
    //         ))).hasValue(frame(employees).frames(Contains, resources.stream()
    //                 .filter(frame -> frame.values(RDF.TYPE).anyMatch(Employee::equals))
    //                 .filter(frame -> frame.strings(RDFS.LABEL).anyMatch(s -> s.equals("Gerard Bondur")))
    //                 .map(frame -> frame(frame.focus())
    //                         .values(RDFS.LABEL, frame.values(RDFS.LABEL))
    //                 ))
    //         ));
    //     }
    //
    //     @Test void testStem() {
    //         exec(dataset(), () -> assertThat(relate(frame(employees), items(
    //
    //                 field(RDFS.LABEL, filter(stem("Gerard B")))
    //
    //         ))).hasValue(frame(employees).frames(Contains, resources.stream()
    //                 .filter(frame -> frame.values(RDF.TYPE).anyMatch(Employee::equals))
    //                 .filter(frame -> frame.strings(RDFS.LABEL).anyMatch(s -> s.equals("Gerard Bondur")))
    //                 .map(frame -> frame(frame.focus())
    //                         .values(RDFS.LABEL, frame.values(RDFS.LABEL))
    //                 ))
    //         ));
    //     }
    //
    // }
    //
    // @Nested final class RelateSetConstraints {
    //
    //     @Test void testAll() {
    //         exec(dataset(), () -> assertThat(relate(frame(employees), items(
    //
    //                 field(subordinate, filter(all(
    //                         iri(employees, "1088"),
    //                         iri(employees, "1102")
    //                 )))
    //
    //         ))).hasValue(frame(employees).frames(Contains, resources.stream()
    //                 .filter(frame -> frame.values(RDF.TYPE).anyMatch(Employee::equals))
    //                 .filter(frame -> frame.values(subordinate).collect(toSet()).containsAll(asList(
    //                         iri(employees, "1088"),
    //                         iri(employees, "1102")
    //                 )))
    //                 .map(frame -> frame(frame.focus())
    //                         .values(subordinate, frame.values(subordinate))
    //                 ))
    //         ));
    //     }
    //
    //     @Test void testAllSingleton() {
    //         exec(dataset(), () -> assertThat(relate(frame(employees), items(
    //
    //                 field(subordinate, filter(all(iri(employees, "1102"))))
    //
    //         ))).hasValue(frame(employees).frames(Contains, resources.stream()
    //                 .filter(frame -> frame.values(RDF.TYPE).anyMatch(Employee::equals))
    //                 .filter(frame -> frame.values(subordinate).collect(toSet()).contains(iri(employees, "1102")))
    //                 .map(frame -> frame(frame.focus())
    //                         .values(subordinate, frame.values(subordinate))
    //                 ))
    //         ));
    //     }
    //
    //     @Test void testAllRoot() {
    //         exec(dataset(), () -> assertThat(relate(frame(employees), items(and(
    //
    //                 filter(all(iri(employees, "1088"), iri(employees, "1102"))),
    //                 field(RDF.TYPE)
    //
    //         )))).hasValue(frame(employees).frames(Contains, resources.stream()
    //                 .filter(frame -> frame.values(RDF.TYPE).anyMatch(Employee::equals))
    //                 .filter(frame -> frame.focus().equals(iri(employees, "1088"))
    //                         || frame.focus().equals(iri(employees, "1102"))
    //                 )
    //                 .map(frame -> frame(frame.focus())
    //                         .values(RDF.TYPE, frame.values(RDF.TYPE))
    //                 ))
    //         ));
    //     }
    //
    //
    //     @Test void testAny() {
    //         exec(dataset(), () -> assertThat(relate(frame(employees), items(
    //
    //                 field(subordinate, filter(any(
    //                         iri(employees, "1056"),
    //                         iri(employees, "1088")
    //                 )))
    //
    //         ))).hasValue(frame(employees).frames(Contains, resources.stream()
    //                 .filter(frame -> frame.values(RDF.TYPE).anyMatch(Employee::equals))
    //                 .filter(frame -> frame.values(subordinate).anyMatch(value ->
    //                         value.equals(iri(employees, "1056")) || value.equals(iri(employees, "1088"))
    //                 ))
    //                 .map(frame -> frame(frame.focus())
    //                         .values(subordinate, frame.values(subordinate))
    //                 ))
    //         ));
    //     }
    //
    //     @Test void testAnySingleton() {
    //         exec(dataset(), () -> assertThat(relate(frame(employees), items(
    //
    //                 field(subordinate, filter(any(
    //                         iri(employees, "1088")
    //                 )))
    //
    //         ))).hasValue(frame(employees).frames(Contains, resources.stream()
    //                 .filter(frame -> frame.values(RDF.TYPE).anyMatch(Employee::equals))
    //                 .filter(frame -> frame.values(subordinate).anyMatch(value ->
    //                         value.equals(iri(employees, "1088"))
    //                 ))
    //                 .map(frame -> frame(frame.focus())
    //                         .values(subordinate, frame.values(subordinate))
    //                 ))
    //         ));
    //     }
    //
    //     @Test void testAnyRoot() {
    //         exec(dataset(), () -> assertThat(relate(frame(employees), items(and(
    //
    //                 filter(any(iri(employees, "1002"), iri(employees, "1056"))),
    //
    //                 field(RDFS.LABEL)
    //
    //         )))).hasValue(frame(employees).frames(Contains, resources.stream()
    //                 .filter(frame -> frame.values(RDF.TYPE).anyMatch(Employee::equals))
    //                 .filter(frame -> frame.focus().equals(iri(employees, "1002"))
    //                         || frame.focus().equals(iri(employees, "1056"))
    //                 )
    //                 .map(frame -> frame(frame.focus())
    //                         .values(RDFS.LABEL, frame.values(RDFS.LABEL))
    //                 ))
    //         ));
    //     }
    //
    // }
    //
    // @Nested final class RelateStructuralConstraints {
    //
    //     @Test void testField() {
    //         exec(dataset(), () -> assertThat(relate(frame(employees), items(and(
    //
    //                 filter(field(code)),
    //                 field(code)
    //
    //         )))).hasValue(frame(employees).frames(Contains, resources.stream()
    //                 .filter(frame -> frame.value(code).isPresent())
    //                 .map(frame -> frame(frame.focus())
    //                         .values(code, frame.values(code))
    //                 ))
    //         ));
    //     }
    //
    //
    //     @Test void testLinkClassFilters() {
    //         exec(dataset(), () -> assertThat(relate(frame(aliases), items(
    //
    //                 filter(link(OWL.SAMEAS, clazz(Employee)))
    //
    //         ))).hasValue(frame(aliases).frames(Contains, resources.stream()
    //                 .filter(frame -> frame.value(RDF.TYPE).filter(Set.<Value>of(Alias, Employee)::contains).isPresent())
    //                 .map(frame -> frame(frame.focus()))
    //         )));
    //     }
    //
    //     @Test void testLinkFieldFilters() {
    //         exec(dataset(), () -> assertThat(relate(frame(aliases), items(
    //
    //                 link(OWL.SAMEAS, filter(field(RDF.TYPE, all(Employee))))
    //
    //         ))).hasValue(frame(aliases).frames(Contains, resources.stream()
    //                 .filter(frame -> frame.value(RDF.TYPE).filter(Set.<Value>of(Alias, Employee)::contains).isPresent())
    //                 .map(frame -> frame(frame.focus()))
    //         )));
    //     }
    //
    //     @Test void testLinkFieldPatterns() {
    //         exec(dataset(), () -> assertThat(relate(frame(employees), items(and(
    //
    //                 filter(clazz(Alias)),
    //
    //                 link(OWL.SAMEAS, field(RDFS.LABEL))
    //
    //         )))).hasValue(frame(employees).frames(Contains, resources.stream()
    //                 .filter(frame -> frame.value(RDF.TYPE).filter(Alias::equals).isPresent())
    //                 .map(frame -> frame(frame.focus())
    //                         .values(RDFS.LABEL, frame.values(OWL.SAMEAS).map(index::get)
    //                                 .flatMap(f -> f.values(RDFS.LABEL))
    //                         )
    //                 )
    //         )));
    //     }
    //
    //     @Test void testLinkFilteringOnLinkedField() {
    //         exec(dataset(), () -> assertThat(relate(frame(employees), items(and(
    //
    //                 filter(clazz(Alias)),
    //
    //                 link(OWL.SAMEAS, field(RDFS.LABEL)),
    //
    //                 filter(link(OWL.SAMEAS, field(RDFS.LABEL, like("Gerard"))))
    //
    //
    //         )))).hasValue(frame(employees).frames(Contains, resources.stream()
    //                 .filter(frame -> frame.value(RDF.TYPE).filter(Alias::equals).isPresent())
    //                 .filter(frame -> frame.values(OWL.SAMEAS).map(index::get)
    //                         .anyMatch(value -> value.strings(RDFS.LABEL).anyMatch(label -> label.contains("Gerard")))
    //                 )
    //                 .map(frame -> frame(frame.focus())
    //                         .values(RDFS.LABEL, frame.values(OWL.SAMEAS).map(index::get)
    //                                 .flatMap(f -> f.values(RDFS.LABEL))
    //                         )
    //                 )
    //         )));
    //     }
    //
    // }
    //
    // @Nested final class RelateLogicalConstraints {
    //
    //     private Shape always(final Shape... shapes) { return mode(Filter, Convey).then(shapes); }
    //
    //
    //     @Test void testGuard() { // reject partially redacted shapes
    //         exec(dataset(),
    //                 () -> Assertions.assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() ->
    //                         relate(frame(employees), items(guard("axis", RDF.NIL)))
    //                 ));
    //     }
    //
    //     @Test void testWhen() { // reject conditional shapes
    //         exec(dataset(),
    //                 () -> Assertions.assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> {
    //                     relate(frame(employees), items(when(guard("axis", RDF.NIL), field(RDF.VALUE))));
    //                 }));
    //     }
    //
    //     @Test void testAnd() {
    //         exec(dataset(), () -> assertThat(relate(frame(employees), items(and(
    //
    //                 always(field(code)),
    //                 always(field(office))
    //
    //         )))).hasValue(frame(employees).frames(Contains, resources.stream()
    //                 .filter(frame -> frame.values(RDF.TYPE).anyMatch(Employee::equals))
    //                 .map(frame -> frame(frame.focus())
    //                         .values(code, frame.values(code))
    //                         .values(office, frame.values(office))
    //                 )
    //         )));
    //     }
    //
    //     @Test void testOr() {
    //         exec(dataset(), () -> assertThat(relate(frame(employees), items(or(
    //
    //                 always(field(code)),
    //                 always(field(OWL.SAMEAS))
    //
    //         )))).hasValue(frame(employees).frames(Contains, resources.stream()
    //                 .filter(frame -> frame.values(RDF.TYPE).anyMatch(type -> type.equals(Employee) || type.equals(Alias)))
    //                 .map(frame -> frame(frame.focus())
    //                         .values(code, frame.values(code))
    //                         .values(OWL.SAMEAS, frame.values(OWL.SAMEAS))
    //                 )
    //         )));
    //     }
    //
    // }

}
