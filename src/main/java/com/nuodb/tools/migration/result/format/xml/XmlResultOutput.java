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
package com.nuodb.tools.migration.result.format.xml;

import com.nuodb.tools.migration.result.format.ResultFormatException;
import com.nuodb.tools.migration.result.format.ResultOutputBase;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import static javax.xml.XMLConstants.*;

/**
 * @author Sergey Bushik
 */
public class XmlResultOutput extends ResultOutputBase implements XmlAttributes {

    private XMLStreamWriter writer;

    private String encoding;
    private String version;

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    protected void doInitOutput() {
        version = getAttribute(ATTRIBUTE_VERSION, VERSION);
        encoding = getAttribute(ATTRIBUTE_ENCODING, ENCODING);

        XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newInstance();
        try {
            if (getWriter() != null) {
                writer = xmlOutputFactory.createXMLStreamWriter(getWriter());
            } else if (getOutputStream() != null) {
                writer = xmlOutputFactory.createXMLStreamWriter(getOutputStream(), getEncoding());
            }
        } catch (XMLStreamException e) {
            throw new ResultFormatException(e);
        }
    }

    @Override
    protected void doWriteBegin() {
        try {
            writer.writeStartDocument(getEncoding(), getVersion());
            writer.writeStartElement(DOCUMENT_ELEMENT);
            writer.writeStartElement(MODEL_ELEMENT);
            for (String column : getResultFormatModel().getColumns()) {
                writer.writeEmptyElement(COLUMN_ELEMENT);
                writer.setPrefix(DEFAULT_NS_PREFIX, NULL_NS_URI);
                writer.writeAttribute(ATTRIBUTE_NAME, column);
            }
            writer.writeEndElement();
        } catch (XMLStreamException e) {
            throw new ResultFormatException(e);
        }
    }

    @Override
    protected void doWriteRow(String[] values) {
        try {
            writer.writeStartElement(ROW_ELEMENT);
            for (String columnValue : values) {
                doWriteColumn(columnValue);
            }
            writer.writeEndElement();
        } catch (XMLStreamException e) {
            throw new ResultFormatException(e);
        }
    }

    protected void doWriteColumn(String value) throws XMLStreamException {
        if (value == null) {
            writer.writeEmptyElement(COLUMN_ELEMENT);
            writer.setPrefix("xsi", W3C_XML_SCHEMA_INSTANCE_NS_URI);
            writer.writeAttribute(W3C_XML_SCHEMA_INSTANCE_NS_URI, "nil", "true");
        } else {
            writer.writeStartElement(COLUMN_ELEMENT);
            writer.writeCharacters(value);
            writer.writeEndElement();
        }
    }

    @Override
    protected void doWriteEnd() {
        try {
            writer.writeEndElement();
            writer.writeEndDocument();
            writer.flush();
        } catch (XMLStreamException e) {
            throw new ResultFormatException(e);
        }
    }

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}