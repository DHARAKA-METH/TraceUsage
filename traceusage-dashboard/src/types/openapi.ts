export type OpenApiImportResponse = {
  fileName: string;
  contentType: string | null;
  sizeBytes: number;
  importedAt: string;
  endpointsDiscovered: number;
  fieldsDiscovered: number;
  deprecatedEndpoints: number;
  deprecatedFields: number;
  status: "IMPORTED" | "ALREADY_IMPORTED" | string;
};
