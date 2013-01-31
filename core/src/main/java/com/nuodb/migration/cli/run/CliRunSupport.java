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
package com.nuodb.migration.cli.run;

import com.nuodb.migration.cli.CliOptions;
import com.nuodb.migration.cli.CliResources;
import com.nuodb.migration.cli.parse.CommandLine;
import com.nuodb.migration.cli.parse.Group;
import com.nuodb.migration.cli.parse.Option;
import com.nuodb.migration.cli.parse.OptionException;
import com.nuodb.migration.cli.parse.option.*;
import com.nuodb.migration.context.support.ApplicationSupport;
import com.nuodb.migration.jdbc.JdbcConstants;
import com.nuodb.migration.spec.JdbcConnectionSpec;
import com.nuodb.migration.spec.ResourceSpec;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import static com.google.common.collect.Maps.newHashMap;
import static com.nuodb.migration.utils.Priority.LOW;
import static java.lang.Integer.MAX_VALUE;
import static java.lang.String.format;

/**
 * @author Sergey Bushik
 */
public class CliRunSupport extends ApplicationSupport implements CliResources, CliOptions {

    public static final TimeZone DEFAULT_TIME_ZONE = TimeZone.getTimeZone("UTC");

    private OptionToolkit optionToolkit;
    private TimeZone defaultTimeZone = DEFAULT_TIME_ZONE;

    protected CliRunSupport(OptionToolkit optionToolkit) {
        this.optionToolkit = optionToolkit;
    }

    /**
     * Builds the source group of options for the source database connection.
     *
     * @return group of options for the source database.
     */
    protected Group createSourceGroup() {
        GroupBuilder group = newGroup().withName(getMessage(SOURCE_GROUP_NAME)).withRequired(true).withMinimum(1);

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
        group.withOption(driver);

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
        group.withOption(url);

        Option username = newOption().
                withName(SOURCE_USERNAME_OPTION).
                withDescription(getMessage(SOURCE_USERNAME_OPTION_DESCRIPTION)).
                withArgument(
                        newArgument().
                                withName(getMessage(SOURCE_USERNAME_ARGUMENT_NAME)).build()
                ).build();
        group.withOption(username);

        Option password = newOption().
                withName(SOURCE_PASSWORD_OPTION).
                withDescription(getMessage(SOURCE_PASSWORD_OPTION_DESCRIPTION)).
                withArgument(
                        newArgument().
                                withName(getMessage(SOURCE_PASSWORD_ARGUMENT_NAME)).build()
                ).build();
        group.withOption(password);

        Option properties = newOption().
                withName(SOURCE_PROPERTIES_OPTION).
                withDescription(getMessage(SOURCE_PROPERTIES_OPTION_DESCRIPTION)).
                withArgument(
                        newArgument().
                                withName(getMessage(SOURCE_PROPERTIES_ARGUMENT_NAME)).build()
                ).build();
        group.withOption(properties);

        Option catalog = newOption().
                withName(SOURCE_CATALOG_OPTION).
                withDescription(getMessage(SOURCE_CATALOG_OPTION_DESCRIPTION)).
                withArgument(
                        newArgument().
                                withName(getMessage(SOURCE_CATALOG_ARGUMENT_NAME)).build()
                ).build();
        group.withOption(catalog);

        Option schema = newOption().
                withName(SOURCE_SCHEMA_OPTION).
                withDescription(getMessage(SOURCE_SCHEMA_OPTION_DESCRIPTION)).
                withArgument(
                        newArgument().
                                withName(getMessage(SOURCE_SCHEMA_ARGUMENT_NAME)).build()
                ).build();
        group.withOption(schema);

        return group.build();
    }

    protected Group createOutputGroup() {
        GroupBuilder group = newGroup().withName(getMessage(OUTPUT_GROUP_NAME)).withRequired(true).withMinimum(1);

        Option type = newOption().
                withName(OUTPUT_TYPE_OPTION).
                withDescription(getMessage(OUTPUT_TYPE_OPTION_DESCRIPTION)).
                withRequired(true).
                withArgument(
                        newArgument().
                                withName(getMessage(OUTPUT_TYPE_ARGUMENT_NAME)).
                                withRequired(true).withMinimum(1).build()
                ).build();
        group.withOption(type);

        Option path = newOption().
                withName(OUTPUT_PATH_OPTION).
                withDescription(getMessage(OUTPUT_PATH_OPTION_DESCRIPTION)).
                withArgument(
                        newArgument().
                                withName(getMessage(OUTPUT_PATH_ARGUMENT_NAME)).build()
                ).build();
        group.withOption(path);

        OptionFormat optionFormat = new OptionFormat(getOptionFormat());
        optionFormat.setArgumentValuesSeparator(null);

        RegexOption attributes = new RegexOption(optionFormat);
        attributes.setName(OUTPUT_OPTION);
        attributes.setDescription(getMessage(OUTPUT_OPTION_DESCRIPTION));
        attributes.addRegex(OUTPUT_OPTION, 1, LOW);
        attributes.setArgument(
                newArgument().
                        withName(getMessage(OUTPUT_OPTION_ARGUMENT_NAME)).
                        withOptionFormat(optionFormat).withMinimum(1).withMaximum(MAX_VALUE).build());
        group.withOption(attributes);

        return group.build();
    }

    protected JdbcConnectionSpec parseSourceGroup(CommandLine commandLine, Option option) {
        JdbcConnectionSpec connectionSpec = new JdbcConnectionSpec();
        connectionSpec.setDriverClassName((String) commandLine.getValue(SOURCE_DRIVER_OPTION));
        connectionSpec.setUrl((String) commandLine.getValue(SOURCE_URL_OPTION));
        connectionSpec.setUsername((String) commandLine.getValue(SOURCE_USERNAME_OPTION));
        connectionSpec.setPassword((String) commandLine.getValue(SOURCE_PASSWORD_OPTION));
        connectionSpec.setProperties(parseProperties(commandLine, SOURCE_PROPERTIES_OPTION, option));
        connectionSpec.setCatalog((String) commandLine.getValue(SOURCE_CATALOG_OPTION));
        connectionSpec.setSchema((String) commandLine.getValue(SOURCE_SCHEMA_OPTION));
        return connectionSpec;
    }

    protected ResourceSpec parseOutputGroup(CommandLine commandLine, Option option) {
        ResourceSpec resource = new ResourceSpec();
        resource.setType((String) commandLine.getValue(OUTPUT_TYPE_OPTION));
        resource.setPath((String) commandLine.getValue(OUTPUT_PATH_OPTION));
        resource.setAttributes(parseAttributes(
                commandLine.<String>getValues(OUTPUT_OPTION), commandLine.getOption(OUTPUT_OPTION)));
        return resource;
    }

    protected Map<String, Object> parseAttributes(List<String> values, Option option) {
        Map<String, Object> attributes = newHashMap();
        for (Iterator<String> iterator = values.iterator(); iterator.hasNext(); ) {
            attributes.put(iterator.next(), iterator.next());
        }
        return attributes;
    }

    protected Option createTimeZoneOption() {
        return newOption().
                withName(TIME_ZONE_OPTION).
                withAlias(TIME_ZONE_SHORT_OPTION, OptionFormat.SHORT).
                withDescription(getMessage(TIME_ZONE_OPTION_DESCRIPTION)).
                withArgument(
                        newArgument().
                                withName(getMessage(TIME_ZONE_ARGUMENT_NAME)).
                                withMinimum(1).
                                withRequired(true).build()
                ).build();
    }

    protected TimeZone parseTimeZoneOption(CommandLine commandLine, Option option) {
        String timeZone = (String) commandLine.getValue(TIME_ZONE_OPTION);
        if (timeZone != null) {
            TimeZone systemTimeZone = TimeZone.getDefault();
            try {
                TimeZone.setDefault(getDefaultTimeZone());
                return TimeZone.getTimeZone(timeZone);
            } finally {
                TimeZone.setDefault(systemTimeZone);
            }
        } else {
            return getDefaultTimeZone();
        }
    }

    /**
     * Parses URL encoded properties name1=value1&name2=value2
     *
     * @param commandLine holding command line options
     * @param trigger     key to key value pairs
     * @param option      the option which contains parsed url
     */
    protected Map<String, Object> parseProperties(CommandLine commandLine, String trigger, Option option) {
        Map<String, Object> properties = null;
        String url = (String) commandLine.getValue(trigger);
        if (url != null) {
            try {
                url = URLDecoder.decode(url, "UTF-8");
            } catch (UnsupportedEncodingException exception) {
                throw new OptionException(option, exception.getMessage());
            }
            String[] params = url.split("&");
            properties = newHashMap();
            for (String param : params) {
                String[] pair = param.split("=");
                if (pair.length != 2) {
                    throw new OptionException(option, format("Malformed name-value pair %1$s", pair));
                }
                properties.put(pair[0], pair[1]);
            }
        }
        return properties;
    }

    protected Group createTargetGroup() {
        GroupBuilder group = newGroup().withName(getMessage(TARGET_GROUP_NAME));

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
        group.withOption(url);

        Option username = newOption().
                withName(TARGET_USERNAME_OPTION).
                withDescription(getMessage(TARGET_USERNAME_OPTION_DESCRIPTION)).
                withArgument(
                        newArgument().
                                withName(getMessage(TARGET_USERNAME_ARGUMENT_NAME)).build()
                ).build();
        group.withOption(username);

        Option password = newOption().
                withName(TARGET_PASSWORD_OPTION).
                withDescription(getMessage(TARGET_PASSWORD_OPTION_DESCRIPTION)).
                withArgument(
                        newArgument().
                                withName(getMessage(TARGET_PASSWORD_ARGUMENT_NAME)).build()
                ).build();
        group.withOption(password);

        Option properties = newOption().
                withName(TARGET_PROPERTIES_OPTION).
                withDescription(getMessage(TARGET_PROPERTIES_OPTION_DESCRIPTION)).
                withArgument(
                        newArgument().
                                withName(getMessage(TARGET_PROPERTIES_ARGUMENT_NAME)).build()
                ).build();
        group.withOption(properties);

        Option schema = newOption().
                withName(TARGET_SCHEMA_OPTION).
                withDescription(getMessage(TARGET_SCHEMA_OPTION_DESCRIPTION)).
                withArgument(
                        newArgument().
                                withName(getMessage(TARGET_SCHEMA_ARGUMENT_NAME)).build()
                ).build();
        group.withOption(schema);
        group.withRequired(true);
        group.withMinimum(1);
        return group.build();
    }

    protected Group createInputGroup() {
        Option path = newOption().
                withName(INPUT_PATH_OPTION).
                withDescription(getMessage(INPUT_PATH_OPTION_DESCRIPTION)).
                withRequired(true).
                withArgument(
                        newArgument().
                                withName(getMessage(INPUT_PATH_ARGUMENT_NAME)).build()
                ).build();

        OptionFormat optionFormat = new OptionFormat(getOptionFormat());
        optionFormat.setArgumentValuesSeparator(null);

        RegexOption attributes = new RegexOption();
        attributes.setName(INPUT_OPTION);
        attributes.setDescription(getMessage(INPUT_OPTION_DESCRIPTION));
        attributes.setOptionFormat(optionFormat);
        attributes.addRegex(INPUT_OPTION, 1, LOW);
        attributes.setArgument(
                newArgument().
                        withName(getMessage(INPUT_OPTION_ARGUMENT_NAME)).
                        withOptionFormat(optionFormat).withMinimum(1).withMaximum(MAX_VALUE).build());
        return newGroup().
                withName(getMessage(INPUT_GROUP_NAME)).
                withRequired(true).
                withMinimum(1).
                withOption(path).
                withOption(attributes).build();
    }

    protected JdbcConnectionSpec parseTargetGroup(CommandLine commandLine, Option option) {
        JdbcConnectionSpec connectionSpec = null;
        if (commandLine.hasOption(TARGET_URL_OPTION)) {
            connectionSpec = new JdbcConnectionSpec();
            connectionSpec.setDriverClassName(JdbcConstants.NUODB_DRIVER_CLASS_NAME);
            connectionSpec.setUrl((String) commandLine.getValue(TARGET_URL_OPTION));
            connectionSpec.setUsername((String) commandLine.getValue(TARGET_USERNAME_OPTION));
            connectionSpec.setPassword((String) commandLine.getValue(TARGET_PASSWORD_OPTION));
            connectionSpec.setProperties(parseProperties(commandLine, TARGET_PROPERTIES_OPTION, option));
            connectionSpec.setSchema((String) commandLine.getValue(TARGET_SCHEMA_OPTION));
        }
        return connectionSpec;
    }

    protected ResourceSpec parseInputGroup(CommandLine commandLine, Option option) {
        ResourceSpec resource = new ResourceSpec();
        resource.setPath((String) commandLine.getValue(INPUT_PATH_OPTION));
        resource.setAttributes(parseAttributes(
                commandLine.<String>getValues(INPUT_OPTION), commandLine.getOption(INPUT_OPTION)));
        return resource;
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

    public TimeZone getDefaultTimeZone() {
        return defaultTimeZone;
    }

    public void setDefaultTimeZone(TimeZone defaultTimeZone) {
        this.defaultTimeZone = defaultTimeZone;
    }
}
