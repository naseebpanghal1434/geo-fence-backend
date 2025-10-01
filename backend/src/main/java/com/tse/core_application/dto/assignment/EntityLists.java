package com.tse.core_application.dto.assignment;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class EntityLists {
    private List<AssignedEntity> users = new ArrayList<>();
    private List<AssignedEntity> teams = new ArrayList<>();
    private List<AssignedEntity> projects = new ArrayList<>();
    private List<AssignedEntity> orgs = new ArrayList<>();
}
