package org.openapitools.codegen.languages;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.openapitools.codegen.model.ModelsMap;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;

public class SchemaGenerator {

    public static final String REFERENCE_KEY = "$ref";
    public static final String PROPERTIES_KEY = "properties";
    private final String outputDir;
    private final String schemaRegistry;

    public SchemaGenerator(String outputDir, String schemaRegistry) {
        this.outputDir = outputDir;
        this.schemaRegistry = schemaRegistry;
    }

    /**
     * Doesn't follow the flow of the normal code generator due to it being easier to generate json this way and not via mustache files
     *
     * @param objs
     */
    public void writeSchemata(Map<String, ModelsMap> objs, Map<String,String> fieldRenames) {
        Map<String, JsonNode> models = new HashMap<>();
        ObjectMapper mapper = new ObjectMapper();
        objs
                .entrySet()
                .stream()
                .forEach(entry -> {
                    try {
                        models.put(entry.getKey(), mapper.readTree(entry.getValue().getModels().get(0).getModel().getModelJson()));
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                });

        models.values().forEach(model->fieldRenames.forEach((oldKey,newKey)->replaceFieldName(oldKey, newKey, model)));

        models.entrySet().stream().forEach(entry -> {
            ModelsMap basedModelsMap = objs.get(entry.getKey());
            embedReferences(entry.getValue(), models, (String) basedModelsMap.get("packageName"));
        });
        models.entrySet().stream().filter(entry -> isEntity(entry.getValue())).forEach(entry -> {
            Map<String, JsonNode> descriptionOfModel = extractDescription(objs.get(entry.getKey()));
            Optional.ofNullable(entry.getValue())
                    .filter(e -> e instanceof ObjectNode)
                    .map(e -> (ObjectNode) e)
                    .ifPresent(e -> {
                        e.setAll(descriptionOfModel);
                        e.setAll(schemaGenerics());
                    });

            try {
                Path path = Path.of(outputDir, "schema/", entry.getKey() + ".json");
                Files.createDirectories(path.getParent());
                Files.writeString(path, entry.getValue().toPrettyString(), StandardOpenOption.CREATE);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void replaceFieldName(String oldName, String newName, JsonNode json) {
        List<JsonNode> parentsOfOldFieldName = searchForParentOfEntity(json, oldName);
        parentsOfOldFieldName.stream().forEach(parent -> {
            if (parent instanceof ObjectNode) {
                ObjectNode baseNode = (ObjectNode) parent;
                JsonNode oldNode = baseNode.get(oldName);
                if (oldNode != null) {
                    baseNode.set(newName, oldNode);
                    baseNode.remove(oldName);
                }
            }
        });
    }

    private String generateSchemaUrl(String packageName, String modelName) {
        return schemaRegistry + "/" + packageName + "/" + modelName;
    }

    private void embedReferences(JsonNode model, Map<String, JsonNode> models, String packageName) {
        List<JsonNode> nodesToReplace = searchForParentOfEntity(model, REFERENCE_KEY);
        nodesToReplace.forEach(nodeToReplace -> {
            if (nodeToReplace != null) {
                if (nodeToReplace instanceof ObjectNode) {
                    ObjectNode parentNode = (ObjectNode) nodeToReplace;
                    String reference = parentNode.get(REFERENCE_KEY).asText();
                    String modelNameToInsert = reference.replace("#/components/schemas/", "");
                    JsonNode modelToInsert = models.get(modelNameToInsert);
                    if (isEntity(modelToInsert)) {
                        //add reference
                        parentNode.replace(REFERENCE_KEY, JsonNodeFactory.instance.textNode(generateSchemaUrl(packageName, modelNameToInsert)));
                    } else {
                        if (isEnum(modelToInsert)) {
                            parentNode.remove(REFERENCE_KEY);
                            modelToInsert.fields().forEachRemaining(entry -> parentNode.put(entry.getKey(), entry.getValue()));
                        } else {
                            parentNode.put(PROPERTIES_KEY, modelToInsert);
                            parentNode.remove(REFERENCE_KEY);
                        }
                    }
                }
            }
        });
    }

    private Map<String, JsonNode> schemaGenerics() {
        return Map.of("$schema", JsonNodeFactory.instance.textNode("http://json-schema.org/schema#"),
                "modelTags", JsonNodeFactory.instance.textNode("")); // No tags for schemas, only for endpoints
    }

    private Map<String, JsonNode> extractDescription(ModelsMap currentEntity) {
        Map<String, JsonNode> answer = new HashMap<>();
        Optional
                .ofNullable(currentEntity.get("classname"))
                .filter(a -> a instanceof String)
                .map(a -> (String) a)
                .map(JsonNodeFactory.instance::textNode)
                .ifPresent(a -> answer.put("title", a));
        Optional
                .ofNullable(currentEntity.get("appVersion"))
                .filter(a -> a instanceof String)
                .map(a -> (String) a)
                .map(JsonNodeFactory.instance::textNode)
                .ifPresent(a -> answer.put("$schemaVersion", a));
        answer.put("$id", JsonNodeFactory.instance.textNode(generateSchemaUrl((String) currentEntity.get("packageName"), (String) currentEntity.get("classname")) + "/schema.json"));
        return answer;
    }

    private List<JsonNode> searchForParentOfEntity(JsonNode node, String entityName) {
        if (node == null) {
            return List.of();
        }
        if (node.has(entityName)) {
            return List.of(node);
        }
        if (!node.isContainerNode()) {
            return List.of();
        }
        List<JsonNode> nodesFound = new ArrayList<>();
        for (JsonNode child : node) {
            if (child.isContainerNode()) {
                List<JsonNode> childResult = searchForParentOfEntity(child, entityName);
                childResult.stream().filter(foundChild -> !foundChild.isMissingNode()).forEach(nodesFound::add);
            }
        }
        return nodesFound;
    }

    public boolean isEntity(JsonNode model) {
        return Optional.ofNullable(model)
                .filter(m -> m.has(PROPERTIES_KEY))
                .map(m -> m.get(PROPERTIES_KEY))
                .filter(m -> m.has("id"))
                .isPresent();
    }

    private boolean isEnum(JsonNode model) {
        return Optional.ofNullable(model)
                .filter(m -> m.has("enum"))
                .isPresent();
    }
}
