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

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.time.*;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static com.metreeca.link.EngineTest.id;
import static com.metreeca.link.Frame.with;
import static com.metreeca.link.Local.local;
import static com.metreeca.link.Stash.decimal;
import static com.metreeca.link.Stash.integer;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class EngineTestXcode {

    protected abstract Engine engine();


    private <T> void xcode(
            final T model, final T value,
            final Function<Data, T> getter, final BiConsumer<Data, T> setter
    ) {

        final Engine engine=engine();

        engine.create(with(new Data(), data -> {

            data.setId(id("/data"));

            setter.accept(data, value);

        }));

        assertThat(engine.retrieve(with(new Data(), data -> {

            data.setId(id("/data"));

            setter.accept(data, model);

        }))).hasValueSatisfying(data -> assertThat(getter.apply(data))
                .isEqualTo(value)
        );
    }


    @Test void testBoolean() {
        xcode(false, true, Data::isBoolean_, Data::setBoolean_);
    }


    @Test void testInteger() {
        xcode(integer(0), integer(123), Data::getInteger, Data::setInteger);
    }

    @Test void testDecimal() {
        xcode(decimal(0), decimal(1.123), Data::getDecimal, Data::setDecimal);
    }


    @Test void testString() {
        xcode("", "string", Data::getString, Data::setString);
    }


    @Test void testYear() {
        xcode(Year.of(Year.MAX_VALUE), Year.now(), Data::getYear, Data::setYear);
    }

    @Test void testLocalDate() {
        xcode(LocalDate.MIN, LocalDate.now(), Data::getLocalDate, Data::setLocalDate);
    }

    @Test void testLocalTime() {
        xcode(LocalTime.MIN, LocalTime.now(), Data::getLocalTime, Data::setLocalTime);
    }

    @Test void testOffsetTime() {
        xcode(OffsetTime.MIN, OffsetTime.now(), Data::getOffsetTime, Data::setOffsetTime);
    }

    @Test void testLocalDateTime() {
        xcode(LocalDateTime.MIN, LocalDateTime.now(), Data::getLocalDateTime, Data::setLocalDateTime);
    }

    @Test void testOffsetDateTime() {
        xcode(OffsetDateTime.MIN, OffsetDateTime.now(), Data::getOffsetDateTime, Data::setOffsetDateTime);
    }

    @Test void testInstant() {
        xcode(Instant.MIN, Instant.now(), Data::getInstant, Data::setInstant);
    }

    @Test void testPeriod() {
        xcode(Period.ofDays(0), Period.of(1, 2, 3), Data::getPeriod, Data::setPeriod);
    }

    @Test void testDuration() {
        xcode(Duration.ofSeconds(0), Duration.ofSeconds(123), Data::getDuration, Data::setDuration);
    }


    @Test void testURI() {
        xcode(URI.create(""), URI.create("https://example.com/"), Data::getUri, Data::setUri);
    }


    @Test void testLocal() {
        xcode(
                local("*", ""),
                local(local("en", "one"), local("it", "uno")),
                Data::getLocal, Data::setLocal
        );
    }

    @Test void testLocals() {
        xcode(
                local("*", Set.of()),
                local(local("en", "one", "two"), local("it", "uno", "due")),
                Data::getLocals, Data::setLocals
        );
    }


    public static final class Data extends EngineTest.Resource {

        private Boolean boolean_;

        private BigInteger integer;
        private BigDecimal decimal;

        private Year year;
        private LocalDate localDate;
        private LocalTime localTime;
        private OffsetTime offsetTime;
        private LocalDateTime localDateTime;
        private OffsetDateTime offsetDateTime;
        private Instant instant;
        private Period period;
        private Duration duration;

        private URI uri;
        private String string;

        private Local<String> local;
        private Local<Set<String>> locals;


        public Boolean isBoolean_() {
            return boolean_;
        }

        public void setBoolean_(final Boolean boolean_) {
            this.boolean_=boolean_;
        }


        public BigInteger getInteger() {
            return integer;
        }

        public void setInteger(final BigInteger integer) {
            this.integer=integer;
        }

        public BigDecimal getDecimal() {
            return decimal;
        }

        public void setDecimal(final BigDecimal decimal) {
            this.decimal=decimal;
        }


        public String getString() {
            return string;
        }

        public void setString(final String string) {
            this.string=string;
        }


        public Year getYear() {
            return year;
        }

        public void setYear(final Year year) {
            this.year=year;
        }

        public LocalDate getLocalDate() {
            return localDate;
        }

        public void setLocalDate(final LocalDate localDate) {
            this.localDate=localDate;
        }

        public LocalTime getLocalTime() {
            return localTime;
        }

        public void setLocalTime(final LocalTime localTime) {
            this.localTime=localTime;
        }

        public OffsetTime getOffsetTime() {
            return offsetTime;
        }

        public void setOffsetTime(final OffsetTime offsetTime) {
            this.offsetTime=offsetTime;
        }

        public LocalDateTime getLocalDateTime() {
            return localDateTime;
        }

        public void setLocalDateTime(final LocalDateTime localDateTime) {
            this.localDateTime=localDateTime;
        }

        public OffsetDateTime getOffsetDateTime() {
            return offsetDateTime;
        }

        public void setOffsetDateTime(final OffsetDateTime offsetDateTime) {
            this.offsetDateTime=offsetDateTime;
        }

        public Instant getInstant() {
            return instant;
        }

        public void setInstant(final Instant instant) {
            this.instant=instant;
        }

        public Period getPeriod() {
            return period;
        }

        public void setPeriod(final Period period) {
            this.period=period;
        }

        public Duration getDuration() {
            return duration;
        }

        public void setDuration(final Duration duration) {
            this.duration=duration;
        }


        public URI getUri() {
            return uri;
        }

        public void setUri(final URI uri) {
            this.uri=uri;
        }


        public Local<String> getLocal() {
            return local;
        }

        public void setLocal(final Local<String> local) {
            this.local=local;
        }

        public Local<Set<String>> getLocals() {
            return locals;
        }

        public void setLocals(final Local<Set<String>> locals) {
            this.locals=locals;
        }

    }

}
