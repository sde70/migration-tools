/**
 * Copyright (c) 2012, NuoDB, Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of NuoDB, Inc. nor the names of its contributors may
 *       be used to endorse or promote products derived from this software
 *       without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL NUODB, INC. BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.nuodb.migration.jdbc.url;

import java.util.Map;

import static org.apache.commons.lang3.StringUtils.*;

/**
 * @author Sergey Bushik
 */
public class MySQLJdbcUrlParser implements JdbcUrlParser {

    @Override
    public boolean canParse(String url) {
        return startsWith(url, "jdbc:mysql");
    }

    @Override
    public JdbcUrl parse(String url, Map<String, Object> overrides) {
        return new MySQLJdbcUrl(url, overrides);
    }

    class MySQLJdbcUrl extends JdbcUrlBase {

        private final String catalog;

        public MySQLJdbcUrl(String url, Map<String, Object> overrides) {
            super(url);
            int prefix = url.indexOf("://");
            int parameters = 0;
            if (prefix > 0 && (parameters = url.indexOf('?', prefix + 3)) > 0) {
                parseParameters(getProperties(), url, parameters);
            }
            String base = parameters > 0 ? url.substring(0, parameters) : url;
            catalog = substringAfterLast(base, "/");
            if (overrides != null) {
                getProperties().putAll(overrides);
            }
        }

        @Override
        public String getCatalog() {
            return catalog;
        }

        @Override
        public String getSchema() {
            return null;
        }
    }
}