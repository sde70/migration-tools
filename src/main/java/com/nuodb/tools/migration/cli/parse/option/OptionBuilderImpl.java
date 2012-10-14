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
package com.nuodb.tools.migration.cli.parse.option;

import com.nuodb.tools.migration.cli.parse.*;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Sergey Bushik
 */
public class OptionBuilderImpl implements OptionBuilder {

    protected int id;
    protected String name;
    protected String description;
    protected boolean required;
    protected Argument argument;
    protected Group group;
    protected Set<String> aliases = new HashSet<String>();
    protected Set<Trigger> triggers = new HashSet<Trigger>();
    private Set<String> prefixes;
    private String argumentSeparator;

    public OptionBuilderImpl(OptionFormat optionFormat) {
        this.prefixes = optionFormat.getOptionPrefixes();
        this.argumentSeparator = optionFormat.getArgumentSeparator();
    }

    @Override
    public OptionBuilder withId(int id) {
        this.id = id;
        return this;
    }

    @Override
    public OptionBuilder withName(String name) {
        this.name = name;
        return this;
    }

    @Override
    public OptionBuilder withAlias(String alias) {
        this.aliases.add(alias);
        return this;
    }

    @Override
    public OptionBuilder withDescription(String description) {
        this.description = description;
        return this;
    }

    @Override
    public OptionBuilder withRequired(boolean required) {
        this.required = required;
        return this;
    }

    public OptionBuilder withGroup(Group group) {
        this.group = group;
        return this;
    }

    @Override
    public OptionBuilder withArgument(Argument argument) {
        this.argument = argument;
        return this;
    }

    @Override
    public OptionBuilder withPrefixes(Set<String> prefixes) {
        this.prefixes = prefixes;
        return this;
    }

    @Override
    public OptionBuilder withArgumentSeparator(String argumentSeparator) {
        this.argumentSeparator = argumentSeparator;
        return this;
    }

    @Override
    public OptionBuilder withTrigger(Trigger trigger) {
        triggers.add(trigger);
        return this;
    }

    @Override
    public Option build() {
        OptionImpl option = new OptionImpl();
        option.setId(id);
        option.setName(name);
        option.setDescription(description);
        option.setRequired(required);
        option.setArgument(argument);
        option.setArgumentSeparator(argumentSeparator);
        option.setPrefixes(prefixes);
        option.setAliases(aliases);
        option.setGroup(group);
        for (Trigger trigger : triggers) {
            option.addTrigger(trigger);
        }
        return option;
    }
}