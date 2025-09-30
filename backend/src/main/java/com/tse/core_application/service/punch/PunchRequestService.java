package com.tse.core_application.service.punch;

import com.tse.core_application.constants.EntityTypes;
import com.tse.core_application.dto.punch.PunchRequestCreateDto;
import com.tse.core_application.dto.punch.PunchRequestViewDto;
import com.tse.core_application.entity.punch.PunchRequest;
import com.tse.core_application.exception.ProblemException;
import com.tse.core_application.repository.punch.PunchRequestRepository;
import com.tse.core_application.service.membership.MembershipProvider;
import com.tse.core_application.service.policy.PolicyGate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing punch requests.
 */
@Service
public class PunchRequestService {

    private static final Logger logger = LoggerFactory.getLogger(PunchRequestService.class);

    private final PunchRequestRepository punchRequestRepository;
    private final MembershipProvider membershipProvider;
    private final PolicyGate policyGate;

    @Value("${attendance.punch.max-past-skew-minutes:5}")
    private int maxPastSkewMinutes;

    @Value("${attendance.punch.max-future-days:30}")
    private int maxFutureDays;

    public PunchRequestService(PunchRequestRepository punchRequestRepository,
                               MembershipProvider membershipProvider,
                               PolicyGate policyGate) {
        this.punchRequestRepository = punchRequestRepository;
        this.membershipProvider = membershipProvider;
        this.policyGate = policyGate;
    }

    @Transactional
    public PunchRequestViewDto createPunchRequest(long orgId, PunchRequestCreateDto dto) {
        // Check policy active
        policyGate.assertPolicyActive(orgId);

        // Validate entity type
        if (!EntityTypes.isValid(dto.getEntityTypeId())) {
            throw new ProblemException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_FAILED",
                    "Invalid entity type",
                    "entityTypeId must be one of: 1 (USER), 2 (ORG), 4 (PROJECT), 5 (TEAM)"
            );
        }

        // Validate entity ID
        if (dto.getEntityId() == null || dto.getEntityId() <= 0) {
            throw new ProblemException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_FAILED",
                    "Invalid entity ID",
                    "entityId must be positive"
            );
        }

        // Validate respond within minutes
        if (dto.getRespondWithinMinutes() == null || dto.getRespondWithinMinutes() <= 0) {
            throw new ProblemException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_FAILED",
                    "Invalid respond time",
                    "respondWithinMinutes must be positive"
            );
        }

        // Validate requested datetime
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime requestedTime = dto.getRequestedDateTime().withOffsetSameInstant(ZoneOffset.UTC);

        // Check not too far in the past
        OffsetDateTime minAllowedPast = now.minusMinutes(maxPastSkewMinutes);
        if (requestedTime.isBefore(minAllowedPast)) {
            throw new ProblemException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_FAILED",
                    "Requested time too far in past",
                    "requestedDateTime cannot be more than " + maxPastSkewMinutes + " minutes in the past"
            );
        }

        // Check not too far in the future
        OffsetDateTime maxAllowedFuture = now.plusDays(maxFutureDays);
        if (requestedTime.isAfter(maxAllowedFuture)) {
            throw new ProblemException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_FAILED",
                    "Requested time too far in future",
                    "requestedDateTime cannot be more than " + maxFutureDays + " days in the future"
            );
        }

        // Create entity
        PunchRequest request = new PunchRequest();
        request.setOrgId(orgId);
        request.setEntityTypeId(dto.getEntityTypeId());
        request.setEntityId(dto.getEntityId());
        request.setRequesterAccountId(dto.getRequesterAccountId());
        request.setRequestedDatetime(requestedTime);
        request.setRespondWithinMinutes(dto.getRespondWithinMinutes());
        request.setExpiresAt(requestedTime.plusMinutes(dto.getRespondWithinMinutes()));
        request.setState(PunchRequest.State.PENDING);

        PunchRequest saved = punchRequestRepository.save(request);

        logger.info("Created punch request {} for org {} targeting {}/{} by requester {}",
                saved.getId(), orgId, dto.getEntityTypeId(), dto.getEntityId(), dto.getRequesterAccountId());

        return toViewDto(saved, now, Collections.emptyList());
    }

    public List<PunchRequestViewDto> getPendingRequestsForAccounts(long orgId, List<Long> accountIds) {
        if (accountIds == null || accountIds.isEmpty()) {
            return Collections.emptyList();
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        // Expand memberships for each account
        Map<Long, Set<EntityRef>> accountToEntities = new HashMap<>();
        for (Long accountId : accountIds) {
            accountToEntities.put(accountId, expandMembership(orgId, accountId));
        }

        // Collect all requests
        List<PunchRequest> allRequests = new ArrayList<>();

        // Group entities by type to minimize queries
        Map<Integer, Set<Long>> entitiesByType = new HashMap<>();
        for (Set<EntityRef> entities : accountToEntities.values()) {
            for (EntityRef ref : entities) {
                entitiesByType.computeIfAbsent(ref.entityTypeId, k -> new HashSet<>()).add(ref.entityId);
            }
        }

        // Query for each entity type
        for (Map.Entry<Integer, Set<Long>> entry : entitiesByType.entrySet()) {
            List<PunchRequest> requests = punchRequestRepository.findPendingForEntities(
                    orgId, now, entry.getKey(), entry.getValue()
            );
            allRequests.addAll(requests);
        }

        // De-duplicate and map to DTOs with account associations
        Map<Long, PunchRequestViewDto> requestMap = new HashMap<>();
        for (PunchRequest req : allRequests) {
            if (!requestMap.containsKey(req.getId())) {
                // Find which accounts this request applies to
                List<Long> appliesToAccounts = new ArrayList<>();
                for (Long accountId : accountIds) {
                    Set<EntityRef> entities = accountToEntities.get(accountId);
                    if (entities.stream().anyMatch(e -> e.matches(req.getEntityTypeId(), req.getEntityId()))) {
                        appliesToAccounts.add(accountId);
                    }
                }
                requestMap.put(req.getId(), toViewDto(req, now, appliesToAccounts));
            }
        }

        return new ArrayList<>(requestMap.values());
    }

    public PunchRequestViewDto getPunchRequestById(long orgId, long requestId) {
        Optional<PunchRequest> requestOpt = punchRequestRepository.findByIdAndOrgId(requestId, orgId);
        if (!requestOpt.isPresent()) {
            throw new ProblemException(
                    HttpStatus.NOT_FOUND,
                    "REQUEST_NOT_FOUND",
                    "Punch request not found",
                    "Punch request " + requestId + " not found in org " + orgId
            );
        }

        PunchRequest request = requestOpt.get();

        // Verify no cross-org leakage
        if (!Objects.equals(request.getOrgId(), orgId)) {
            throw new ProblemException(
                    HttpStatus.CONFLICT,
                    "CROSS_ORG_MISMATCH",
                    "Cross-organization mismatch",
                    "Request belongs to different organization"
            );
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        return toViewDto(request, now, Collections.emptyList());
    }

    public List<PunchRequestViewDto> getPendingRequestHistory(long orgId, OffsetDateTime from, OffsetDateTime to, List<Long> accountIds) {
        if (accountIds == null || accountIds.isEmpty()) {
            return Collections.emptyList();
        }

        // Default to today if not specified
        if (from == null) {
            from = OffsetDateTime.now(ZoneOffset.UTC).withHour(0).withMinute(0).withSecond(0).withNano(0);
        }
        if (to == null) {
            to = from.plusDays(1);
        }

        // Expand memberships for each account
        Map<Long, Set<EntityRef>> accountToEntities = new HashMap<>();
        for (Long accountId : accountIds) {
            accountToEntities.put(accountId, expandMembership(orgId, accountId));
        }

        // Collect all requests
        List<PunchRequest> allRequests = new ArrayList<>();

        // Group entities by type
        Map<Integer, Set<Long>> entitiesByType = new HashMap<>();
        for (Set<EntityRef> entities : accountToEntities.values()) {
            for (EntityRef ref : entities) {
                entitiesByType.computeIfAbsent(ref.entityTypeId, k -> new HashSet<>()).add(ref.entityId);
            }
        }

        // Query for each entity type
        for (Map.Entry<Integer, Set<Long>> entry : entitiesByType.entrySet()) {
            List<PunchRequest> requests = punchRequestRepository.findHistoryForEntities(
                    orgId, from, to, entry.getKey(), entry.getValue()
            );
            allRequests.addAll(requests);
        }

        // De-duplicate and map to DTOs
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        Map<Long, PunchRequestViewDto> requestMap = new HashMap<>();
        for (PunchRequest req : allRequests) {
            if (!requestMap.containsKey(req.getId())) {
                // Find which accounts this request applies to
                List<Long> appliesToAccounts = new ArrayList<>();
                for (Long accountId : accountIds) {
                    Set<EntityRef> entities = accountToEntities.get(accountId);
                    if (entities.stream().anyMatch(e -> e.matches(req.getEntityTypeId(), req.getEntityId()))) {
                        appliesToAccounts.add(accountId);
                    }
                }
                requestMap.put(req.getId(), toViewDto(req, now, appliesToAccounts));
            }
        }

        return new ArrayList<>(requestMap.values());
    }

    private Set<EntityRef> expandMembership(long orgId, long accountId) {
        Set<EntityRef> entities = new HashSet<>();

        // USER
        entities.add(new EntityRef(EntityTypes.USER, accountId));

        // TEAM
        List<Long> teamIds = membershipProvider.listTeamsForUser(orgId, accountId);
        teamIds.forEach(teamId -> entities.add(new EntityRef(EntityTypes.TEAM, teamId)));

        // PROJECT
        List<Long> projectIds = membershipProvider.listProjectsForUser(orgId, accountId);
        projectIds.forEach(projectId -> entities.add(new EntityRef(EntityTypes.PROJECT, projectId)));

        // ORG
        entities.add(new EntityRef(EntityTypes.ORG, orgId));

        return entities;
    }

    private PunchRequestViewDto toViewDto(PunchRequest request, OffsetDateTime now, List<Long> appliesToAccountIds) {
        PunchRequestViewDto dto = new PunchRequestViewDto();
        dto.setId(request.getId());
        dto.setOrgId(request.getOrgId());
        dto.setEntityTypeId(request.getEntityTypeId());
        dto.setEntityId(request.getEntityId());
        dto.setRequesterAccountId(request.getRequesterAccountId());
        dto.setRequestedDateTime(request.getRequestedDatetime());
        dto.setRespondWithinMinutes(request.getRespondWithinMinutes());
        dto.setExpiresAt(request.getExpiresAt());
        dto.setState(request.getState().name());

        // Compute activeNow and secondsRemaining
        boolean activeNow = !now.isBefore(request.getRequestedDatetime()) && now.isBefore(request.getExpiresAt());
        dto.setActiveNow(activeNow);

        if (activeNow) {
            long secondsRemaining = Duration.between(now, request.getExpiresAt()).getSeconds();
            dto.setSecondsRemaining(Math.max(0, secondsRemaining));
        } else {
            dto.setSecondsRemaining(0L);
        }

        dto.setAppliesToAccountIds(appliesToAccountIds);

        return dto;
    }

    /**
     * Internal class for entity reference.
     */
    private static class EntityRef {
        final int entityTypeId;
        final long entityId;

        EntityRef(int entityTypeId, long entityId) {
            this.entityTypeId = entityTypeId;
            this.entityId = entityId;
        }

        boolean matches(int typeId, long entId) {
            return this.entityTypeId == typeId && this.entityId == entId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof EntityRef)) return false;
            EntityRef entityRef = (EntityRef) o;
            return entityTypeId == entityRef.entityTypeId && entityId == entityRef.entityId;
        }

        @Override
        public int hashCode() {
            return Objects.hash(entityTypeId, entityId);
        }
    }
}
