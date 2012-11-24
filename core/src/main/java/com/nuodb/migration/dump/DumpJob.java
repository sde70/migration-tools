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
package com.nuodb.migration.dump;

import com.google.common.collect.Lists;
import com.nuodb.migration.jdbc.connection.ConnectionProvider;
import com.nuodb.migration.jdbc.connection.ConnectionServices;
import com.nuodb.migration.jdbc.dialect.DatabaseDialect;
import com.nuodb.migration.jdbc.dialect.DatabaseDialectResolver;
import com.nuodb.migration.jdbc.model.Database;
import com.nuodb.migration.jdbc.model.DatabaseInspector;
import com.nuodb.migration.jdbc.model.Table;
import com.nuodb.migration.jdbc.query.*;
import com.nuodb.migration.jdbc.type.JdbcTypeRegistry;
import com.nuodb.migration.jdbc.type.access.JdbcTypeValueAccessProvider;
import com.nuodb.migration.job.JobBase;
import com.nuodb.migration.job.JobExecution;
import com.nuodb.migration.resultset.catalog.Catalog;
import com.nuodb.migration.resultset.catalog.CatalogEntry;
import com.nuodb.migration.resultset.catalog.CatalogWriter;
import com.nuodb.migration.resultset.format.ResultSetFormatFactory;
import com.nuodb.migration.resultset.format.ResultSetOutput;
import com.nuodb.migration.resultset.format.jdbc.JdbcTypeValueFormatRegistryResolver;
import com.nuodb.migration.spec.NativeQuerySpec;
import com.nuodb.migration.spec.SelectQuerySpec;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.sql.*;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;

import static com.google.common.io.Closeables.closeQuietly;
import static com.nuodb.migration.jdbc.model.ObjectType.*;
import static java.lang.String.format;
import static java.sql.Connection.TRANSACTION_READ_COMMITTED;
import static java.sql.Connection.TRANSACTION_REPEATABLE_READ;
import static java.sql.ResultSet.CONCUR_READ_ONLY;
import static java.sql.ResultSet.TYPE_FORWARD_ONLY;
import static org.apache.commons.lang3.StringUtils.isEmpty;

/**
 * @author Sergey Bushik
 */
public class DumpJob extends JobBase {

    private static final String QUERY_ENTRY_NAME = "query-%1$tH-%1$tM-%1$tS";

    protected final Log log = LogFactory.getLog(getClass());

    private TimeZone timeZone;
    private Catalog catalog;
    private Collection<SelectQuerySpec> selectQuerySpecs;
    private Collection<NativeQuerySpec> nativeQuerySpecs;
    private String outputType;
    private Map<String, String> attributes;
    private ConnectionProvider connectionProvider;
    private DatabaseDialectResolver databaseDialectResolver;
    private ResultSetFormatFactory resultSetFormatFactory;
    private JdbcTypeValueFormatRegistryResolver jdbcTypeValueFormatRegistryResolver;

    @Override
    public void execute(JobExecution jobExecution) throws Exception {
        DumpJobExecution dumpJobExecution = new DumpJobExecution(jobExecution);
        ConnectionServices connectionServices = connectionProvider.getConnectionServices();
        dumpJobExecution.setConnectionServices(connectionServices);
        try {
            dump(dumpJobExecution);
        } finally {
            if (connectionServices != null) {
                connectionServices.close();
            }
        }
    }

    protected void dump(DumpJobExecution dumpJobExecution) throws SQLException {
        ConnectionServices connectionServices = dumpJobExecution.getConnectionServices();
        DatabaseInspector databaseInspector = connectionServices.getDatabaseInspector();
        databaseInspector.withObjectTypes(CATALOG, SCHEMA, TABLE, COLUMN);
        databaseInspector.withDatabaseDialectResolver(databaseDialectResolver);

        Database database = databaseInspector.inspect();
        dumpJobExecution.setDatabase(database);

        Connection connection = connectionServices.getConnection();
        DatabaseMetaData metaData = connection.getMetaData();
        dumpJobExecution.setJdbcTypeValueFormatRegistry(jdbcTypeValueFormatRegistryResolver.resolve(metaData));

        CatalogWriter catalogWriter = getCatalog().getCatalogWriter();
        dumpJobExecution.setCatalogWriter(catalogWriter);

        DatabaseDialect databaseDialect = database.getDatabaseDialect();
        try {
            databaseDialect.setTransactionIsolation(connection,
                    new int[]{TRANSACTION_REPEATABLE_READ, TRANSACTION_READ_COMMITTED});

            if (databaseDialect.supportsSessionTimeZone()) {
                databaseDialect.setSessionTimeZone(connection, timeZone);
            }

            for (SelectQuery selectQuery : createSelectQueries(database, getSelectQuerySpecs())) {
                dump(dumpJobExecution, selectQuery, createCatalogEntry(selectQuery, getOutputType()));
            }
            for (NativeQuery nativeQuery : createNativeQueries(getNativeQuerySpecs())) {
                dump(dumpJobExecution, nativeQuery, createCatalogEntry(nativeQuery, getOutputType()));
            }
        } finally {
            closeQuietly(catalogWriter);

            if (databaseDialect.supportsSessionTimeZone()) {
                databaseDialect.setSessionTimeZone(connection, null);
            }
        }
    }

    protected CatalogEntry createCatalogEntry(SelectQuery selectQuery, String type) {
        Table table = selectQuery.getTables().get(0);
        return new CatalogEntry(table.getName(), type);
    }

    protected CatalogEntry createCatalogEntry(NativeQuery nativeQuery, String type) {
        return new CatalogEntry(format(QUERY_ENTRY_NAME, new Date()), type);
    }

    protected void dump(final DumpJobExecution dumpJobExecution, final Query query,
                        final CatalogEntry catalogEntry) throws SQLException {
        final Database database = dumpJobExecution.getDatabase();
        QueryTemplate queryTemplate = new QueryTemplate(dumpJobExecution.getConnectionServices().getConnection());
        queryTemplate.execute(
                new StatementCreator<PreparedStatement>() {
                    @Override
                    public PreparedStatement create(Connection connection) throws SQLException {
                        return prepareStatement(connection, database, query);
                    }
                },
                new StatementCallback<PreparedStatement>() {
                    @Override
                    public void execute(PreparedStatement preparedStatement) throws SQLException {
                        dump(dumpJobExecution, preparedStatement, catalogEntry);
                    }
                }
        );
    }

    protected void dump(DumpJobExecution dumpJobExecution, PreparedStatement preparedStatement,
                        CatalogEntry catalogEntry) throws SQLException {
        final ResultSetOutput resultSetOutput = getResultSetFormatFactory().createOutput(getOutputType());
        resultSetOutput.setAttributes(getAttributes());
        resultSetOutput.setJdbcTypeValueFormatRegistry(dumpJobExecution.getJdbcTypeValueFormatRegistry());

        DatabaseDialect databaseDialect = dumpJobExecution.getDatabase().getDatabaseDialect();
        if (!databaseDialect.supportsSessionTimeZone()) {
            resultSetOutput.setTimeZone(getTimeZone());
        }
        JdbcTypeRegistry jdbcTypeRegistry = databaseDialect.getJdbcTypeRegistry();
        resultSetOutput.setJdbcTypeValueAccessProvider(new JdbcTypeValueAccessProvider(jdbcTypeRegistry));

        ResultSet resultSet = preparedStatement.executeQuery();

        CatalogWriter catalogWriter = dumpJobExecution.getCatalogWriter();
        catalogWriter.addEntry(catalogEntry);
        resultSetOutput.setOutputStream(catalogWriter.getEntryOutput(catalogEntry));
        resultSetOutput.setResultSet(resultSet);

        resultSetOutput.writeBegin();
        while (dumpJobExecution.isRunning() && resultSet.next()) {
            resultSetOutput.writeRow();
        }
        resultSetOutput.writeEnd();
    }

    protected PreparedStatement prepareStatement(Connection connection, Database database,
                                                 Query query) throws SQLException {
        if (log.isDebugEnabled()) {
            log.debug(format("Prepare SQL: %s", query.toQuery()));
        }
        PreparedStatement preparedStatement = connection.prepareStatement(
                query.toQuery(), TYPE_FORWARD_ONLY, CONCUR_READ_ONLY);
        DatabaseDialect databaseDialect = database.getDatabaseDialect();
        databaseDialect.enableStreaming(preparedStatement);
        return preparedStatement;
    }

    protected Collection<SelectQuery> createSelectQueries(Database database,
                                                          Collection<SelectQuerySpec> selectQuerySpecs) {
        Collection<SelectQuery> selectQueries = Lists.newArrayList();
        if (selectQuerySpecs.isEmpty()) {
            selectQueries.addAll(createSelectQueries(database));
        } else {
            for (SelectQuerySpec selectQuerySpec : selectQuerySpecs) {
                selectQueries.add(createSelectQuery(database, selectQuerySpec));
            }
        }
        return selectQueries;
    }

    protected Collection<SelectQuery> createSelectQueries(Database database) {
        DatabaseDialect databaseDialect = database.getDatabaseDialect();
        Collection<SelectQuery> selectQueries = Lists.newArrayList();
        for (Table table : database.listTables()) {
            if (Table.TABLE.equals(table.getType())) {
                SelectQueryBuilder builder = new SelectQueryBuilder();
                builder.setDatabaseDialect(databaseDialect);
                builder.setTable(table);
                builder.setQualifyNames(true);
                selectQueries.add(builder.build());
            } else {
                if (log.isTraceEnabled()) {
                    log.trace(format("Skip %s %s", table.getQualifiedName(databaseDialect), table.getType()));
                }
            }
        }
        return selectQueries;
    }

    protected SelectQuery createSelectQuery(Database database, SelectQuerySpec selectQuerySpec) {
        DatabaseDialect databaseDialect = database.getDatabaseDialect();
        String tableName = selectQuerySpec.getTable();
        SelectQueryBuilder builder = new SelectQueryBuilder();
        builder.setQualifyNames(true);
        builder.setDatabaseDialect(databaseDialect);
        builder.setTable(database.findTable(tableName));
        builder.setColumns(selectQuerySpec.getColumns());
        if (!isEmpty(selectQuerySpec.getFilter())) {
            builder.addFilter(selectQuerySpec.getFilter());
        }
        return builder.build();
    }

    protected Collection<NativeQuery> createNativeQueries(Collection<NativeQuerySpec> nativeQuerySpecs) {
        Collection<NativeQuery> queries = Lists.newArrayList();
        for (NativeQuerySpec nativeQuerySpec : nativeQuerySpecs) {
            NativeQueryBuilder builder = new NativeQueryBuilder();
            builder.setQuery(nativeQuerySpec.getQuery());
            queries.add(builder.build());
        }
        return queries;
    }

    public ConnectionProvider getConnectionProvider() {
        return connectionProvider;
    }

    public void setConnectionProvider(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    public TimeZone getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(TimeZone timeZone) {
        this.timeZone = timeZone;
    }

    public Catalog getCatalog() {
        return catalog;
    }

    public void setCatalog(Catalog catalog) {
        this.catalog = catalog;
    }

    public Collection<SelectQuerySpec> getSelectQuerySpecs() {
        return selectQuerySpecs;
    }

    public void setSelectQuerySpecs(Collection<SelectQuerySpec> selectQuerySpecs) {
        this.selectQuerySpecs = selectQuerySpecs;
    }

    public Collection<NativeQuerySpec> getNativeQuerySpecs() {
        return nativeQuerySpecs;
    }

    public void setNativeQuerySpecs(Collection<NativeQuerySpec> nativeQuerySpecs) {
        this.nativeQuerySpecs = nativeQuerySpecs;
    }

    public String getOutputType() {
        return outputType;
    }

    public void setOutputType(String outputType) {
        this.outputType = outputType;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    public DatabaseDialectResolver getDatabaseDialectResolver() {
        return databaseDialectResolver;
    }

    public void setDatabaseDialectResolver(DatabaseDialectResolver databaseDialectResolver) {
        this.databaseDialectResolver = databaseDialectResolver;
    }

    public ResultSetFormatFactory getResultSetFormatFactory() {
        return resultSetFormatFactory;
    }

    public void setResultSetFormatFactory(ResultSetFormatFactory resultSetFormatFactory) {
        this.resultSetFormatFactory = resultSetFormatFactory;
    }

    public JdbcTypeValueFormatRegistryResolver getJdbcTypeValueFormatRegistryResolver() {
        return jdbcTypeValueFormatRegistryResolver;
    }

    public void setJdbcTypeValueFormatRegistryResolver(
            JdbcTypeValueFormatRegistryResolver jdbcTypeValueFormatRegistryResolver) {
        this.jdbcTypeValueFormatRegistryResolver = jdbcTypeValueFormatRegistryResolver;
    }
}