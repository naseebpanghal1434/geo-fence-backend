export const EntityTypes = {
  USER: 1,
  ORG: 2,
  PROJECT: 4,
  TEAM: 5,
} as const;

export type EntityTypesType = typeof EntityTypes;
