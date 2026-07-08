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
- `ORDER_STORAGE_PATH`
- `ALLOWED_ORIGINS`

`ORDER_STORAGE_PATH` points to the text file used for order storage. On Render, we use `/tmp/shogun-sakura/orders.txt`.

## Notes

- PostgreSQL is not used.
- Orders are appended as JSON Lines in a text file.
