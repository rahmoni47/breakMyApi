import { MAX_SPEC_SIZE_BYTES } from "./fileUtils";

export function validateNewTestForm(formData, providerConfigured) {
  const errors = {};

  const projectName = formData.projectName?.trim();
  if (!projectName) {
    errors.projectName =
      "Project name is required. Please provide a title for this test run.";
  }

  if (!formData.specFile) {
    errors.specFile =
      "JSON API specification file is missing. Please upload a valid JSON/OpenAPI schema file.";
  } else if (formData.specFile.size > MAX_SPEC_SIZE_BYTES) {
    errors.specFile = "The uploaded file exceeds the 25 MB limit.";
  }

  if (!formData.specification) {
    errors.specification =
      formData.specificationError ||
      "Unable to parse API specification. Please upload a supported file.";
  }

  if (!formData.testDepth) {
    errors.testDepth =
      "Please select a test speed depth mode to calculate suite workload before initiating.";
  }

  if (!formData.modelId) {
    errors.modelId = "Please select an AI model for this run.";
  }

  if (!providerConfigured) {
    errors.provider =
      "Selected model provider is not configured. Add credentials in settings.";
  }

  return {
    isValid: Object.keys(errors).length === 0,
    errors,
  };
}
