# Render Deployment

This backend is intended to be deployed as a Render Web Service using Docker. The repository already includes a Docker build definition in [backend/Dockerfile](Dockerfile) and a service definition in [render.yaml](../render.yaml).

## Recommended Render Web Service settings

Use the following values in the Render dashboard so the build succeeds without extra manual steps.

- Service type: Web Service
- Runtime: Docker
- Repository root: the repository root containing [backend/Dockerfile](Dockerfile)
- Root directory: `backend`
- Dockerfile path: `Dockerfile`
- Build command: leave empty
- Start command: leave empty
- Health check path: `/api/health`
- Auto deploy: enabled

### Why these settings work

- The Docker build expects a backend-focused context, so using the backend folder as the root directory makes the build path consistent.
- The container exposes port `8080`, and the application reads the Render-provided `PORT` environment variable through [backend/src/main/resources/application.yml](src/main/resources/application.yml).
- The health endpoint is `/api/health`, which Render can probe after deployment.

## Required environment variables

Set these values in the Render dashboard instead of committing them to the repository.

- `GEMINI_API_KEY`
- `MCP_INTERNAL_TOKEN`
- `MCP_BASE_URL`
- `ALLOWED_ORIGINS`

### Variable guidance

- `GEMINI_API_KEY` is required for the AI chat feature.
- `MCP_INTERNAL_TOKEN` and `MCP_BASE_URL` are required for the internal MCP integration used by the chat workflow.
- `ALLOWED_ORIGINS` should be `https://sr5msym6xbw3oil22im7.github.io` in production.

## Build notes

- Do not set a custom build command for this service. Render should use the Dockerfile directly.
- Do not set a custom start command either; the container already starts the Spring Boot application with the Java entrypoint defined in [backend/Dockerfile](Dockerfile).
- If you prefer to keep the repository root as the service root in Render, use the values from [render.yaml](../render.yaml) instead and keep the Docker context set to `backend`.

## Deployment notes

- The frontend sends order requests to this backend API.
- Order data is stored temporarily in the Render Web Service's H2 in-memory database.
- When an order is accepted, the API returns an order completion message and order ID to the frontend.
- Keep secrets in the Render service settings; do not store them in the repository.
