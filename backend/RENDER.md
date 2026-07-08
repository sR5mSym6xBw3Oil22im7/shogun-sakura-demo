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

- `PORT`
- `DATABASE_URL`
- `DATABASE_USERNAME`
- `DATABASE_PASSWORD`
- `ALLOWED_ORIGINS`

The backend accepts either a full Postgres URL such as `postgres://...` or `postgresql://...`, or a JDBC URL such as `jdbc:postgresql://...`.

## Notes

- The frontend sends orders to this backend API.
- Order data is saved to Render PostgreSQL.
- When the insert succeeds, the API returns an order completion message and order ID to the frontend.
