package com.bioinformatics.exportservice.mapper;

import com.bioinformatics.exportservice.dto.ExportPipelineCreateRequest;
import com.bioinformatics.exportservice.dto.ExportPipelineResponse;
import com.bioinformatics.exportservice.entity.ExportPipeline;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.ArrayList;
import java.util.List;

/**
 * MapStruct mapper for {@link ExportPipeline} entity and DTOs.
 *
 * <p>Handles:
 * <ul>
 *   <li>Entity → Response DTO: converts JSONB fieldSchema to List&lt;String&gt;
 *   <li>Request DTO → Entity: creates new entity from request, sets defaults
 * </ul>
 *
 * <p>Note: fieldSchema in the entity is a JsonNode (JSONB), but the DTO uses List&lt;String&gt;
 * for simpler API contracts. Conversion uses Jackson to parse/construct JSON arrays.
 */

@Mapper(componentModel = "spring")
public interface ExportPipelineMapper {

    ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Custom mapping: JsonNode -> List<String>
     */
    @Named("mapJsonNodeToList")
    static List<String> mapJsonNodeToList(JsonNode jsonNode) {
        if (jsonNode == null || !jsonNode.isArray()) {
            return new ArrayList<>();
        }

        List<String> list = new ArrayList<>();
        jsonNode.forEach(node -> list.add(node.asText()));
        return list;
    }

    /**
     * Custom mapping: List<String> -> JsonNode
     */
    @Named("mapListToJsonNode")
    static JsonNode mapListToJsonNode(List<String> list) {
        if (list == null) {
            return MAPPER.createArrayNode();
        }
        return MAPPER.valueToTree(list);
    }

    /**
     * Converts entity to response DTO.
     * MapStruct will automatically use the default method below for fieldSchema (JsonNode -> List<String>).
     */
    @Mapping(target = "filter", source = "filterJson")
    @Mapping(target = "fieldSchema", source = "fieldSchema", qualifiedByName = "mapJsonNodeToList")
    ExportPipelineResponse toDto(ExportPipeline entity);

    /**
     * Converts request DTO to entity.
     * Sets userId to the provided value and maps specific fields.
     *
     * @param request the create request DTO
     * @param userId  the username of the requesting user
     * @return a new ExportPipeline entity with request data and defaults
     */
    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "filterJson", source = "filter")
    @Mapping(target = "fieldSchema", source = "fieldSchema", qualifiedByName = "mapListToJsonNode")
    // Ignore internal fields to prevent MapStruct "Unmapped target property" warnings
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true) // Handled by @Builder.Default (QUEUED)
    @Mapping(target = "estimatedRows", ignore = true)
    @Mapping(target = "actualRows", ignore = true)
    @Mapping(target = "filePath", ignore = true)
    @Mapping(target = "fileSizeBytes", ignore = true)
    @Mapping(target = "errorMessage", ignore = true)
    @Mapping(target = "jobExecutionId", ignore = true)
    @Mapping(target = "createdAt", ignore = true) // Handled by @PrePersist
    @Mapping(target = "startedAt", ignore = true)
    @Mapping(target = "completedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "durationMs", ignore = true)
    ExportPipeline toEntity(ExportPipelineCreateRequest request, String userId);
}


