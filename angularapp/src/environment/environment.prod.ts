export const environment = {
  production: true,
  // This placeholder will be replaced during build by replace-env.js
  // Set BACKEND_API_URL environment variable in Vercel dashboard
  // Example: https://your-backend.railway.app/api
  // IMPORTANT: Do NOT include /api in BACKEND_API_URL - it will be added automatically
  // Correct: https://your-backend.railway.app
  // Wrong: https://your-backend.railway.app/api
  apiUrl: '/api' // This will be replaced by replace-env.js with BACKEND_API_URL
};

