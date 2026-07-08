# SHOGUN SAKURA

Portfolio-style demo site with a GitHub Pages frontend and a Render backend.

## URLs

- Frontend: `https://sr5msym6xbw3oil22im7.github.io/shogun-sakura-demo/frontend/`
- Backend: `https://shogun-sakura-demo.onrender.com/`

## Notes

- The frontend sends order requests to the backend API.
- The backend stores order data in a text file instead of PostgreSQL.
- This is a demo project, so payment, shipping, and inventory management are not implemented.

## Local Run

### Backend

```powershell
cd backend
mvn clean package
mvn spring-boot:run
```

## API

- `GET /api/health`
- `POST /api/orders`

## Render

- Docker Web Service
- Health check: `/api/health`
- Order storage path: `ORDER_STORAGE_PATH`

## CORS

The backend allows requests from:

- `https://sr5msym6xbw3oil22im7.github.io`
- `http://localhost:5500`
- `http://127.0.0.1:5500`
