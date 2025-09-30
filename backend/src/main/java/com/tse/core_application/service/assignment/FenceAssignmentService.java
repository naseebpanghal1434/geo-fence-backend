package com.tse.core_application.service.assignment;

import com.tse.core_application.constants.EntityTypes;
import com.tse.core_application.dto.assignment.*;
import com.tse.core_application.entity.assignment.FenceAssignment;
import com.tse.core_application.entity.fence.GeoFence;
import com.tse.core_application.exception.FenceNotFoundException;
import com.tse.core_application.exception.ProblemException;
import com.tse.core_application.repository.assignment.FenceAssignmentRepository;
import com.tse.core_application.repository.fence.GeoFenceRepository;
import com.tse.core_application.service.dir.DirectoryProvider;
import com.tse.core_application.service.dir.EntityRef;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FenceAssignmentService {

    private final FenceAssignmentRepository assignmentRepository;
    private final GeoFenceRepository fenceRepository;
    private final DirectoryProvider directoryProvider;

    public FenceAssignmentService(FenceAssignmentRepository assignmentRepository,
                                  GeoFenceRepository fenceRepository,
                                  DirectoryProvider directoryProvider) {
        this.assignmentRepository = assignmentRepository;
        this.fenceRepository = fenceRepository;
        this.directoryProvider = directoryProvider;
    }

    @Transactional
    public AssignFenceResult assignFenceToEntity(Long orgId, AssignFenceRequest request) {
        // Validate fence exists and is active
        GeoFence fence = fenceRepository.findByIdAndOrgId(request.getFenceId(), orgId)
                .orElseThrow(() -> new FenceNotFoundException(request.getFenceId(), orgId));

        if (!fence.getOrgId().equals(orgId)) {
            throw new ProblemException(
                    org.springframework.http.HttpStatus.CONFLICT,
                    "CROSS_ORG_MISMATCH",
                    "Cross-org mismatch",
                    "Fence org_id does not match path org_id");
        }

        if (!Boolean.TRUE.equals(fence.getIsActive())) {
            throw new ProblemException(
                    org.springframework.http.HttpStatus.CONFLICT,
                    "FENCE_INACTIVE",
                    "Fence inactive",
                    "Cannot assign inactive fence id=" + request.getFenceId());
        }

        AssignFenceResult result = new AssignFenceResult();
        result.setFenceId(request.getFenceId());
        result.setUpdatedAt(OffsetDateTime.now());
        result.setUpdatedBy(request.getUpdatedBy());

        AssignmentSummary summary = new AssignmentSummary();
        List<EntityResult> results = new ArrayList<>();

        // Process add items
        if (request.getAdd() != null) {
            for (EntityActionItem item : request.getAdd()) {
                EntityResult entityResult = processAdd(orgId, request.getFenceId(), item, request.getUpdatedBy(), summary);
                results.add(entityResult);
            }
        }

        // Process remove items
        if (request.getRemove() != null) {
            for (EntityActionItem item : request.getRemove()) {
                EntityResult entityResult = processRemove(orgId, request.getFenceId(), item, summary);
                results.add(entityResult);
            }
        }

        result.setSummary(summary);
        result.setResults(results);

        return result;
    }

    private EntityResult processAdd(Long orgId, Long fenceId, EntityActionItem item, Long updatedBy, AssignmentSummary summary) {
        // Validate entity type
        if (!EntityTypes.isValid(item.getEntityTypeId())) {
            summary.incrementErrors();
            return new EntityResult(item.getEntityTypeId(), item.getEntityId(), "ERROR", false,
                    Collections.emptyList(), "Invalid entity type: " + item.getEntityTypeId());
        }

        if (item.getEntityId() == null || item.getEntityId() <= 0) {
            summary.incrementErrors();
            return new EntityResult(item.getEntityTypeId(), item.getEntityId(), "ERROR", false,
                    Collections.emptyList(), "Invalid entity ID");
        }

        // Check if assignment already exists
        Optional<FenceAssignment> existing = assignmentRepository.findByOrgIdAndFenceIdAndEntityTypeIdAndEntityId(
                orgId, fenceId, item.getEntityTypeId(), item.getEntityId());

        if (existing.isPresent()) {
            // Assignment already exists - handle makeDefault
            FenceAssignment assignment = existing.get();
            boolean wasUpdated = false;

            if (Boolean.TRUE.equals(item.getMakeDefault()) && !Boolean.TRUE.equals(assignment.getIsDefault())) {
                // Unset other defaults
                assignmentRepository.unsetDefaultForEntity(orgId, item.getEntityTypeId(), item.getEntityId());
                assignmentRepository.flush();

                // Set this as default
                assignment.setIsDefault(true);
                assignment.setUpdatedBy(updatedBy);
                assignmentRepository.save(assignment);

                summary.incrementUpdatedDefault();
                wasUpdated = true;
            }

            List<Long> allFenceIds = assignmentRepository.findFenceIdsByEntity(orgId, item.getEntityTypeId(), item.getEntityId());

            if (wasUpdated) {
                return new EntityResult(item.getEntityTypeId(), item.getEntityId(), "UPDATED_DEFAULT", true,
                        allFenceIds, "Set as default.");
            } else {
                summary.incrementNoops();
                return new EntityResult(item.getEntityTypeId(), item.getEntityId(), "NOOP",
                        assignment.getIsDefault(), allFenceIds, "Already assigned.");
            }
        }

        // Create new assignment
        FenceAssignment newAssignment = new FenceAssignment();
        newAssignment.setOrgId(orgId);
        newAssignment.setFenceId(fenceId);
        newAssignment.setEntityTypeId(item.getEntityTypeId());
        newAssignment.setEntityId(item.getEntityId());
        newAssignment.setCreatedBy(updatedBy);
        newAssignment.setUpdatedBy(updatedBy);

        // Check if entity has no fences - auto-set as default
        List<FenceAssignment> entityAssignments = assignmentRepository.findByOrgIdAndEntityTypeIdAndEntityId(
                orgId, item.getEntityTypeId(), item.getEntityId());

        boolean makeDefault = Boolean.TRUE.equals(item.getMakeDefault()) || entityAssignments.isEmpty();

        if (makeDefault) {
            // Unset any existing default
            assignmentRepository.unsetDefaultForEntity(orgId, item.getEntityTypeId(), item.getEntityId());
            assignmentRepository.flush();
            newAssignment.setIsDefault(true);
            summary.incrementUpdatedDefault();
        } else {
            newAssignment.setIsDefault(false);
        }

        assignmentRepository.save(newAssignment);
        summary.incrementAdded();

        List<Long> allFenceIds = assignmentRepository.findFenceIdsByEntity(orgId, item.getEntityTypeId(), item.getEntityId());

        String message = makeDefault ? "Assigned and set as default." : "Assigned.";
        return new EntityResult(item.getEntityTypeId(), item.getEntityId(), "ADDED", makeDefault,
                allFenceIds, message);
    }

    private EntityResult processRemove(Long orgId, Long fenceId, EntityActionItem item, AssignmentSummary summary) {
        // Validate entity type
        if (!EntityTypes.isValid(item.getEntityTypeId())) {
            summary.incrementErrors();
            return new EntityResult(item.getEntityTypeId(), item.getEntityId(), "ERROR", false,
                    Collections.emptyList(), "Invalid entity type: " + item.getEntityTypeId());
        }

        // Check if assignment exists
        Optional<FenceAssignment> existing = assignmentRepository.findByOrgIdAndFenceIdAndEntityTypeIdAndEntityId(
                orgId, fenceId, item.getEntityTypeId(), item.getEntityId());

        if (!existing.isPresent()) {
            summary.incrementNoops();
            List<Long> allFenceIds = assignmentRepository.findFenceIdsByEntity(orgId, item.getEntityTypeId(), item.getEntityId());
            return new EntityResult(item.getEntityTypeId(), item.getEntityId(), "NOOP", false,
                    allFenceIds, "Fence not assigned previously.");
        }

        FenceAssignment assignment = existing.get();
        boolean wasDefault = Boolean.TRUE.equals(assignment.getIsDefault());

        // Delete the assignment
        assignmentRepository.delete(assignment);
        assignmentRepository.flush();

        // If it was default, handle default reassignment
        if (wasDefault) {
            List<FenceAssignment> remainingAssignments = assignmentRepository.findByOrgIdAndEntityTypeIdAndEntityId(
                    orgId, item.getEntityTypeId(), item.getEntityId());

            if (remainingAssignments.size() == 1) {
                // Set the only remaining fence as default
                FenceAssignment remaining = remainingAssignments.get(0);
                remaining.setIsDefault(true);
                assignmentRepository.save(remaining);
            }
            // If more than one remains or none, leave no default
        }

        summary.incrementRemoved();

        List<Long> allFenceIds = assignmentRepository.findFenceIdsByEntity(orgId, item.getEntityTypeId(), item.getEntityId());

        return new EntityResult(item.getEntityTypeId(), item.getEntityId(), "REMOVED", false,
                allFenceIds, "Removed; assignment deleted.");
    }

    @Transactional(readOnly = true)
    public AssignedEntitiesResponse getAssignedEntities(Long orgId, Long fenceId) {
        // Validate fence exists
        GeoFence fence = fenceRepository.findByIdAndOrgId(fenceId, orgId)
                .orElseThrow(() -> new FenceNotFoundException(fenceId, orgId));

        AssignedEntitiesResponse response = new AssignedEntitiesResponse();
        response.setFenceId(fenceId);

        // Get all assignments for this fence
        List<FenceAssignment> assignments = assignmentRepository.findByOrgIdAndFenceId(orgId, fenceId);

        // Group by entity type
        EntityLists assigned = buildAssignedLists(orgId, assignments);
        response.setAssigned(assigned);

        // Get unassigned entities (from directory provider - currently NOOP)
        EntityLists unassigned = new EntityLists();
        response.setUnassigned(unassigned);

        // Count
        EntityCounts count = new EntityCounts(
                assigned.getUsers().size(),
                assigned.getTeams().size(),
                assigned.getProjects().size(),
                assigned.getOrgs().size()
        );
        response.setCount(count);

        return response;
    }

    private EntityLists buildAssignedLists(Long orgId, List<FenceAssignment> assignments) {
        EntityLists lists = new EntityLists();

        Map<Integer, List<FenceAssignment>> byType = assignments.stream()
                .collect(Collectors.groupingBy(FenceAssignment::getEntityTypeId));

        // Users
        List<FenceAssignment> users = byType.getOrDefault(EntityTypes.USER, Collections.emptyList());
        lists.setUsers(users.stream()
                .map(a -> buildAssignedEntity(orgId, a))
                .collect(Collectors.toList()));

        // Teams
        List<FenceAssignment> teams = byType.getOrDefault(EntityTypes.TEAM, Collections.emptyList());
        lists.setTeams(teams.stream()
                .map(a -> buildAssignedEntity(orgId, a))
                .collect(Collectors.toList()));

        // Projects
        List<FenceAssignment> projects = byType.getOrDefault(EntityTypes.PROJECT, Collections.emptyList());
        lists.setProjects(projects.stream()
                .map(a -> buildAssignedEntity(orgId, a))
                .collect(Collectors.toList()));

        // Orgs
        List<FenceAssignment> orgs = byType.getOrDefault(EntityTypes.ORG, Collections.emptyList());
        lists.setOrgs(orgs.stream()
                .map(a -> buildAssignedEntity(orgId, a))
                .collect(Collectors.toList()));

        return lists;
    }

    private AssignedEntity buildAssignedEntity(Long orgId, FenceAssignment assignment) {
        // Get name from directory provider or use placeholder
        String name = getEntityName(assignment.getEntityTypeId(), assignment.getEntityId());

        // Get all fence IDs for this entity
        List<Long> allFenceIds = assignmentRepository.findFenceIdsByEntity(
                orgId, assignment.getEntityTypeId(), assignment.getEntityId());

        return new AssignedEntity(
                assignment.getEntityId(),
                name,
                assignment.getIsDefault(),
                allFenceIds
        );
    }

    private String getEntityName(Integer entityTypeId, Long entityId) {
        String typeName = EntityTypes.getTypeName(entityTypeId);
        return typeName.substring(0, 1).toUpperCase() + typeName.substring(1) + " " + entityId;
    }
}
