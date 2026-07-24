# Render Deployment

This backend is deployed as a Render Web Service using Docker.

## Service

- Name: `shogun-sakura-demo`
- URL: `https://shogun-sakura-demo.onrender.com/`
- Root directory: `backend`
- Dockerfile: `backend/Dockerfile`

## Settings

- Health check path: `/api/health`
- Auto deploy: on commit

## Environment Variables

Set the PostgreSQL connection values and Gemini API key in the Render Dashboard. Keep them in the Render service settings rather than in the repository.

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `GEMINI_API_KEY`
- `MCP_INTERNAL_TOKEN`
- `MCP_BASE_URL`
- `ALLOWED_ORIGINS`

The backend uses `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and
`SPRING_DATASOURCE_PASSWORD` directly for its PostgreSQL connection. Set
`GEMINI_API_KEY`, `MCP_INTERNAL_TOKEN`, and `MCP_BASE_URL` in the service
settings to enable the AI chat and MCP integration.

## Notes

- The frontend sends orders to this backend API.
- Order data is saved to Render PostgreSQL.
- When the insert succeeds, the API returns an order completion message and order ID to the frontend.
- Keep database connection values and the Gemini API key in Render Dashboard settings; do not save them in the repository.
