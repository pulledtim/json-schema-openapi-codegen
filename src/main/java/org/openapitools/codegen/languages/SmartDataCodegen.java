package org.openapitools.codegen.languages;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.servers.Server;
import org.openapitools.codegen.*;
import org.openapitools.codegen.languages.features.BeanValidationFeatures;
import org.openapitools.codegen.languages.features.OptionalFeatures;
import org.openapitools.codegen.languages.features.UseGenericResponseFeatures;
import org.openapitools.codegen.model.ModelMap;
import org.openapitools.codegen.model.ModelsMap;
import org.openapitools.codegen.utils.ModelUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.openapitools.codegen.CodegenConstants.*;

public class SmartDataCodegen extends AbstractJavaCodegen
        implements BeanValidationFeatures, UseGenericResponseFeatures, OptionalFeatures {

    public static final String INTROSPECTED = "introspected";
    public static final String DATETIME_RELAXED = "dateTimeRelaxed";
    public static final String SCHEMA_REGISTRY = "schemaRegistry";

    private static final Logger LOG = LoggerFactory.getLogger(SmartDataCodegen.class);

    private String schemaRegistry = "https://smart-data-models.github.io/data-models.test";
    private boolean useBeanValidation = true;
    private boolean useOptional = true;
    private boolean introspected = true;
    private boolean dateTimeRelaxed = true;

    private Map<String, String> fieldNameMapping = new HashMap<>();

    public SmartDataCodegen() {

        // enable the supported default-codegen features

        supportsAdditionalPropertiesWithComposedSchema = true;
        useOneOfInterfaces = true;

        cliOptions.clear();
        cliOptions.add(CliOption.newBoolean(USE_BEANVALIDATION, "Use bean validation annotations", useBeanValidation));
        cliOptions.add(CliOption.newBoolean(USE_OPTIONAL, "Use Optional<T> instead of @Nullable.", useOptional));
        cliOptions.add(CliOption.newBoolean(INTROSPECTED, "Add @Introspected to models", introspected));
        cliOptions.add(CliOption.newBoolean(DATETIME_RELAXED, "Relaxed parsing of datetimes.", dateTimeRelaxed));
        cliOptions.add(CliOption.newBoolean(OPENAPI_NULLABLE, "Enable OpenAPI Jackson Nullable", openApiNullable));
        cliOptions.add(CliOption.newString(SOURCE_FOLDER, SOURCE_FOLDER_DESC));
        cliOptions.add(CliOption.newString(SCHEMA_REGISTRY, "Url of the registry that will host the schemata"));
        cliOptions.add(CliOption.newString("testFolder", "test folder for generated code"));
        cliOptions.add(CliOption.newString("fieldNameMapping", "Field names to be replaced"));

        // there is no documentation template yet

        apiTemplateFiles.clear();
        apiDocTemplateFiles.clear();
        apiTestTemplateFiles.clear();
        modelTemplateFiles.clear();
        modelTemplateFiles.put("model.mustache", ".java");
        modelDocTemplateFiles.clear();
        modelTestTemplateFiles.clear();

        // parent flags

        additionalProperties.clear();
        additionalProperties.put(USE_BEANVALIDATION, useBeanValidation);
        additionalProperties.put(USE_OPTIONAL, useOptional);
        additionalProperties.put(INTROSPECTED, introspected);

        // add custom type mappings

        typeMapping.clear();
        typeMapping.put("object", "java.lang.Object");
        typeMapping.put("AnyType", "java.lang.Object");
        typeMapping.put("date", "java.time.LocalDate");
        typeMapping.put("DateTime", "java.time.Instant");
        typeMapping.put("array", "java.util.List");
        typeMapping.put("map", "java.util.Map");
        typeMapping.put("set", "java.util.Set");
        typeMapping.put("boolean", "java.lang.Boolean");
        typeMapping.put("string", "java.lang.String");
        typeMapping.put("int", "java.lang.Integer");
        typeMapping.put("integer", "java.lang.Integer");
        typeMapping.put("Integer", "java.lang.Integer");
        typeMapping.put("long", "java.lang.Long");
        typeMapping.put("Long", "java.lang.Long");
        typeMapping.put("float", "java.lang.Float");
        typeMapping.put("Float", "java.lang.Float");
        typeMapping.put("double", "java.lang.Double");
        typeMapping.put("Double", "java.lang.Double");
        typeMapping.put("number", "java.lang.Double");
        typeMapping.put("BigDecimal", "java.lang.Double");
        typeMapping.put("UUID", "java.util.UUID");
        typeMapping.put("URI", "java.net.URI");
        typeMapping.put("URL", "java.net.URL");
        typeMapping.put("file", "byte[]");
        typeMapping.put("ByteArray", "byte[]");
        typeMapping.put("Authentication", "io.micronaut.security.authentication.Authentication");
        typeMapping.put("MultipartBody", "io.micronaut.http.client.multipart.MultipartBody");
        typeMapping.put("fileUpload", "io.micronaut.http.multipart.CompletedFileUpload");
        typeMapping.put("asyncFileUpload", "io.micronaut.http.multipart.StreamingFileUpload");
        typeMapping.put("asyncCompletable", "reactor.core.publisher.Mono");
        typeMapping.put("asyncSingle", "reactor.core.publisher.Mono");
        typeMapping.put("asyncMaybe", "reactor.core.publisher.Mono");
        typeMapping.put("asyncFlowable", "reactor.core.publisher.Flux");
        typeMapping.put("Generated", "jakarta.annotation.Generated");
        typeMapping.put("Nullable", "io.micronaut.core.annotation.Nullable");
        typeMapping.put("Nonnull", "io.micronaut.core.annotation.NonNull");
        typeMapping.put("Inject", "jakarta.inject.Inject");
        typeMapping.put("Singleton", "jakarta.inject.Singleton");
        instantiationTypes.clear();
        instantiationTypes.put("array", "java.util.ArrayList");
        instantiationTypes.put("map", "java.util.HashMap");
        instantiationTypes.put("set", "java.util.LinkedHashSet");
        importMapping.clear();

        // allow list and file as variables

        reservedWords.remove("file");
        reservedWords.remove("list");
        reservedWords.add("authentication");
    }

    @Override
    public String getName() {
        return "smart-data";
    }

    @Override
    public void postProcess() {
    }

    @Override
    public void processOpts() {
        BiFunction<String, String, String> getOrDefault = (key,
                                                           defaultValue) -> (String) additionalProperties.computeIfAbsent(key, k -> defaultValue);

        // reuse package if other packages are not provided

        var packageName = getOrDefault.apply(CodegenConstants.PACKAGE_NAME, "changeMe");
        apiPackage = getOrDefault.apply(CodegenConstants.API_PACKAGE, packageName);
        modelPackage = getOrDefault.apply(CodegenConstants.MODEL_PACKAGE, packageName);
        invokerPackage = getOrDefault.apply(CodegenConstants.INVOKER_PACKAGE, packageName);
        additionalProperties.put("isModelImport", !apiPackage.equals(modelPackage));

        // process flags - this class

        if (additionalProperties.containsKey(USE_BEANVALIDATION)) {
            useBeanValidation = convertPropertyToBooleanAndWriteBack(USE_BEANVALIDATION);
        }
        if (additionalProperties.containsKey(USE_OPTIONAL)) {
            useOptional = convertPropertyToBooleanAndWriteBack(USE_OPTIONAL);
        }
        if (additionalProperties.containsKey(INTROSPECTED)) {
            introspected = convertPropertyToBooleanAndWriteBack(INTROSPECTED);
        }
        if (additionalProperties.containsKey(DATETIME_RELAXED)) {
            dateTimeRelaxed = convertPropertyToBooleanAndWriteBack(DATETIME_RELAXED);
        }
        if (additionalProperties.containsKey(OPENAPI_NULLABLE)) {
            openApiNullable = convertPropertyToBooleanAndWriteBack(OPENAPI_NULLABLE);
        }

        templateDir = "SmartData";
        projectFolder = getOrDefault.apply("projectFolder", "generated-sources");
        projectTestFolder = getOrDefault.apply("projectTestFolder", "generated-test-sources");
        sourceFolder = getOrDefault.apply(SOURCE_FOLDER, projectFolder + File.separator + "openapi");
        testFolder = getOrDefault.apply("testFolder", projectTestFolder + File.separator + "openapi");
        modelNameSuffix = getOrDefault.apply(MODEL_NAME_SUFFIX, modelNameSuffix);


        // add type mappings for mustache

        additionalProperties.put("type.Authentication", typeMapping.get("Authentication"));
        additionalProperties.put("type.Nullable", typeMapping.get("Nullable"));
        additionalProperties.put("type.Nonnull", typeMapping.get("Nonnull"));
        additionalProperties.put("type.Inject", typeMapping.get("Inject"));
        additionalProperties.put("type.Singleton", typeMapping.get("Singleton"));
        additionalProperties.put("type.MultipartBody", typeMapping.get("MultipartBody"));
        instantiationTypes.forEach((k, v) -> additionalProperties.put("instantiationType." + k, v));
        Optional.ofNullable(typeMapping.get("Generated"))
                .filter(type -> !type.isBlank())
                .ifPresent(type -> additionalProperties.put("type.Generated", type));

        this.fieldNameMapping.putAll(Map.of("@type", "tmForumType", "@schemaLocation", "atSchemaLocation", "@baseType", "atBaseType"));
    }

    @Override
    public CodegenOperation fromOperation(String path, String httpMethod, Operation source, List<Server> servers) {
        return super.fromOperation(path, httpMethod, source, servers);
    }

    @Override
    public CodegenResponse fromResponse(String responseCode, ApiResponse response) {
        return super.fromResponse(responseCode, response);
    }

    private boolean isReferenceToEntity(String reference, Map<String, ModelsMap> objs) {
        return Stream
                .ofNullable(reference)
                .map(path -> path.replace("#/components/schemas/", ""))
                .map(objs::get)
                .map(ModelsMap::getModels)
                .flatMap(Collection::stream)
                .map(ModelMap::getModel)
                .map(CodegenModel::getVars)
                .flatMap(Collection::stream)
                .map(CodegenProperty::getName)
                .anyMatch("id"::equals);
    }

    private void addModellingHint(Map<String, ModelsMap> objs) {
        objs
                .values()
                .stream()
                .map(ModelsMap::getModels)
                .flatMap(Collection::stream)
                .map(ModelMap::getModel)
                .map(CodegenModel::getVars)
                .flatMap(Collection::stream)
                .filter(a -> a.getRef() != null)
                .filter(a -> !a.isEnumRef)
                .filter(a -> isReferenceToEntity(a.getRef(), objs))
                .forEach(e -> {
                    e.getVendorExtensions().put("ngsi_relationship", e.dataType);
                });
        objs
                .values()
                .stream()
                .map(ModelsMap::getModels)
                .flatMap(Collection::stream)
                .map(ModelMap::getModel)
                .map(CodegenModel::getVars)
                .flatMap(Collection::stream)
                .filter(CodegenProperty::getIsArray)
                .forEach(e -> {
                    CodegenProperty items = e.getItems();
                    if (isReferenceToEntity(items.getRef(), objs)) {
                        e.getVendorExtensions().put("ngsi_relationshiplist", items.dataType);
                    }
                });
        objs
                .values()
                .stream()
                .map(ModelsMap::getModels)
                .flatMap(Collection::stream)
                .map(ModelMap::getModel)
                .filter(e -> e.getVars().stream().map(CodegenProperty::getName).anyMatch("id"::equals))
                .forEach(e -> e.getVendorExtensions().put("ngsi_entityWithId", "true"));
    }

    @Override
    public Map<String, ModelsMap> updateAllModels(Map<String, ModelsMap> objs) {
        var superObjs = super.updateAllModels(objs);

        // Create json schema
        new SchemaGenerator(outputFolder(), schemaRegistry).writeSchemata(superObjs, fieldNameMapping);

        addModellingHint(superObjs);
        // remove AllOf

        objs.entrySet().removeIf(e -> e.getKey().endsWith("_allOf"));

        var allModels = getAllModels(objs);
        for (CodegenModel model : allModels.values()) {

            // check if composed schemas for additional properties should be handled and apply to the map if so.

            if (supportsAdditionalPropertiesWithComposedSchema && model.getAdditionalProperties() != null) {
                model.getVendorExtensions().put("additionalPropertiesMap", Map.of(
                        "keyType", "java.lang.String",
                        "valueType", model.getAdditionalProperties().getDataType()));
            }

            // handle discriminator

            var discriminator = model.discriminator;
            if (discriminator == null) {
                continue;
            }

            // remove discriminator type

            model.vars.stream()
                    .filter(property -> property.getName().equals(discriminator.getPropertyName()))
                    .findAny().ifPresent(property -> {
                        discriminator.setPropertyType(property.getDataType());
                        model.vars.remove(property);
                    });

            // add discriminator value to submodel

            for (var mappedModel : discriminator.getMappedModels()) {

                var subModel = allModels.get(mappedModel.getModelName());
                if (subModel == null) {
                    LOG.warn("{} - model in discriminator {} with name {} not found",
                            model.name, discriminator.getPropertyName(), mappedModel.getModelName());
                    continue;
                }
                if (subModel.parentModel == null) {
                    subModel.parentModel = model;
                    subModel.parent = model.getClassname();
                    LOG.warn("{} added missing sub model {}", model.name, subModel.name);
                }
                subModel.vars.removeIf(property -> property.getName().equals(discriminator.getPropertyName()));

                var extensions = subModel.getVendorExtensions();
                extensions.put("discriminatorPropertyGetter", discriminator.getPropertyGetter());
                extensions.put("discriminatorPropertyType", discriminator.getPropertyType());
                switch (discriminator.getPropertyType()) {
                    case "java.lang.String":
                        extensions.put("discriminatorPropertyValue", '"' + mappedModel.getMappingName() + '"');
                        break;
                    case "java.lang.Long":
                    case "java.lang.Integer":
                    case "java.lang.Double":
                    case "java.lang.Float":
                        extensions.put("discriminatorPropertyValue", mappedModel.getMappingName());
                        break;
                    default:
                        extensions.put("discriminatorPropertyValue", discriminator.getPropertyType() + "."
                                + toEnumVarName(mappedModel.getMappingName(), ""));
                }
            }
        }

        return superObjs;
    }

    @Override
    public void postProcessParameter(CodegenParameter parameter) {
    }

    @Override
    public void postProcessModelProperty(CodegenModel model, CodegenProperty property) {
        super.postProcessModelProperty(model, property);
    }

    @Override
    public boolean isDataTypeString(String dataType) {
        return List.of("java.lang.String", "String").contains(dataType);
    }

    // enum

    @Override
    public String toEnumName(CodegenProperty property) {
        return property.nameInCamelCase;
    }

    @Override
    public String toEnumVarName(String value, String datatype) {
        return super.toEnumVarName(value, datatype.replace("java.lang.", ""));
    }

    @Override
    public String toEnumValue(String value, String datatype) {
        if (List.of("int", "Integer", "java.lang.Integer").contains(datatype)) {
            return value;
        }
        if (List.of("long", "Long", "java.lang.Long").contains(datatype)) {
            return value + "L";
        }
        if (List.of("float", "Float", "java.lang.Float").contains(datatype)) {
            return value + "F";
        }
        if (List.of("double", "Double", "java.lang.Double").contains(datatype)) {
            return value + "D";
        }
        return super.toEnumValue(value, datatype);
    }

    // setter

    @Override
    public void setUseBeanValidation(boolean useBeanValidation) {
        this.useBeanValidation = useBeanValidation;
    }

    @Override
    public void setUseGenericResponse(boolean useGenericResponse) {
    }

    @Override
    public void setUseOptional(boolean useOptional) {
        this.useOptional = useOptional;
    }

    @Override
    public String toDefaultValue(Schema schema) {
        if (ModelUtils.isGenerateAliasAsModel() && schema.get$ref() != null) {
            return "new " + getSchemaType(schema) + "()";
        }
        return super.toDefaultValue(schema);
    }

    @Override
    public String toExampleValue(Schema schema) {

        // first choice: use the example, provided from the spec
        // second choice: use the default provided by the spec
        // special handling for enum: if no example or default is provided, use the first value
        Optional<String> value = Optional
                .ofNullable(schema.getExample())
                .or(() -> Optional.ofNullable(schema.getDefault()))
                .or(() -> Optional.ofNullable(schema.getEnum()).flatMap(e -> e.stream().findFirst()))
                .map(Object::toString);

        // third choice: set type-specific default examples

        if (ModelUtils.isBooleanSchema(schema)) {
            return value.orElse("false");
        }
        if (ModelUtils.isLongSchema(schema)) {
            return value.orElse("100L") + "L";
        }
        if (ModelUtils.isFloatSchema(schema)) {
            return value.orElse("12.34") + "F";
        }
        if (ModelUtils.isDoubleSchema(schema)) {
            return value.orElse("43.21") + "D";
        }
        if (ModelUtils.isIntegerSchema(schema) || ModelUtils.isShortSchema(schema)) {
            return value.orElse("12");
        }
        if (ModelUtils.isNumberSchema(schema)) {
            return value.map(v -> "java.lang.Number.valueOf(\"" + v + "\")").orElse("12.34");
        }

        if (ModelUtils.isDateSchema(schema)) {
            if ("java.time.LocalDate".equals(typeMapping.get(schema.getType()))) {
                return "java.time.LocalDate." + value.map(v -> "parse(\"" + v + "\")").orElse("now()");
            } else {
                // we cannot provide a valid example for all possible date types
                return "null";
            }
        }
        if (ModelUtils.isDateTimeSchema(schema)) {
            if ("java.time.Instant".equals(typeMapping.get(schema.getType()))) {
                return "java.time.Instant." + value.map(v -> "parse(\"" + v + "\")").orElse("now()");
            } else {
                // we cannot provide a valid example for all possible date types
                return "null";
            }
        }
        if (ModelUtils.isByteArraySchema(schema) || ModelUtils.isBinarySchema(schema)) {
            return value.orElse("\"byteArray\".getBytes()");
        }
        if (ModelUtils.isFileSchema(schema)) {
            return value.orElse("\"myFile\".getBytes()");
        }
        if (ModelUtils.isUUIDSchema(schema)) {
            return "java.util.UUID." + value.map(v -> "fromString(\"" + v + "\")").orElse("randomUUID()");
        }
        if (ModelUtils.isURISchema(schema)) {
            return "java.net.URI.create(\"" + value.orElse("my:uri") + "\")";
        }
        if (ModelUtils.isEmailSchema(schema)) {
            return '"' + value.orElse("mail@example.org") + '"';
        }
        if (ModelUtils.isStringSchema(schema)) {
            return '"' + value.orElse("string") + '"';
        }

        if (ModelUtils.isMapSchema(schema)) {
            return "java.util.Map." + value
                    .map(v -> (Map<?, ?>) new org.yaml.snakeyaml.Yaml().loadAs(v, Map.class))
                    .filter(map -> !map.isEmpty())
                    .map(map -> map.entrySet().stream()
                            .map(e -> "new java.util.AbstractMap.SimpleEntry(\""
                                    + e.getKey() + "\", \"" + e.getValue() + "\")")
                            .collect(Collectors.joining(", ")))
                    .map(map -> "ofEntries(" + map + ")")
                    .orElse("of()");
        }
        if (ModelUtils.isSet(schema)) {
            return "java.util.Set.of(" + value.map(v -> v.substring(1, v.length() - 1)).orElse("") + ")";
        }
        if (ModelUtils.isArraySchema(schema)) {
            return "java.util.List.of(" + value.map(v -> v.substring(1, v.length() - 1)).orElse("") + ")";
        }

        // if no example can be generated, leave it null.
        return "null";
    }
}
