package com.tse.core_application.dto.assignment;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class EntityCounts {
    private int users = 0;
    private int teams = 0;
    private int projects = 0;
    private int orgs = 0;
}
