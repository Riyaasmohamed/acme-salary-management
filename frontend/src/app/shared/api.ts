// STUDY NOTES: One constant with the API prefix "/api". Both API services build their URLs from it, so the prefix lives in one place.
// STUDY NOTES: How /api reaches the backend: `ng serve` reads "proxyConfig": "proxy.conf.json" (angular.json), and proxy.conf.json forwards every /api request from localhost:4200 to http://localhost:8080.
/**
 * Base path of the backend API. In development the Angular dev server proxies it to
 * http://localhost:8080 (see proxy.conf.json), so no CORS configuration is needed.
 */
// WHY: A relative URL (no host, no port) means the browser calls the same origin as the page, so there is no cross-origin (CORS) request.
// INTERVIEW: In production the built UI would be served from the same origin as the API (or behind one reverse proxy), so '/api' still works unchanged.
export const API_BASE_URL = '/api';
