package com.tse.core_application.service.membership.impl;

import com.tse.core_application.service.membership.MembershipProvider;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * No-operation implementation of MembershipProvider.
 * Returns empty lists for teams and projects.
 * Used as default until real directory/membership system is integrated.
 */
@Service
public class NoopMembershipProvider implements MembershipProvider {

    @Override
    public List<Long> listTeamsForUser(long orgId, long accountId) {
        return Collections.emptyList();
    }

    @Override
    public List<Long> listProjectsForUser(long orgId, long accountId) {
        return Collections.emptyList();
    }

    @Override
    public boolean orgExists(long orgId) {
        return true;
    }
}
