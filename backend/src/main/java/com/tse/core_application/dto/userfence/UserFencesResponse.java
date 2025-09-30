package com.tse.core_application.dto.userfence;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * Response containing all effective fences for a user in an organization.
 */
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

    public UserFencesResponse() {
    }

    public Long getOrgId() {
        return orgId;
    }

    public void setOrgId(Long orgId) {
        this.orgId = orgId;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public Long getDefaultFenceIdForUser() {
        return defaultFenceIdForUser;
    }

    public void setDefaultFenceIdForUser(Long defaultFenceIdForUser) {
        this.defaultFenceIdForUser = defaultFenceIdForUser;
    }

    public List<EffectiveFenceDto> getFences() {
        return fences;
    }

    public void setFences(List<EffectiveFenceDto> fences) {
        this.fences = fences;
    }

    public Counts getCounts() {
        return counts;
    }

    public void setCounts(Counts counts) {
        this.counts = counts;
    }
}
