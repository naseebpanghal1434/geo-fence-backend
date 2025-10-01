package com.tse.core_application.dto.userfence;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response containing all effective fences for a user in an organization.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserFencesResponse {

    @JsonProperty("orgId")
    private Long orgId;

    @JsonProperty("accountId")
    private Long accountId;

    @JsonProperty("defaultFenceIdForUser")
    private Long defaultFenceIdForUser;

    @JsonProperty("fences")
    private List<EffectiveFenceDto> fences = new ArrayList<>();

    @JsonProperty("counts")
    private Counts counts;
}
