package com.tse.core_application.dto.assignment;

import com.fasterxml.jackson.annotation.JsonInclude;

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
public class EntityResult {
    private Integer entityTypeId;
    private Long entityId;
    private String action;
    private Boolean defaultForEntity;
    private List<Long> fenceIds;
    private String message;
}
