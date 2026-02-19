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
package net.sf.jasperreports.json.export;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.sf.jasperreports.annotations.properties.Property;
import net.sf.jasperreports.annotations.properties.PropertyScope;
import net.sf.jasperreports.engine.DefaultJasperReportsContext;
import net.sf.jasperreports.engine.JRAbstractExporter;
import net.sf.jasperreports.engine.JRCommonText;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRGenericPrintElement;
import net.sf.jasperreports.engine.JRPrintElement;
import net.sf.jasperreports.engine.JRPrintFrame;
import net.sf.jasperreports.engine.JRPrintHyperlink;
import net.sf.jasperreports.engine.JRPrintPage;
import net.sf.jasperreports.engine.JRPrintText;
import net.sf.jasperreports.engine.JRPropertiesMap;
import net.sf.jasperreports.engine.JRPropertiesUtil;
import net.sf.jasperreports.engine.JRPropertiesUtil.PropertySuffix;
import net.sf.jasperreports.engine.JRRuntimeException;
import net.sf.jasperreports.engine.JasperReportsContext;
import net.sf.jasperreports.engine.export.JRExportProgressMonitor;
import net.sf.jasperreports.engine.export.data.BooleanTextValue;
import net.sf.jasperreports.engine.export.data.DateTextValue;
import net.sf.jasperreports.engine.export.data.NumberTextValue;
import net.sf.jasperreports.engine.export.data.StringTextValue;
import net.sf.jasperreports.engine.export.data.TextValue;
import net.sf.jasperreports.engine.export.data.TextValueHandler;
import net.sf.jasperreports.engine.type.EnumUtil;
import net.sf.jasperreports.engine.type.NamedEnum;
import net.sf.jasperreports.engine.util.JRDataUtils;
import net.sf.jasperreports.engine.util.JRStyledText;
import net.sf.jasperreports.engine.util.JRStyledTextUtil;
import net.sf.jasperreports.export.ExportInterruptedException;
import net.sf.jasperreports.export.ExporterInputItem;
import net.sf.jasperreports.export.WriterExporterOutput;
import net.sf.jasperreports.json.util.JsonUtil;
import net.sf.jasperreports.properties.PropertyConstants;
import tools.jackson.core.io.JsonStringEncoder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;


/**
 * @author Narcis Marcu (narcism@users.sourceforge.net)
 */
public class JsonMetadataExporter extends JRAbstractExporter<JsonMetadataReportConfiguration, JsonExporterConfiguration, WriterExporterOutput, JsonExporterContext>
{

    private static final Log log = LogFactory.getLog(JsonMetadataExporter.class);

    public static final String JSON_EXPORTER_KEY = JRPropertiesUtil.PROPERTY_PREFIX + "json";

    protected static final String JSON_EXPORTER_PROPERTIES_PREFIX = JRPropertiesUtil.PROPERTY_PREFIX + "export.json.";

    protected static final String EXCEPTION_MESSAGE_KEY_INVALID_JSON_OBJECT = "export.json.invalid.json.object";
    protected static final String EXCEPTION_MESSAGE_KEY_INVALID_JSON_OBJECT_SEMANTIC = EXCEPTION_MESSAGE_KEY_INVALID_JSON_OBJECT + ".semantic";
    protected static final String EXCEPTION_MESSAGE_KEY_INVALID_JSON_OBJECT_ARRAY_FOUND = EXCEPTION_MESSAGE_KEY_INVALID_JSON_OBJECT + ".array.found";

    @Property(
            category = PropertyConstants.CATEGORY_EXPORT,
            scopes = {PropertyScope.ELEMENT},
            sinceVersion = PropertyConstants.VERSION_6_0_0
            )
    public static final String JSON_EXPORTER_PATH_PROPERTY = JSON_EXPORTER_PROPERTIES_PREFIX + "path";
    @Property(
            category = PropertyConstants.CATEGORY_EXPORT,
            defaultValue = PropertyConstants.BOOLEAN_FALSE,
            scopes = {PropertyScope.ELEMENT},
            sinceVersion = PropertyConstants.VERSION_6_0_0,
            valueType = Boolean.class
            )
    public static final String JSON_EXPORTER_REPEAT_VALUE_PROPERTY = JSON_EXPORTER_PROPERTIES_PREFIX + "repeat.value";
    @Property(
            category = PropertyConstants.CATEGORY_EXPORT,
            scopes = {PropertyScope.ELEMENT},
            sinceVersion = PropertyConstants.VERSION_6_0_0
            )
    public static final String JSON_EXPORTER_DATA_PROPERTY = JSON_EXPORTER_PROPERTIES_PREFIX + "data";

    @Property(
            name = "net.sf.jasperreports.export.json.repeat.{path}",
            category = PropertyConstants.CATEGORY_EXPORT,
            defaultValue = PropertyConstants.BOOLEAN_FALSE,
            scopes = {PropertyScope.ELEMENT},
            sinceVersion = PropertyConstants.VERSION_6_1_0,
            valueType = Boolean.class
            )
    public static final String JSON_EXPORTER_REPEAT_PROPERTIES_PREFIX = JSON_EXPORTER_PROPERTIES_PREFIX + "repeat.";
    @Property(
            name = "net.sf.jasperreports.export.json.number.{path}",
            category = PropertyConstants.CATEGORY_EXPORT,
            scopes = {PropertyScope.ELEMENT},
            sinceVersion = PropertyConstants.VERSION_6_1_0
            )
    public static final String JSON_EXPORTER_NUMBER_PROPERTIES_PREFIX = JSON_EXPORTER_PROPERTIES_PREFIX + "number.";
    @Property(
            name = "net.sf.jasperreports.export.json.date.{path}",
            category = PropertyConstants.CATEGORY_EXPORT,
            scopes = {PropertyScope.ELEMENT},
            sinceVersion = PropertyConstants.VERSION_6_1_0
            )
    public static final String JSON_EXPORTER_DATE_PROPERTIES_PREFIX = JSON_EXPORTER_PROPERTIES_PREFIX + "date.";
    @Property(
            name = "net.sf.jasperreports.export.json.boolean.{path}",
            category = PropertyConstants.CATEGORY_EXPORT,
            scopes = {PropertyScope.ELEMENT},
            sinceVersion = PropertyConstants.VERSION_6_1_0
            )
    public static final String JSON_EXPORTER_BOOLEAN_PROPERTIES_PREFIX = JSON_EXPORTER_PROPERTIES_PREFIX + "boolean.";
    @Property(
            name = "net.sf.jasperreports.export.json.string.{path}",
            category = PropertyConstants.CATEGORY_EXPORT,
            scopes = {PropertyScope.ELEMENT},
            sinceVersion = PropertyConstants.VERSION_6_1_0
            )
    public static final String JSON_EXPORTER_STRING_PROPERTIES_PREFIX = JSON_EXPORTER_PROPERTIES_PREFIX + "string.";

    private static final String JSON_SCHEMA_ROOT_NAME = "___root";

    protected final DateFormat isoDateFormat = JRDataUtils.getIsoDateFormat();

    protected Writer writer;
    protected int reportIndex;
    protected int pageIndex;

    private Map<String, SchemaNode> pathToValueNode = new HashMap<>();
    private Map<String, SchemaNode> pathToObjectNode = new HashMap<>();
    private final Map<SchemaNode, ArrayList<String>> visitedMembers = new HashMap<>();

    private final ArrayList<SchemaNode> openedSchemaNodes = new ArrayList<>();

    private String jsonSchema;
    private String previousPath;
    private boolean escapeMembers;
    private boolean gotSchema;

    public void validateSchema(String jsonSchema) throws JRException {
        final ObjectMapper mapper = JsonUtil.createObjectMapper();

        final JsonNode root = mapper.readTree(jsonSchema);
        if (root.isObject()) {
            pathToValueNode = new HashMap<>();
            pathToObjectNode = new HashMap<>();

            previousPath = null;

            if (!isValid((ObjectNode) root, JSON_SCHEMA_ROOT_NAME, "", null)) {
                throw
                    new JRException(
                        EXCEPTION_MESSAGE_KEY_INVALID_JSON_OBJECT_SEMANTIC,
                        (Object[])null
                        );
            }
        } else {
            throw
                new JRException(
                    EXCEPTION_MESSAGE_KEY_INVALID_JSON_OBJECT_ARRAY_FOUND,
                    (Object[])null
                    );
        }
    }

    private boolean isValid(ObjectNode objectNode, String objectName, String currentPath, SchemaNode parent) {
        String nodeTypeValue = null;
        final JsonNode typeNode = objectNode.path("_type");

        if (typeNode.isMissingNode()) {
            nodeTypeValue = "object";
        } else if (!typeNode.isTextual()) {
            return false;
        }

        if (nodeTypeValue == null) {
            nodeTypeValue = typeNode.asText();
        }

        final NodeTypeEnum nodeType = NodeTypeEnum.getByName(nodeTypeValue);

        // enforce type "object" or "array" with "_children"
        if (!(NodeTypeEnum.OBJECT.equals(nodeType) || NodeTypeEnum.ARRAY.equals(nodeType) && objectNode.has("_children"))) {
            return false;
        }

        // enforce "_children" of type Object when typeNode is "array"
        if (NodeTypeEnum.ARRAY.equals(nodeType) && !objectNode.path("_children").isObject()) {
            return false;
        }

        boolean result = true;
        String availablePath = currentPath;
        SchemaNode schemaNode;

        availablePath = availablePath.length() > 0 ? (availablePath.endsWith(".") ? availablePath : availablePath + ".") + objectName : objectName;

        // _children properties are passed to the parent array
        if (parent != null) {
            schemaNode = parent;
        } else {

            int level;

            if (JSON_SCHEMA_ROOT_NAME.equals(objectName)) {
                level = 0;
            } else if (availablePath.length() > 0 && availablePath.indexOf(".") > 0) {
                level = availablePath.split("\\.").length - 1;
            } else {
                level = 1;
            }

            schemaNode = new SchemaNode(level, objectName, nodeType, currentPath.endsWith(".") ? currentPath.substring(0, currentPath.length() - 2) : currentPath);
            pathToObjectNode.put(availablePath, schemaNode);
        }

        for (final String field : objectNode.propertyNames()) {
            final JsonNode node = objectNode.path(field);
            String localPath = availablePath;

            if (!field.startsWith("_")) {
                schemaNode.addMember(field);
                if (node.isTextual() && node.asText().equals("value")) {
                    localPath = localPath.length() > 0 ? (localPath.endsWith(".") ? localPath : localPath + ".") + field : field;
                    pathToValueNode.put(localPath, schemaNode);
                } else if (node.isObject() && !isValid((ObjectNode) node, field, availablePath, null) || !node.isObject()) {
                    result = false;
                    break;
                }
            } else if (field.equals("_children") && !isValid((ObjectNode) node, "", availablePath, schemaNode)) {
                result = false;
                break;
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("object is valid: " + objectNode);
            log.debug("objectName: " + objectName);
            log.debug("currentPath: " + currentPath);
        }

        return result;
    }

    public JsonMetadataExporter()
    {
        this(DefaultJasperReportsContext.getInstance());
    }

    public JsonMetadataExporter(JasperReportsContext jasperReportsContext)
    {
        super(jasperReportsContext);

        exporterContext = new ExporterContext();
    }


    @Override
    protected Class<JsonExporterConfiguration> getConfigurationInterface()
    {
        return JsonExporterConfiguration.class;
    }


    @Override
    protected Class<JsonMetadataReportConfiguration> getItemConfigurationInterface()
    {
        return JsonMetadataReportConfiguration.class;
    }


    @Override
    public String getExporterKey()
    {
        return JSON_EXPORTER_KEY;
    }

    @Override
    public String getExporterPropertiesPrefix()
    {
        return JSON_EXPORTER_PROPERTIES_PREFIX;
    }

    @Override
    public void exportReport() throws JRException
    {
        /*   */
        ensureJasperReportsContext();
        ensureInput();

        //FIXMENOW check all exporter properties that are supposed to work at report level

        initExport();

        ensureOutput();

        writer = getExporterOutput().getWriter();

        try
        {
            exportReportToWriter();
        }
        catch (final IOException e)
        {
            throw
                new JRException(
                    EXCEPTION_MESSAGE_KEY_OUTPUT_WRITER_ERROR,
                    new Object[]{jasperPrint.getName()},
                    e);
        }
        finally
        {
            getExporterOutput().close();
            resetExportContext();//FIXMEEXPORT check if using same finally is correct; everywhere
        }
    }

    @Override
    protected void initExport()
    {
        super.initExport();
    }

    @Override
    protected void initReport()
    {
        super.initReport();
    }

    protected void exportReportToWriter() throws JRException, IOException
    {
        final List<ExporterInputItem> items = exporterInput.getItems();

        for(reportIndex = 0; reportIndex < items.size(); reportIndex++)//FIXMEJSONMETA deal with batch export
        {
            final ExporterInputItem item = items.get(reportIndex);

            setCurrentExporterInputItem(item);

            final JsonMetadataReportConfiguration currentItemConfiguration = getCurrentItemConfiguration();

            escapeMembers = currentItemConfiguration.isEscapeMembers();
            final String jsonSchemaResource = currentItemConfiguration.getJsonSchemaResource();

            if (jsonSchemaResource != null) {
                try (
                    Scanner scanner =
                        new Scanner(
                            getRepository().getInputStreamFromLocation(jsonSchemaResource),
                            StandardCharsets.UTF_8.name()
                            )
                    )
                {
                    jsonSchema = scanner.useDelimiter("\\A").next();
                }

                validateSchema(jsonSchema);
                gotSchema = true;
            } else if (log.isWarnEnabled()) {
                log.warn("No JSON Schema provided!");
            }

            final List<JRPrintPage> pages = jasperPrint.getPages();
            if (pages != null && pages.size() > 0)
            {
                final PageRange pageRange = getPageRange();
                final int startPageIndex = pageRange == null || pageRange.getStartPageIndex() == null ? 0 : pageRange.getStartPageIndex();
                final int endPageIndex = pageRange == null || pageRange.getEndPageIndex() == null ? pages.size() - 1 : pageRange.getEndPageIndex();

                JRPrintPage page = null;
                for(pageIndex = startPageIndex; pageIndex <= endPageIndex; pageIndex++)
                {
                    checkInterrupted();

                    page = pages.get(pageIndex);

                    exportPage(page);
                }

                closeOpenNodes();
            }

            if (log.isDebugEnabled()) {
                for (final Map.Entry<String, SchemaNode> entry: pathToValueNode.entrySet()) {
                    log.debug("pathToValueNode: path: " + entry.getKey() + "; node: " + entry.getValue());
                }

                for (final Map.Entry<String, SchemaNode> entry: pathToObjectNode.entrySet()) {
                    log.debug("pathToObjectNode: path: " + entry.getKey() + "; node: " + entry.getValue());
                }
            }
        }

        final boolean flushOutput = getCurrentConfiguration().isFlushOutput();
        if (flushOutput)
        {
            writer.flush();
        }
    }

    protected void exportPage(JRPrintPage page) throws IOException, ExportInterruptedException
    {
        final Collection<JRPrintElement> elements = page.getElements();

        exportElements(elements);

        final JRExportProgressMonitor progressMonitor = getCurrentItemConfiguration().getProgressMonitor();
        if (progressMonitor != null)
        {
            progressMonitor.afterPageExport();
        }
    }


    protected void exportElements(Collection<JRPrintElement> elements) throws IOException, ExportInterruptedException
    {
        if (elements != null && elements.size() > 0)
        {
            for (final JRPrintElement element : elements) {
                checkInterrupted();
                if (filter == null || filter.isToExport(element))
                {
                    exportElement(element);

                    if (element instanceof JRGenericPrintElement)
                    {
                        //exportElement(element);
                    }
                    else if (element instanceof JRPrintFrame)
                    {
                        exportElements(((JRPrintFrame) element).getElements());
                    }
                }
            }
        }
    }

    protected void exportElement(JRPrintElement element) throws IOException
    {
        final JRPropertiesMap propMap = element.getPropertiesMap();

        final List<PropertySuffix> properties = JRPropertiesUtil.getProperties(element, JSON_EXPORTER_PROPERTIES_PREFIX);

        for (final PropertySuffix property : properties)
        {
            String propertyPath = null;
            boolean repeatValue = false;
            Object value = null;
            boolean legacyPathProperty = false;

            final String propertyName = property.getKey();

            if (propertyName.equals(JSON_EXPORTER_PATH_PROPERTY))
            {
                legacyPathProperty = true;
                propertyPath = property.getValue();
                repeatValue = getPropertiesUtil().getBooleanProperty(propMap, JSON_EXPORTER_REPEAT_VALUE_PROPERTY, false);
            }
            else if (propertyName.startsWith(JSON_EXPORTER_STRING_PROPERTIES_PREFIX))
            {
                propertyPath = propertyName.substring(JSON_EXPORTER_STRING_PROPERTIES_PREFIX.length());
                repeatValue = getPropertiesUtil().getBooleanProperty(propMap, JSON_EXPORTER_REPEAT_PROPERTIES_PREFIX + propertyPath, false);
                value = property.getValue();
            }
            else if (propertyName.startsWith(JSON_EXPORTER_NUMBER_PROPERTIES_PREFIX))
            {
                propertyPath = propertyName.substring(JSON_EXPORTER_NUMBER_PROPERTIES_PREFIX.length());
                repeatValue = getPropertiesUtil().getBooleanProperty(propMap, JSON_EXPORTER_REPEAT_PROPERTIES_PREFIX + propertyPath, false);
                value = Double.parseDouble(property.getValue());
            }
            else if (propertyName.startsWith(JSON_EXPORTER_DATE_PROPERTIES_PREFIX))
            {
                propertyPath = propertyName.substring(JSON_EXPORTER_DATE_PROPERTIES_PREFIX.length());
                repeatValue = getPropertiesUtil().getBooleanProperty(propMap, JSON_EXPORTER_REPEAT_PROPERTIES_PREFIX + propertyPath, false);
                try
                {
                    value = isoDateFormat.parse(property.getValue());
                }
                catch (final ParseException e)
                {
                    throw new JRRuntimeException(e);
                }
            }
            else if (propertyName.startsWith(JSON_EXPORTER_BOOLEAN_PROPERTIES_PREFIX))
            {
                propertyPath = propertyName.substring(JSON_EXPORTER_BOOLEAN_PROPERTIES_PREFIX.length());
                repeatValue = getPropertiesUtil().getBooleanProperty(propMap, JSON_EXPORTER_REPEAT_PROPERTIES_PREFIX + propertyPath, false);
                value = Boolean.parseBoolean(property.getValue());
            }

            if (propertyPath != null && propertyPath.length() > 0)
            {
                final String absolutePath = JSON_SCHEMA_ROOT_NAME + "." + propertyPath;

                // we have a mapped node for this path
                if (gotSchema)
                {
                    if (pathToValueNode.containsKey(absolutePath))
                    {
                        if (log.isDebugEnabled()) {
                            log.debug("found element with path: " + propertyPath);
                        }

                        if (legacyPathProperty)
                        {
                            value = getValue(element);
                        }

                        processElement(value, absolutePath, repeatValue);
                    }
                }
                else
                {
                    prepareSchema(absolutePath);
                    if (log.isDebugEnabled()) {
                        log.debug("found element with path: " + propertyPath);
                    }

                    if (legacyPathProperty)
                    {
                        value = getValue(element);
                    }

                    processElement(value, absolutePath, repeatValue);
                }
            }
        }
    }

    private void prepareSchema(String absolutePath) {
        if (!pathToValueNode.containsKey(absolutePath)) {
            final String valueProperty = absolutePath.substring(absolutePath.lastIndexOf(".") + 1);
            final String[] objectPathSegments = absolutePath.substring(0, absolutePath.lastIndexOf(".")).split("\\.");
            SchemaNode node = null;

            for (int i = 0; i < objectPathSegments.length; i++) {
                final StringBuilder objectPath = new StringBuilder(objectPathSegments[0]);
                for (int j = 1; j <= i; j++) {
                    objectPath.append(".").append(objectPathSegments[j]);
                }

                if (!pathToObjectNode.containsKey(objectPath.toString())) {
                    String schemaNodePath = "";

                    for (int k = 0; k < i; k++) {
                        schemaNodePath += schemaNodePath.length() > 0 ? "." + objectPathSegments[k] : objectPathSegments[k];
                    }

                    node = new SchemaNode(i, objectPathSegments[i], NodeTypeEnum.ARRAY, schemaNodePath);


                    pathToObjectNode.put(objectPath.toString(), node);
                } else {
                    node = pathToObjectNode.get(objectPath.toString());
                }

                if (i < objectPathSegments.length - 1 && node.getMember(objectPathSegments[i+1]) == null) {
                    node.addMember(objectPathSegments[i+1]);
                }
            }

            node.addMember(valueProperty);
            pathToValueNode.put(absolutePath, node);
        }
    }

    private Object getValue(JRPrintElement element) throws IOException
    {
        Object value;
        final String textStr;
        final boolean hasDataProp;
        if (element.getPropertiesMap().containsProperty(JSON_EXPORTER_DATA_PROPERTY))
        {
            hasDataProp = true;
            textStr = element.getPropertiesMap().getProperty(JSON_EXPORTER_DATA_PROPERTY);
        }
        else
        {
            hasDataProp = false;
            if (element instanceof JRPrintText)
            {
                final JRPrintText printText = (JRPrintText)element;
                final JRStyledText styledText = getStyledText(printText);

                if (styledText != null)
                {
                    textStr = styledText.getText();
                }
                else
                {
                    textStr = null;
                }
            }
            else
            {
                textStr = null;
            }
        }

        if (element instanceof JRPrintText)
        {
            final JRPrintText printText = (JRPrintText)element;
            final TextValue textValue = getTextValue(printText, textStr);
            final LocalTextValueHandler handler = new LocalTextValueHandler(hasDataProp, textStr);
            try
            {
                textValue.handle(handler);
            }
            catch (final JRException e)
            {
                throw new JRRuntimeException(e);
            }
            value = handler.getValue();
        }
        else
        {
            value = textStr;
        }

        return value;
    }

    private void processElement(Object value, String absolutePath, boolean repeatValue) throws IOException
    {
        if (openedSchemaNodes.size() == 0) {
            // initialize the json for the first time
            initJson(absolutePath, value, repeatValue);
        } else {
            final String valueProperty = absolutePath.substring(absolutePath.lastIndexOf(".") + 1);

            final String[] curSegments = absolutePath.substring(0, absolutePath.lastIndexOf(".")).split("\\.");
            final String[] prevSegments = previousPath.substring(0, previousPath.lastIndexOf(".")).split("\\.");

            final int ln = Math.min(curSegments.length, prevSegments.length);
            int lastCommonIndex = -1;

            for (int i = 0; i < ln; i++) {
                if (curSegments[i].equals(prevSegments[i])) {
                    lastCommonIndex = i;
                } else {
                    break;
                }
            }

            final int commonSegmentsNo = lastCommonIndex + 1;

            // compared to previous path, we have different path with common segments
            if (commonSegmentsNo < prevSegments.length) {
                if (log.isDebugEnabled()) {
                    log.debug("\tgot different path with common segments");
                }

                // close the extra path segments of the previous path
                closeExtraPathSegments(prevSegments, lastCommonIndex);

                // open new path segments for the current path
                openPathSegments(curSegments, lastCommonIndex + 1);
            }
            // we have a longer path that extends previous path
            else if (commonSegmentsNo == prevSegments.length && curSegments.length > prevSegments.length) {
                if (log.isDebugEnabled()) {
                    log.debug("\tgot longer path than previous one");
                }

                // open new paths
                openPathSegments(curSegments, lastCommonIndex + 1);
            }

            final SchemaNode currentNode = pathToValueNode.get(absolutePath);

            if (log.isDebugEnabled()) {
                log.debug("\tcurrent node is: " + currentNode.getType().getName());
            }

            if (currentNode.isArray()) {
                writePathProperty(currentNode, valueProperty, value, repeatValue);
            }
            // just write the value for property, no repeat
            else {
                writePathProperty(currentNode, valueProperty, value, false);
            }
        }

        previousPath = absolutePath;
    }

    private void writePathProperty(SchemaNode node, String valueProperty, Object value, boolean repeatValue) throws IOException {
        if (log.isDebugEnabled()) {
            log.debug("\twriting property: " + valueProperty);
        }
        ArrayList<String> vizMembers = visitedMembers.get(node);
        String lastProp = null;
        int lastPropIdx = -1;
        final int valPropIdx = node.indexOfMember(valueProperty);

        if (vizMembers != null && vizMembers.size() > 0) {
            lastProp = vizMembers.get(vizMembers.size() - 1);
            lastPropIdx = node.indexOfMember(lastProp);
        } else {
            vizMembers = new ArrayList<>();
            visitedMembers.put(node, vizMembers);
        }

        boolean foundPreviousRepeated = false;

        // if property of the same object
        if (lastProp == null || valPropIdx > lastPropIdx) {
            if (log.isDebugEnabled()) {
                log.debug("\tgot property of the same object");
            }

            // check for repeated values, if any, before writing current
            if (lastProp != null) {
                foundPreviousRepeated = writeReapeatedValues(node, lastPropIdx + 1, valPropIdx);
            } else {
                foundPreviousRepeated = writeReapeatedValues(node, 0, valPropIdx);
            }

            if (foundPreviousRepeated || vizMembers.size() > 0) {
                writer.write(",\n");
            }

            writeEscaped(node, valueProperty, value, repeatValue);

            // mark visited property for current node
            visitedMembers.get(node).add(valueProperty);
        }
        // create new object
        else {
            if (log.isDebugEnabled()) {
                log.debug("\tgot property of a new object");
            }
            // before closing current object, write the repeated values, if any, from last accessed property until the end is reached
            writeReapeatedValues(node, lastPropIdx + 1, node.getMembers().size());

            // close existing object
            writer.write("},\n{");

            // check for repeated values, if any, before writing current
            foundPreviousRepeated = writeReapeatedValues(node, 0, valPropIdx);

            if (foundPreviousRepeated) {
                writer.write(",");
            }

            writeEscaped(node, valueProperty, value, repeatValue);

            // mark visited property for current node
            visitedMembers.get(node).clear();
            visitedMembers.get(node).add(valueProperty);
        }
    }

    private boolean writeReapeatedValues(SchemaNode node, int from, int to) throws IOException {
        return writeReapeatedValues(node, from, to, true);
    }

    private boolean writeReapeatedValues(SchemaNode node, int from, int to, boolean startWithComma) throws IOException {
        boolean found = false;
        SchemaNodeMember member;

        for (int i = from; i < to; i++) {
            member = node.getMember(i);
            if (member.isRepeatValue() && member.getPreviousValue() != null) {
                found = true;
                if (i != 0 && startWithComma) {
                    writer.write(",");
                }
                if (escapeMembers) {
                    writer.write("\"" + member.getName() + "\":");
                } else {
                    writer.write(member.getName() + ":");
                }

                writeValue(member.getPreviousValue());

                if (log.isDebugEnabled()) {
                    log.debug("\t\twriting repeated value for member: " + member.getName());
                }
            }
        }

        return found;
    }

    private void writeEscaped(SchemaNode node, String valueProperty, Object value, boolean repeatValue) throws IOException {
        // write current value
        if (escapeMembers) {
            writer.write("\"" + valueProperty + "\":");
        } else {
            writer.write(valueProperty + ":");
        }

        writeValue(value);

        // mark repeated value
        if (repeatValue) {
            final SchemaNodeMember nodeMember = node.getMember(valueProperty);
            nodeMember.setRepeatValue(true);
            nodeMember.setPreviousValue(value);
        }
    }

    private void closeExtraPathSegments(String[] prevSegments, int lastCommonIndex) throws IOException {
        for (int i = prevSegments.length - 1; i > lastCommonIndex; i--) {
            final StringBuilder sb = new StringBuilder(prevSegments[0]);
            for (int j=1; j <= i; j++) {
                sb.append(".").append(prevSegments[j]);
            }

            final SchemaNode toClose = pathToObjectNode.get(sb.toString());

            if (openedSchemaNodes.get(openedSchemaNodes.size() - 1).equals(toClose)) {
                openedSchemaNodes.remove(openedSchemaNodes.size() - 1);
            } else if (log.isWarnEnabled()) {
                log.warn("unexpected");
            }

            // write previous repeated before closing
            if (toClose.isArray()) {
                final List<String> vizMembers = visitedMembers.get(toClose);
                final String lastProp = vizMembers.get(vizMembers.size() - 1);
                final int lastPropIdx = toClose.indexOfMember(lastProp);
                writeReapeatedValues(toClose, lastPropIdx + 1, toClose.getMembers().size());

                // clear visited member cache for closed node
                vizMembers.clear();
            }

            if (toClose.isObject()) {
                writer.write("}\n");
            } else {
                writer.write("}]\n");
            }

            if (log.isDebugEnabled()) {
                log.debug("\t\tclosing " + toClose.getType().getName() + " path: " + sb.toString());
            }
        }
    }

    private void openPathSegments(String[] pathSegments, int from) throws IOException {
        for (int i = from; i < pathSegments.length; i++) {
            final StringBuilder sb = new StringBuilder(pathSegments[0]);
            final StringBuilder parentPath = new StringBuilder(pathSegments[0]);
            for (int j=1; j <= i; j++) {
                sb.append(".").append(pathSegments[j]);
                if (j < i) {
                    parentPath.append(".").append(pathSegments[j]);
                }
            }

            final SchemaNode parent = pathToObjectNode.get(parentPath.toString());
            final String currentProperty = pathSegments[i];
            boolean foundPreviousRepeated = false;

            ArrayList<String> vizMembers = visitedMembers.get(parent);
            String lastVisitedProp = null;
            int lastVisitedPropIdx = -1;
            final int currentPropIdx = parent.indexOfMember(currentProperty);

            if (vizMembers != null && vizMembers.size() > 0) {
                lastVisitedProp = vizMembers.get(vizMembers.size() - 1);
                lastVisitedPropIdx = parent.indexOfMember(lastVisitedProp);
            }

            // before opening new path, check if previous has repeated values to be written
            if (parent.isArray()) {
                if (lastVisitedProp != null) {
                    foundPreviousRepeated = writeReapeatedValues(parent, lastVisitedPropIdx + 1, currentPropIdx, false);
                } else {
                    vizMembers = new ArrayList<>();
                    visitedMembers.put(parent, vizMembers);
                }

                vizMembers.add(currentProperty);
            }

            if (foundPreviousRepeated ||
                    // got another property of the same object
                    lastVisitedPropIdx != -1 && currentPropIdx > lastVisitedPropIdx) {
                writer.write(",");
            }

            if (escapeMembers) {
                writer.write("\"" + currentProperty + "\":");
            } else {
                writer.write(currentProperty + ":");
            }

            final SchemaNode toOpen = pathToObjectNode.get(sb.toString());

            openedSchemaNodes.add(toOpen);

            if (toOpen.isObject()) {
                writer.write("{");
            } else {
                writer.write("[{");
            }

            if (log.isDebugEnabled()) {
                log.debug("\t\topening " + toOpen.getType().getName() + " path: " + sb.toString());
            }
        }
    }

    private void closeOpenNodes() throws IOException {
        if (openedSchemaNodes.size() == 0) {
            return;
        }

        SchemaNode toClose;
        for (int i = openedSchemaNodes.size() - 1; i >= 0; i--) {
            toClose = openedSchemaNodes.get(i);
            if (toClose.isArray()) {
                // write previous repeated before closing
                final List<String> vizMembers = visitedMembers.get(toClose);

                final String lastProp = vizMembers.get(vizMembers.size() - 1);
                final int lastPropIdx = toClose.indexOfMember(lastProp);
                writeReapeatedValues(toClose, lastPropIdx + 1, toClose.getMembers().size());

                // clear visited member cache for closed node
                vizMembers.clear();

                writer.write("}]");
            } else {
                writer.write("}");
            }

            if (log.isDebugEnabled()) {
                log.debug("closing " + toClose.getType().getName() + " path: " + (toClose.path.length() > 0 ? toClose.path + "." : "") + toClose.name);
            }
        }
    }

    private void initJson(String firstPath, Object firstValue, boolean repeatValue) throws IOException {
        if (log.isDebugEnabled()) {
            log.debug("Initializing JSON with first absolute path: " + firstPath);
        }
        final String[] segments = firstPath.split("\\.");

        String currentPath = "";
        SchemaNode schemaNode = null;
        int i;

        for (i=0; i < segments.length - 1; i++) {
            currentPath = currentPath.length() > 0 ? currentPath + "." + segments[i] : segments[i];
            schemaNode = pathToObjectNode.get(currentPath);

            openedSchemaNodes.add(schemaNode);

            if (i == 0) { // got root node
                if (schemaNode.isObject()) {
                    writer.write("{");
                } else {
                    writer.write("[{");
                }
            } else {
                final String parentPath = currentPath.substring(0, currentPath.lastIndexOf("."));
                final SchemaNode parent = pathToObjectNode.get(parentPath);
                final String currentProperty = segments[i];

                final ArrayList<String> vizMembers = new ArrayList<>();
                vizMembers.add(currentProperty);
                visitedMembers.put(parent, vizMembers);

                if (schemaNode.isObject()) {
                    if (escapeMembers) {
                        writer.write("\"" + currentProperty + "\": {");
                    } else {
                        writer.write(currentProperty + ": {");
                    }
                } else if (escapeMembers) {
                    writer.write("\"" + currentProperty + "\": [{");
                } else {
                    writer.write(currentProperty + ": [{");
                }
            }
        }

        if (escapeMembers) {
            writer.write("\"" + segments[i] + "\": ");
        } else {
            writer.write(segments[i] + ": ");
        }
        writeValue(firstValue);

        // mark repeated value
        if (schemaNode != null && repeatValue) {
            final SchemaNodeMember nodeMember = schemaNode.getMember(segments[i]);
            nodeMember.setRepeatValue(true);
            nodeMember.setPreviousValue(firstValue);
        }

        // mark visited property for current node
        final ArrayList<String> members = new ArrayList<>();
        members.add(segments[i]);
        visitedMembers.put(schemaNode, members);
    }

    private void writeValue(Object value)throws IOException {
        if (value != null) {
            if (
                    value instanceof Number
                            || value instanceof Boolean
                    )
            {
                writer.write(value.toString());
            } else if (value instanceof Date) {
                writer.write("\"");
                writer.write(isoDateFormat.format((Date)value));
                writer.write("\"");
            } else {
                writer.write("\"");
                final StringBuilder quotedBldr = new StringBuilder();
                JsonStringEncoder.getInstance().quoteAsString(value.toString(), quotedBldr);
                writer.write(quotedBldr.toString());
                writer.write("\"");
            }
        } else {
            writer.write("null");  // FIXMEJSONMETA: how to treat null values?
        }
    }

    @Override
    protected JRStyledText getStyledText(JRPrintText textElement)
    {
        JRStyledText styledText = textElement.getFullStyledText(noneSelector);

        if (styledText != null && !JRCommonText.MARKUP_NONE.equals(textElement.getMarkup()))
        {
            styledText = JRStyledTextUtil.getBulletedText(styledText);
        }

        return styledText;
    }

    protected class ExporterContext extends BaseExporterContext implements JsonExporterContext
    {
        @Override
        public String getHyperlinkURL(JRPrintHyperlink link)
        {
            return ""; // FIXMEJSONMETA should we treat hyperlinks?
        }
    }


    private static class SchemaNode {
        private final int level;
        private final String name;
        private final NodeTypeEnum type;
        private final String path;
        private final List<SchemaNodeMember> members;
        private final List<String> memberNames;

        public SchemaNode(int _level, String _name, NodeTypeEnum _type, String _path) {
            level = _level;
            name = _name;
            type = _type;
            path = _path;
            members = new ArrayList<>();
            memberNames = new ArrayList<>();
        }

        public NodeTypeEnum getType() {
            return type;
        }

        public void addMember(String memberName) {
            members.add(new SchemaNodeMember(memberName));
            memberNames.add(memberName);
        }

        public boolean isObject() {
            return NodeTypeEnum.OBJECT.equals(type);
        }

        public boolean isArray() {
            return NodeTypeEnum.ARRAY.equals(type);
        }

        public int indexOfMember(String memberName) {
            return memberNames.indexOf(memberName);
        }

        public SchemaNodeMember getMember(int i) {
            return members.get(i);
        }

        public SchemaNodeMember getMember(String memberName) {
            if (indexOfMember(memberName) != -1) {
                return members.get(indexOfMember(memberName));
            }  else {
                return null;
            }
        }

        public List<SchemaNodeMember> getMembers() {
            return members;
        }

        @Override
        public String toString() {
            final StringBuilder out = new StringBuilder("{");
            final boolean isArray = NodeTypeEnum.ARRAY.equals(type);

            out.append("level: ").append(level).append(", ");
            out.append("name: \"").append(name).append("\", ");
            out.append("type: \"").append(type.getName()).append("\", ");
            out.append("path: \"").append(path).append("\", ");
            out.append("members: [");
            if (isArray) {
                out.append("{");
            }
            for (int i=0, ln = members.size(); i < ln; i++) {
                out.append("\"").append(members.get(i).getName()).append("\"");
                if (i < ln-1) {
                    out.append(", ");
                }
            }
            if (isArray) {
                out.append("}");
            }
            out.append("]}");
            return out.toString();
        }

        @Override
        public boolean equals(Object obj) {
            return this.level == ((SchemaNode)obj).level
                    && this.name.equals(((SchemaNode)obj).name)
                    && this.type.equals(((SchemaNode)obj).type)
                    && this.path.equals(((SchemaNode)obj).path);
        }

        @Override
        public int hashCode() {
            int hash = level !=0 ? level : 41;
            hash = hash * 41 + name.hashCode();
            hash = hash * 41 + type.hashCode();
            hash = hash * 41 + path.hashCode();
            return hash;
        }
    }

    private static class SchemaNodeMember {

        private boolean repeatValue;
        private Object previousValue;
        private final String name;

        public SchemaNodeMember(String _name) {
            name = _name;
        }

        public String getName() {
            return name;
        }

        public boolean isRepeatValue() {
            return repeatValue;
        }

        public void setRepeatValue(boolean _repeatValue) {
            repeatValue = _repeatValue;
        }

        public Object getPreviousValue() {
            return previousValue;
        }

        public void setPreviousValue(Object _previousValue) {
            previousValue = _previousValue;
        }

    }

    private enum NodeTypeEnum implements NamedEnum
    {
        /**
         *
         */
        OBJECT("object"),

        /**
         *
         */
        ARRAY("array");

        private final String name;

        NodeTypeEnum(String _name)
        {
            name = _name;
        }

        @Override
        public String getName()
        {
            return name;
        }

        public static NodeTypeEnum getByName(String name)
        {
            return EnumUtil.getEnumByName(values(), name);
        }
    }

    private class LocalTextValueHandler implements TextValueHandler
    {
        Object value;
        boolean hasDataProp;
        String textStr;

        public LocalTextValueHandler(boolean hasDataProp, String textStr)
        {
            this.hasDataProp = hasDataProp;
            this.textStr = textStr;
        }

        public Object getValue()
        {
            return value;
        }

        @Override
        public void handle(StringTextValue textValue) {
            value = textValue.getText();
        }

        @Override
        public void handle(NumberTextValue textValue) {
            if (hasDataProp) {
                if (textStr != null) {
                    try {
                        value = Double.parseDouble(textStr);
                    } catch (final NumberFormatException nfe) {
                        throw new JRRuntimeException(nfe);
                    }
                }
            } else {
                value = textValue.getValue();
            }
        }

        @Override
        public void handle(DateTextValue textValue) {
            if (hasDataProp) {
                if (textStr != null) {
                    try {
                        value = new Date(Long.parseLong(textStr));
                    } catch (final NumberFormatException nfe) {
                        try {
                            value = isoDateFormat.parse(textStr);
                        } catch (final ParseException pe) {
                            throw new JRRuntimeException(pe);
                        }
                    }
                }
            } else {
                value = textValue.getValue();
            }
        }

        @Override
        public void handle(BooleanTextValue textValue) {
            value = hasDataProp ? Boolean.valueOf(textStr) : textValue.getValue();
        }

    }
}
