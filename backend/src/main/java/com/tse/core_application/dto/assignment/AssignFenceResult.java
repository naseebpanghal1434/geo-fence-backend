package com.tse.core_application.dto.assignment;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.OffsetDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AssignFenceResult {
    private Long fenceId;
    private AssignmentSummary summary;
    private List<EntityResult> results;
    private OffsetDateTime updatedAt;
    private Long updatedBy;
}
