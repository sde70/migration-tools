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
package com.nuodb.migration.jdbc.dialect;

import com.nuodb.migration.jdbc.dialect.mysql.MySQLTypeRegistry;
import com.nuodb.migration.jdbc.type.JdbcTypeRegistry;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.TimeZone;

/**
 * @author Sergey Bushik
 */
public class MySQLDialect extends StandardDialect {

    public MySQLDialect(DatabaseMetaData metaData) {
        super(metaData);
    }

    @Override
    public char openQuote() {
        return '`';
    }

    @Override
    public char closeQuote() {
        return '`';
    }

    @Override
    public boolean supportsSessionTimeZone() {
        return true;
    }

    @Override
    public void setSessionTimeZone(Connection connection, TimeZone timeZone) throws SQLException {
        Statement statement = connection.createStatement();
        try {
            String timeZoneAsValue = timeZone != null ? timeZoneAsValue(timeZone) : "SYSTEM";
            statement.execute("SET @@session.time_zone = '" + timeZoneAsValue + "'");
        } finally {
            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException exception) {
                    if (log.isErrorEnabled()) {
                        log.error("Failed closing statement", exception);
                    }
                }
            }
        }
    }

    protected String timeZoneAsValue(TimeZone timeZone) {
        int rawOffset = timeZone.getRawOffset();
        int dstSavings = timeZone.getDSTSavings();
        int absOffset = Math.abs(rawOffset + dstSavings);
        String zeros = "00";
        String hoursOffset = Integer.toString(absOffset / 3600000);
        String minutesOffset = Integer.toString((absOffset % 3600000) / 60000);

        StringBuilder value = new StringBuilder(6);
        value.append(rawOffset >= 0 ? '+' : '-');
        value.append(zeros.substring(0, zeros.length() - hoursOffset.length()));
        value.append(hoursOffset);
        value.append(':');
        value.append(zeros.substring(0, zeros.length() - minutesOffset.length()));
        value.append(minutesOffset);
        return value.toString();
    }

    /**
     * Forces driver to stream ResultSet http://goo.gl/kl1Nr
     *
     * @param statement to stream ResultSet
     * @throws SQLException
     */
    @Override
    public void enableStreaming(Statement statement) throws SQLException {
        statement.setFetchSize(Integer.MIN_VALUE);
    }

    @Override
    public JdbcTypeRegistry getJdbcTypeRegistry() {
        return MySQLTypeRegistry.INSTANCE;
    }
}