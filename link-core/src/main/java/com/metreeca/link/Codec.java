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

import java.io.IOException;

/**
 * Wire format codec.
 */
public interface Codec {

    public Frame decode(final Readable source, final Shape shape) throws IOException, CodecException;


    /**
     * <p><strong>Warning</strong> / Codecs are not required to perform {@linkplain Shape#validate(Frame) validation}:
     * {@code frame} is expected to be consistent with the provided {@code shape}.</p>
     *
     * @param target
     * @param shape
     * @param frame
     * @param <A>
     *
     * @return
     *
     * @throws IOException
     */
    public <A extends Appendable> A encode(final A target, final Shape shape, final Frame frame) throws IOException;

    public <A extends Appendable> A encode(final A target, final Shape shape, final Trace trace) throws IOException;

}
