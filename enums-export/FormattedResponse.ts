export const FormattedResponse = {
  SUCCESS: "SUCCESS",
  ERROR: "ERROR",
  FAILED: "FAILED",
} as const;

export type FormattedResponseType = typeof FormattedResponse;
