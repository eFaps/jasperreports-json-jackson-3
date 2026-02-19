/*
 * JasperReports - Free Java Reporting Library.
 * Copyright (C) 2001 - 2025 Cloud Software Group, Inc. All rights reserved.
 * http://www.jaspersoft.com
 *
 * Unless you have purchased a commercial license agreement from Jaspersoft,
 * the following license terms apply:
 *
 * This program is part of JasperReports.
 *
 * JasperReports is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * JasperReports is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with JasperReports. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.jasperreports.json.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperReportsContext;
import net.sf.jasperreports.engine.type.JsonOperatorEnum;
import net.sf.jasperreports.repo.RepositoryContext;
import net.sf.jasperreports.repo.RepositoryUtil;
import net.sf.jasperreports.repo.SimpleRepositoryContext;
import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;


/**
 *
 * @author Narcis Marcu (narcism@users.sourceforge.net)
 */
public class JsonUtil {

    private static final Log log = LogFactory.getLog(JsonUtil.class);

    public static final String EXCEPTION_MESSAGE_KEY_UNKNOWN_OPERATOR = "util.json.unknown.operator";

    public static boolean evaluateJsonExpression(JsonNode contextNode, String attributeExpression) throws JRException {

        if (attributeExpression == null) {
            return true;
        }

        String attribute = null;
        JsonOperatorEnum operator = null;
        String value = null;
        boolean result = false;

        for (final JsonOperatorEnum joe: JsonOperatorEnum.values()) {
            final int indexOfOperator = attributeExpression.indexOf(joe.getValue());
            if (indexOfOperator != -1) {
                operator = joe;
                attribute = attributeExpression.substring(0, indexOfOperator).trim();
                value = attributeExpression.substring(indexOfOperator + joe.getValue().length()).trim();
                break;
            }
        }

        if (operator == null) {
            final StringBuilder possibleOperations = new StringBuilder();
            for (final JsonOperatorEnum op: JsonOperatorEnum.values()) {
                possibleOperations.append(op.getValue()).append(",");
            }
            throw
                new JRException(
                    EXCEPTION_MESSAGE_KEY_UNKNOWN_OPERATOR,
                    new Object[]{attributeExpression, possibleOperations});
        }

        if (attribute != null && operator != null && value != null) {
            // going down the path of the attribute must return a value node
            if (!contextNode.path(attribute).isValueNode()) {
                result = false;
            } else {
                final String contextValue = contextNode.path(attribute).asText();
                switch(operator) {
                case LT:
                    try {
                        result = Double.parseDouble(contextValue) < Double.parseDouble(value);
                    } catch (final NumberFormatException nfe) {
                        result = false;
                    }
                    break;
                case LE:
                    try {
                        result = Double.parseDouble(contextValue) <= Double.parseDouble(value);
                    } catch (final NumberFormatException nfe) {
                        result = false;
                    }
                    break;
                case GT:
                    try {
                        result = Double.parseDouble(contextValue) > Double.parseDouble(value);
                    } catch (final NumberFormatException nfe) {
                        result = false;
                    }
                    break;
                case GE:
                    try {
                        result = Double.parseDouble(contextValue) >= Double.parseDouble(value);
                    } catch (final NumberFormatException nfe) {
                        result = false;
                    }
                    break;
                case EQ:
                    result = contextValue.equals(value);
                    break;
                case NE:
                    result = !contextValue.equals(value);
                    break;
                default:
                }
            }
        }

        return result;
    }

    public static ObjectMapper createObjectMapper() {
        return JsonMapper.builder()
            .enable(JsonReadFeature.ALLOW_UNQUOTED_PROPERTY_NAMES)
            .enable(JsonReadFeature.ALLOW_SINGLE_QUOTES)
            .enable(JsonReadFeature.ALLOW_JAVA_COMMENTS)
            .build();
    }

    public static JsonNode parseJson(File file) throws JRException {
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            return parseJson(fileInputStream);
        } catch (final IOException e) {
            throw new JRException(e);
        }
    }

    public static JsonNode parseJson(JasperReportsContext jasperReportsContext, String location) throws JRException {
        return parseJson(SimpleRepositoryContext.of(jasperReportsContext), location);
    }

    public static JsonNode parseJson(RepositoryContext repositoryContext, String location) throws JRException {
        final RepositoryUtil repository = RepositoryUtil.getInstance(repositoryContext);
        final InputStream stream = repository.getInputStreamFromLocation(location);
        try {
            return parseJson(stream);
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (final IOException e) {
                    if (log.isWarnEnabled()) {
                        log.warn("Failed to close input stream for location " + location, e);
                    }
                }
            }
        }
    }

    public static JsonNode parseJson(InputStream jsonStream) throws JRException {
        final ObjectMapper mapper = createObjectMapper();
        return mapper.readTree(jsonStream);
    }
}
