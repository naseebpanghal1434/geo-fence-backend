package com.tse.core_application.controller.policy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tse.core_application.dto.policy.PolicyCreateRequest;
import com.tse.core_application.dto.policy.PolicyUpdateRequest;
import com.tse.core_application.entity.policy.AttendancePolicy;
import com.tse.core_application.repository.policy.AttendancePolicyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
public class GeoFencingPolicyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AttendancePolicyRepository policyRepository;

    @BeforeEach
    public void setUp() {
        policyRepository.deleteAll();
    }

    @Test
    public void testCreatePolicy_Success_ReturnsCreated() throws Exception {
        Long orgId = 100L;
        PolicyCreateRequest request = new PolicyCreateRequest();
        request.setCreatedBy(123L);

        mockMvc.perform(post("/api/orgs/" + orgId + "/createGeoFencingPolicy")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("CREATED")))
                .andExpect(jsonPath("$.orgId", is(orgId.intValue())))
                .andExpect(jsonPath("$.policyId", notNullValue()))
                .andExpect(jsonPath("$.defaultsApplied", is(true)));
    }

    @Test
    public void testCreatePolicy_Idempotent_ReturnsNoop() throws Exception {
        Long orgId = 101L;

        // First create
        mockMvc.perform(post("/api/orgs/" + orgId + "/createGeoFencingPolicy")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("CREATED")));

        // Second create (should be NOOP)
        mockMvc.perform(post("/api/orgs/" + orgId + "/createGeoFencingPolicy")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("NOOP")))
                .andExpect(jsonPath("$.orgId", is(orgId.intValue())))
                .andExpect(jsonPath("$.policyId", notNullValue()));
    }

    @Test
    public void testGetPolicy_NotFound_Returns404() throws Exception {
        Long orgId = 999L;

        mockMvc.perform(get("/api/orgs/" + orgId + "/getGeoFencePolicy"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("POLICY_NOT_FOUND")))
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.title", is("Policy not found")));
    }

    @Test
    public void testGetPolicy_Success_ReturnsFullPolicy() throws Exception {
        Long orgId = 102L;

        // Create policy first
        mockMvc.perform(post("/api/orgs/" + orgId + "/createGeoFencingPolicy")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isCreated());

        // Get policy
        mockMvc.perform(get("/api/orgs/" + orgId + "/getGeoFencePolicy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.policyId", notNullValue()))
                .andExpect(jsonPath("$.orgId", is(orgId.intValue())))
                .andExpect(jsonPath("$.isActive", is(false)))
                .andExpect(jsonPath("$.fenceRadiusM", is(150)))
                .andExpect(jsonPath("$.accuracyGateM", is(80)))
                .andExpect(jsonPath("$.outsideFencePolicy", is("WARN")))
                .andExpect(jsonPath("$.integrityPosture", is("WARN")));
    }

    @Test
    public void testUpdatePolicy_InvalidFenceRadius_Returns400() throws Exception {
        Long orgId = 103L;

        // Create policy first
        mockMvc.perform(post("/api/orgs/" + orgId + "/createGeoFencingPolicy")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isCreated());

        // Update with invalid fenceRadiusM (< 30)
        PolicyUpdateRequest updateRequest = new PolicyUpdateRequest();
        updateRequest.setFenceRadiusM(25);
        updateRequest.setAccuracyGateM(80);
        updateRequest.setIsActive(true);
        updateRequest.setOutsideFencePolicy(AttendancePolicy.OutsideFencePolicy.BLOCK);
        updateRequest.setIntegrityPosture(AttendancePolicy.IntegrityPosture.WARN);
        updateRequest.setAllowCheckinBeforeStartMin(20);
        updateRequest.setLateCheckinAfterStartMin(30);
        updateRequest.setAllowCheckoutBeforeEndMin(15);
        updateRequest.setMaxCheckoutAfterEndMin(60);
        updateRequest.setNotifyBeforeShiftStartMin(10);
        updateRequest.setCooldownSeconds(120);
        updateRequest.setMaxSuccessfulPunchesPerDay(6);
        updateRequest.setMaxFailedPunchesPerDay(3);
        updateRequest.setMaxWorkingHoursPerDay(10);
        updateRequest.setDwellInMin(3);
        updateRequest.setDwellOutMin(5);
        updateRequest.setAutoOutEnabled(false);
        updateRequest.setAutoOutDelayMin(5);
        updateRequest.setUndoWindowMin(5);

        mockMvc.perform(put("/api/orgs/" + orgId + "/updateGeoFencePolicy")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_FAILED")))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.detail", containsString("fenceRadiusM must be >= 30")));
    }

    @Test
    public void testUpdatePolicy_Success_ReturnsUpdated() throws Exception {
        Long orgId = 104L;

        // Create policy first
        mockMvc.perform(post("/api/orgs/" + orgId + "/createGeoFencingPolicy")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isCreated());

        // Update with valid data
        PolicyUpdateRequest updateRequest = new PolicyUpdateRequest();
        updateRequest.setFenceRadiusM(180);
        updateRequest.setAccuracyGateM(70);
        updateRequest.setIsActive(true);
        updateRequest.setOutsideFencePolicy(AttendancePolicy.OutsideFencePolicy.BLOCK);
        updateRequest.setIntegrityPosture(AttendancePolicy.IntegrityPosture.BLOCK);
        updateRequest.setAllowCheckinBeforeStartMin(25);
        updateRequest.setLateCheckinAfterStartMin(35);
        updateRequest.setAllowCheckoutBeforeEndMin(20);
        updateRequest.setMaxCheckoutAfterEndMin(70);
        updateRequest.setNotifyBeforeShiftStartMin(15);
        updateRequest.setCooldownSeconds(150);
        updateRequest.setMaxSuccessfulPunchesPerDay(8);
        updateRequest.setMaxFailedPunchesPerDay(5);
        updateRequest.setMaxWorkingHoursPerDay(12);
        updateRequest.setDwellInMin(5);
        updateRequest.setDwellOutMin(7);
        updateRequest.setAutoOutEnabled(true);
        updateRequest.setAutoOutDelayMin(10);
        updateRequest.setUndoWindowMin(10);
        updateRequest.setUpdatedBy(456L);

        mockMvc.perform(put("/api/orgs/" + orgId + "/updateGeoFencePolicy")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UPDATED")))
                .andExpect(jsonPath("$.policyId", notNullValue()))
                .andExpect(jsonPath("$.orgId", is(orgId.intValue())))
                .andExpect(jsonPath("$.updatedAt", notNullValue()));

        // Verify changes
        mockMvc.perform(get("/api/orgs/" + orgId + "/getGeoFencePolicy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fenceRadiusM", is(180)))
                .andExpect(jsonPath("$.accuracyGateM", is(70)))
                .andExpect(jsonPath("$.isActive", is(true)))
                .andExpect(jsonPath("$.outsideFencePolicy", is("BLOCK")))
                .andExpect(jsonPath("$.integrityPosture", is("BLOCK")));
    }

    @Test
    public void testUpdatePolicy_PolicyNotFound_Returns404() throws Exception {
        Long orgId = 999L;

        PolicyUpdateRequest updateRequest = new PolicyUpdateRequest();
        updateRequest.setFenceRadiusM(180);
        updateRequest.setAccuracyGateM(70);
        updateRequest.setIsActive(true);
        updateRequest.setOutsideFencePolicy(AttendancePolicy.OutsideFencePolicy.WARN);
        updateRequest.setIntegrityPosture(AttendancePolicy.IntegrityPosture.WARN);
        updateRequest.setAllowCheckinBeforeStartMin(20);
        updateRequest.setLateCheckinAfterStartMin(30);
        updateRequest.setAllowCheckoutBeforeEndMin(15);
        updateRequest.setMaxCheckoutAfterEndMin(60);
        updateRequest.setNotifyBeforeShiftStartMin(10);
        updateRequest.setCooldownSeconds(120);
        updateRequest.setMaxSuccessfulPunchesPerDay(6);
        updateRequest.setMaxFailedPunchesPerDay(3);
        updateRequest.setMaxWorkingHoursPerDay(10);
        updateRequest.setDwellInMin(3);
        updateRequest.setDwellOutMin(5);
        updateRequest.setAutoOutEnabled(false);
        updateRequest.setAutoOutDelayMin(5);
        updateRequest.setUndoWindowMin(5);

        mockMvc.perform(put("/api/orgs/" + orgId + "/updateGeoFencePolicy")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("POLICY_NOT_FOUND")))
                .andExpect(jsonPath("$.status", is(404)));
    }
}
