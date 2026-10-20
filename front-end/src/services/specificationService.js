function normalizeServers(openApiDoc) {
  if (Array.isArray(openApiDoc.servers) && openApiDoc.servers.length > 0) {
    return openApiDoc.servers.map((server) => server.url).filter(Boolean);
  }

  const host = openApiDoc.host;
  const basePath = openApiDoc.basePath || "";
  const schemes = openApiDoc.schemes || ["https"];

  if (host) {
    return schemes.map((scheme) => `${scheme}://${host}${basePath}`);
  }

  return [];
}

function extractEndpointsFromOpenApi(paths = {}) {
  const endpoints = [];
  Object.entries(paths).forEach(([path, methods]) => {
    Object.entries(methods || {}).forEach(([method, operation]) => {
      const normalizedMethod = method.toUpperCase();
      if (
        !["GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD"].includes(
          normalizedMethod,
        )
      ) {
        return;
      }
      endpoints.push({
        method: normalizedMethod,
        path,
        operationId: operation?.operationId || null,
        summary: operation?.summary || "",
        parameters: operation?.parameters || [],
        requestBody: operation?.requestBody || null,
        responses: operation?.responses || {},
        security: operation?.security || null,
      });
    });
  });

  return endpoints;
}

function parseOpenApi(documentJson, rawText, fileName) {
  const isOpenApi3 =
    typeof documentJson.openapi === "string" &&
    documentJson.openapi.startsWith("3");
  const isSwagger2 =
    typeof documentJson.swagger === "string" &&
    documentJson.swagger.startsWith("2");

  if (!isOpenApi3 && !isSwagger2) {
    return null;
  }

  const endpoints = extractEndpointsFromOpenApi(documentJson.paths);
  if (endpoints.length === 0) {
    throw new Error(
      "No valid API endpoints were found in the specification paths.",
    );
  }

  return {
    format: isOpenApi3 ? "OpenAPI 3.x" : "Swagger 2.0",
    rawText,
    metadata: {
      title: documentJson.info?.title || "Untitled API",
      version: documentJson.info?.version || "unknown",
      fileName,
      endpointCount: endpoints.length,
      servers: normalizeServers(documentJson),
    },
    endpoints,
  };
}

function parsePostmanCollection(documentJson, rawText, fileName) {
  const isCollection =
    documentJson.info &&
    documentJson.info.schema &&
    String(documentJson.info.schema).includes("collection");

  if (!isCollection || !Array.isArray(documentJson.item)) return null;

  const endpoints = [];
  const stack = [...documentJson.item];

  while (stack.length) {
    const current = stack.pop();
    if (Array.isArray(current.item)) {
      stack.push(...current.item);
      continue;
    }

    const request = current.request;
    if (!request) continue;

    const method = String(request.method || "").toUpperCase();
    const path = Array.isArray(request.url?.path)
      ? `/${request.url.path.join("/")}`
      : request.url?.raw || "/unknown";

    if (!method || !path) continue;
    endpoints.push({
      method,
      path,
      operationId: null,
      summary: current.name || "",
      parameters: request.url?.query || [],
      requestBody: request.body || null,
      responses: {},
      security: null,
    });
  }

  if (endpoints.length === 0) {
    throw new Error(
      "The request collection does not contain any executable requests.",
    );
  }

  return {
    format: "Postman Collection",
    rawText,
    metadata: {
      title: documentJson.info?.name || "Untitled Collection",
      version: documentJson.info?.version || "unknown",
      fileName,
      endpointCount: endpoints.length,
      servers: [],
    },
    endpoints,
  };
}

export function parseSpecification(rawText, fileName) {
  let json;
  try {
    json = JSON.parse(rawText);
  } catch {
    throw new Error(
      "Invalid JSON file. Please upload a valid JSON API specification.",
    );
  }

  const openApi = parseOpenApi(json, rawText, fileName);
  if (openApi) return openApi;

  const postman = parsePostmanCollection(json, rawText, fileName);
  if (postman) return postman;

  throw new Error(
    "Unsupported API specification format. Upload OpenAPI 3.x, Swagger 2.0, or a supported Postman collection JSON.",
  );
}
