export const environment = {
  production: true,
  // For production, use relative path or set via build-time replacement
  // You can set NG_APP_API_URL in Vercel environment variables
  // For now, using relative path which works if backend is on same domain
  // If backend is on different domain, you'll need to set the full URL
  apiUrl: '/api'  // Change this to your backend URL, e.g., 'https://your-backend.herokuapp.com/api'
};

