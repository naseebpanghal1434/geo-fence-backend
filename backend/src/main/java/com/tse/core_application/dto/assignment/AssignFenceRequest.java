package com.tse.core_application.dto.assignment;

import javax.validation.constraints.NotNull;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AssignFenceRequest {

    @NotNull
    private Long fenceId;

    private List<EntityActionItem> add;

    private List<EntityActionItem> remove;

    private Long updatedBy;
}
