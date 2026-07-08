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

Set the PostgreSQL connection values in the same style as the `psql` command shown in `render.psql.txt`:

- `DATABASE_URL`
- `DATABASE_USERNAME`
- `DATABASE_PASSWORD`
- `ALLOWED_ORIGINS`

The backend builds a JDBC connection from those values and enables SSL for Render PostgreSQL.

## Notes

- The frontend sends orders to this backend API.
- Order data is saved to Render PostgreSQL.
- When the insert succeeds, the API returns an order completion message and order ID to the frontend.
