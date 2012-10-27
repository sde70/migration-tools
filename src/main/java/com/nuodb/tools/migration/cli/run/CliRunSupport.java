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
package com.nuodb.tools.migration.cli.run;

import com.nuodb.tools.migration.cli.CliOptions;
import com.nuodb.tools.migration.cli.CliResources;
import com.nuodb.tools.migration.cli.parse.CommandLine;
import com.nuodb.tools.migration.cli.parse.Option;
import com.nuodb.tools.migration.cli.parse.OptionException;
import com.nuodb.tools.migration.cli.parse.option.*;
import com.nuodb.tools.migration.context.support.ApplicationSupport;
import com.nuodb.tools.migration.spec.*;
import com.nuodb.tools.migration.utils.Priority;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.*;

/**
 * @author Sergey Bushik
 */
public class CliRunSupport extends ApplicationSupport implements CliResources, CliOptions {

    private OptionToolkit optionToolkit;

    protected CliRunSupport(OptionToolkit optionToolkit) {
        this.optionToolkit = optionToolkit;
    }

    /**
     * Builds the source group of options for the source database connection.
     *
     * @return group of options for the source database.
     */
    protected Option createSourceGroup() {
        Option driver = newOption().
                withName(SOURCE_DRIVER_OPTION).
                withDescription(getMessage(SOURCE_DRIVER_OPTION_DESCRIPTION)).
                withRequired(true).
                withArgument(
                        newArgument().
                                withName(getMessage(SOURCE_DRIVER_ARGUMENT_NAME)).
                                withRequired(true).
                                withMinimum(1).build()
                ).build();
        Option url = newOption().
                withName(SOURCE_URL_OPTION).
                withDescription(getMessage(SOURCE_URL_OPTION_DESCRIPTION)).
                withRequired(true).
                withArgument(
                        newArgument().
                                withName(getMessage(SOURCE_URL_ARGUMENT_NAME)).
                                withRequired(true).
                                withMinimum(1).build()
                ).build();
        Option username = newOption().
                withName(SOURCE_USERNAME_OPTION).
                withDescription(getMessage(SOURCE_USERNAME_OPTION_DESCRIPTION)).
                withArgument(
                        newArgument().
                                withName(getMessage(SOURCE_USERNAME_ARGUMENT_NAME)).build()
                ).build();
        Option password = newOption().
                withName(SOURCE_PASSWORD_OPTION).
                withDescription(getMessage(SOURCE_PASSWORD_OPTION_DESCRIPTION)).
                withArgument(
                        newArgument().
                                withName(getMessage(SOURCE_PASSWORD_ARGUMENT_NAME)).build()
                ).build();
        Option properties = newOption().
                withName(SOURCE_PROPERTIES_OPTION).
                withDescription(getMessage(SOURCE_PROPERTIES_OPTION_DESCRIPTION)).
                withArgument(
                        newArgument().
                                withName(getMessage(SOURCE_PROPERTIES_ARGUMENT_NAME)).build()
                ).build();
        Option catalog = newOption().
                withName(SOURCE_CATALOG_OPTION).
                withDescription(getMessage(SOURCE_CATALOG_OPTION_DESCRIPTION)).
                withArgument(
                        newArgument().
                                withName(getMessage(SOURCE_CATALOG_ARGUMENT_NAME)).build()
                ).build();
        Option schema = newOption().
                withName(SOURCE_SCHEMA_OPTION).
                withDescription(getMessage(SOURCE_SCHEMA_OPTION_DESCRIPTION)).
                withArgument(
                        newArgument().
                                withName(getMessage(SOURCE_SCHEMA_ARGUMENT_NAME)).build()
                ).build();
        return newGroup().
                withName(getMessage(SOURCE_GROUP_NAME)).
                withRequired(true).
                withMinimum(1).
                withOption(driver).
                withOption(url).
                withOption(username).
                withOption(password).
                withOption(properties).
                withOption(catalog).
                withOption(schema).build();
    }

    protected Option createOutputGroup() {
        OptionFormat optionFormat = optionToolkit.getOptionFormat();
        Option type = newOption().
                withName(OUTPUT_TYPE_OPTION).
                withDescription(getMessage(OUTPUT_TYPE_OPTION_DESCRIPTION)).
                withRequired(true).
                withArgument(
                        newArgument().
                                withName(getMessage(OUTPUT_TYPE_ARGUMENT_NAME)).
                                withRequired(true).
                                withMinimum(1).build()
                ).build();

        Option path = newOption().
                withName(OUTPUT_PATH_OPTION).
                withDescription(getMessage(OUTPUT_PATH_OPTION_DESCRIPTION)).
                withRequired(true).
                withArgument(
                        newArgument().
                                withName(getMessage(OUTPUT_PATH_ARGUMENT_NAME)).build()
                ).build();

        RegexOption attributes = new RegexOption();
        attributes.setName(OUTPUT_OPTION);
        attributes.setDescription(getMessage(OUTPUT_OPTION_DESCRIPTION));
        attributes.setPrefixes(optionFormat.getOptionPrefixes());
        attributes.setArgumentSeparator(optionFormat.getArgumentSeparator());
        attributes.addRegex(OUTPUT_OPTION, 1, Priority.LOW);
        attributes.setArgument(
                newArgument().
                        withName(getMessage(OUTPUT_OPTION_ARGUMENT_NAME)).
                        withValuesSeparator(null).withMinimum(1).withMaximum(Integer.MAX_VALUE).build());
        return newGroup().
                withName(getMessage(OUTPUT_GROUP_NAME)).
                withRequired(true).
                withMinimum(1).
                withOption(type).
                withOption(path).
                withOption(attributes).build();
    }

    /**
     * Table option handles -table=users, -table=roles and stores it items the option in the  command line.
     */
    protected Option createSelectQueryGroup() {
        OptionFormat optionFormat = optionToolkit.getOptionFormat();
        Option table = newOption().
                withName(TABLE_OPTION).
                withDescription(getMessage(TABLE_OPTION_DESCRIPTION)).
                withArgument(
                        newArgument().
                                withName(getMessage(TABLE_ARGUMENT_NAME)).
                                withMinimum(1).
                                withRequired(true).build()
                ).build();

        RegexOption tableFilter = new RegexOption();
        tableFilter.setName(TABLE_FILTER_OPTION);
        tableFilter.setDescription(getMessage(TABLE_FILTER_OPTION_DESCRIPTION));
        tableFilter.setPrefixes(optionFormat.getOptionPrefixes());
        tableFilter.setArgumentSeparator(optionFormat.getArgumentSeparator());
        tableFilter.addRegex(TABLE_FILTER_OPTION, 1, Priority.LOW);
        tableFilter.setArgument(
                newArgument().
                        withName(getMessage(TABLE_FILTER_ARGUMENT_NAME)).
                        withValuesSeparator(null).
                        withMinimum(1).
                        withRequired(true).build()
        );
        return newGroup().
                withName(getMessage(TABLE_GROUP_NAME)).
                withOption(table).
                withOption(tableFilter).
                withMaximum(Integer.MAX_VALUE).
                build();
    }

    protected Option createNativeQueryGroup() {
        Option query = newOption().
                withName(QUERY_OPTION).
                withDescription(getMessage(QUERY_OPTION_DESCRIPTION)).
                withArgument(
                        newArgument().
                                withName(getMessage(QUERY_ARGUMENT_NAME)).
                                withMinimum(1).
                                withValuesSeparator(null).
                                withRequired(true).build()
                ).build();
        return newGroup().
                withName(getMessage(QUERY_GROUP_NAME)).
                withOption(query).
                withMaximum(Integer.MAX_VALUE).build();

    }

    protected ConnectionSpec parseSourceGroup(CommandLine commandLine, Option option) {
        DriverManagerConnectionSpec connection = new DriverManagerConnectionSpec();
        connection.setCatalog(commandLine.<String>getValue(SOURCE_CATALOG_OPTION));
        connection.setSchema(commandLine.<String>getValue(SOURCE_SCHEMA_OPTION));
        connection.setDriver(commandLine.<String>getValue(SOURCE_DRIVER_OPTION));
        connection.setUrl(commandLine.<String>getValue(SOURCE_URL_OPTION));
        connection.setUsername(commandLine.<String>getValue(SOURCE_USERNAME_OPTION));
        connection.setPassword(commandLine.<String>getValue(SOURCE_PASSWORD_OPTION));
        String properties = commandLine.getValue(SOURCE_PROPERTIES_OPTION);
        if (properties != null) {
            Map<String, String> map = parseUrl(option, properties);
            connection.setProperties(map);
        }
        return connection;
    }

    protected FormatSpec parseOutputGroup(CommandLine commandLine, Option option) {
        FormatSpec format = new FormatSpecBase();
        format.setType(commandLine.<String>getValue(OUTPUT_TYPE_OPTION));
        format.setPath(commandLine.<String>getValue(OUTPUT_PATH_OPTION));
        format.setAttributes(parseFormatAttributes(
                commandLine.getOption(OUTPUT_OPTION), commandLine.<String>getValues(OUTPUT_OPTION)));
        return format;
    }

    protected Map<String, String> parseFormatAttributes(Option option, List<String> values) {
        Map<String, String> attributes = new HashMap<String, String>();
        for (Iterator<String> iterator = values.iterator(); iterator.hasNext(); ) {
            attributes.put(iterator.next(), iterator.next());
        }
        return attributes;
    }

    protected Collection<SelectQuerySpec> parseSelectQueryGroup(CommandLine commandLine, Option option) {
        Map<String, SelectQuerySpec> tableQueryMapping = new HashMap<String, SelectQuerySpec>();
        for (String table : commandLine.<String>getValues(TABLE_OPTION)) {
            tableQueryMapping.put(table, new SelectQuerySpec(table));
        }
        for (Iterator<String> iterator = commandLine.<String>getValues(
                TABLE_FILTER_OPTION).iterator(); iterator.hasNext(); ) {
            String name = iterator.next();
            SelectQuerySpec selectQuerySpec = tableQueryMapping.get(name);
            if (selectQuerySpec == null) {
                tableQueryMapping.put(name, selectQuerySpec = new SelectQuerySpec(name));
            }
            selectQuerySpec.setFilter(iterator.next());
        }
        return tableQueryMapping.values();
    }

    protected Collection<NativeQuerySpec> parseNativeQueryGroup(CommandLine commandLine, Option option) {
        List<NativeQuerySpec> nativeQuerySpecs = new ArrayList<NativeQuerySpec>();
        for (String query : commandLine.<String>getValues(QUERY_OPTION)) {
            NativeQuerySpec nativeQuerySpec = new NativeQuerySpec();
            nativeQuerySpec.setQuery(query);
            nativeQuerySpecs.add(nativeQuerySpec);
        }
        return nativeQuerySpecs;
    }

    /**
     * Parses url name1=value1&name2=value2 encoded string.
     *
     * @param option the option which contains parsed url
     * @param url    to be parsed
     * @return map of strings to strings formed from key value pairs from url
     */
    protected Map<String, String> parseUrl(Option option, String url) {
        try {
            url = URLDecoder.decode(url, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new OptionException(option, e.getMessage());
        }
        String[] params = url.split("&");
        Map<String, String> map = new HashMap<String, String>();
        for (String param : params) {
            String[] pair = param.split("=");
            if (pair.length != 2) {
                throw new OptionException(option, String.format("Malformed name-value pair %1$s", pair));
            }
            map.put(pair[0], pair[1]);
        }
        return map;
    }

    protected Option createTargetGroup() {
        Option url = newOption().
                withName(TARGET_URL_OPTION).
                withDescription(getMessage(TARGET_URL_OPTION_DESCRIPTION)).
                withRequired(true).
                withArgument(
                        newArgument().
                                withName(getMessage(TARGET_URL_ARGUMENT_NAME)).
                                withRequired(true).
                                withMinimum(1).build()
                ).build();
        Option username = newOption().
                withName(TARGET_USERNAME_OPTION).
                withDescription(getMessage(TARGET_USERNAME_OPTION_DESCRIPTION)).
                withArgument(
                        newArgument().
                                withName(getMessage(TARGET_USERNAME_ARGUMENT_NAME)).build()
                ).build();
        Option password = newOption().
                withName(TARGET_PASSWORD_OPTION).
                withDescription(getMessage(TARGET_PASSWORD_OPTION_DESCRIPTION)).
                withArgument(
                        newArgument().
                                withName(getMessage(TARGET_PASSWORD_ARGUMENT_NAME)).build()
                ).build();
        Option properties = newOption().
                withName(TARGET_PROPERTIES_OPTION).
                withDescription(getMessage(TARGET_PROPERTIES_OPTION_DESCRIPTION)).
                withArgument(
                        newArgument().
                                withName(getMessage(TARGET_PROPERTIES_ARGUMENT_NAME)).build()
                ).build();
        Option schema = newOption().
                withName(TARGET_SCHEMA_OPTION).
                withDescription(getMessage(TARGET_SCHEMA_OPTION_DESCRIPTION)).
                withArgument(
                        newArgument().
                                withName(getMessage(TARGET_SCHEMA_ARGUMENT_NAME)).build()
                ).build();
        return newGroup().
                withName(getMessage(TARGET_GROUP_NAME)).
                withRequired(true).
                withMinimum(1).
                withOption(url).
                withOption(username).
                withOption(password).
                withOption(properties).
                withOption(schema).build();
    }

    protected Option createInputGroup() {
        OptionFormat optionFormat = optionToolkit.getOptionFormat();
        Option type = newOption().
                withName(INPUT_TYPE_OPTION).
                withDescription(getMessage(INPUT_TYPE_OPTION_DESCRIPTION)).
                withArgument(
                        newArgument().
                                withName(getMessage(INPUT_TYPE_ARGUMENT_NAME)).
                                withRequired(true).
                                withMinimum(1).build()
                ).build();

        Option path = newOption().
                withName(INPUT_PATH_OPTION).
                withDescription(getMessage(INPUT_PATH_OPTION_DESCRIPTION)).
                withRequired(true).
                withArgument(
                        newArgument().
                                withName(getMessage(INPUT_PATH_ARGUMENT_NAME)).build()
                ).build();

        RegexOption attributes = new RegexOption();
        attributes.setName(INPUT_OPTION);
        attributes.setDescription(getMessage(INPUT_OPTION_DESCRIPTION));
        attributes.setPrefixes(optionFormat.getOptionPrefixes());
        attributes.setArgumentSeparator(optionFormat.getArgumentSeparator());
        attributes.addRegex(INPUT_OPTION, 1, Priority.LOW);
        attributes.setArgument(
                newArgument().
                        withName(getMessage(INPUT_OPTION_ARGUMENT_NAME)).
                        withValuesSeparator(null).withMinimum(1).withMaximum(Integer.MAX_VALUE).build());
        return newGroup().
                withName(getMessage(INPUT_GROUP_NAME)).
                withRequired(true).
                withMinimum(1).
                withOption(type).
                withOption(path).
                withOption(attributes).build();
    }

    protected ConnectionSpec parseTargetGroup(CommandLine commandLine, Option option) {
        DriverManagerConnectionSpec connection = new DriverManagerConnectionSpec();
        connection.setSchema(commandLine.<String>getValue(TARGET_SCHEMA_OPTION));
        connection.setUrl(commandLine.<String>getValue(TARGET_URL_OPTION));
        connection.setUsername(commandLine.<String>getValue(TARGET_USERNAME_OPTION));
        connection.setPassword(commandLine.<String>getValue(TARGET_PASSWORD_OPTION));
        String properties = commandLine.getValue(TARGET_PROPERTIES_OPTION);
        if (properties != null) {
            Map<String, String> map = parseUrl(option, properties);
            connection.setProperties(map);
        }
        return connection;
    }

    protected FormatSpec parseInputGroup(CommandLine commandLine, Option cliLoad) {
        FormatSpec format = new FormatSpecBase();
        format.setType(commandLine.<String>getValue(INPUT_TYPE_OPTION));
        format.setPath(commandLine.<String>getValue(INPUT_PATH_OPTION));
        format.setAttributes(parseFormatAttributes(
                commandLine.getOption(INPUT_OPTION), commandLine.<String>getValues(INPUT_OPTION)));
        return format;
    }

    protected OptionBuilder newOption() {
        return optionToolkit.newOption();
    }

    protected GroupBuilder newGroup() {
        return optionToolkit.newGroup();
    }

    protected ArgumentBuilder newArgument() {
        return optionToolkit.newArgument();
    }

    protected OptionFormat getOptionFormat() {
        return optionToolkit.getOptionFormat();
    }
}
