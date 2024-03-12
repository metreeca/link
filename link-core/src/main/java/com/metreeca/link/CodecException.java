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

import static java.lang.String.format;

public final class CodecException extends RuntimeException {

    private static final long serialVersionUID=-1267685327499864471L;


    public CodecException(final String message) {
        super(message);
    }

    public CodecException(final String message, final int line, final int col) {
        super(format("(%d,%d) %s", line, col, message));
    }

}