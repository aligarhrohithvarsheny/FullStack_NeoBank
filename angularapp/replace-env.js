const fs = require('fs');
const path = require('path');

// Read the environment file
const envFilePath = path.join(__dirname, 'src', 'environment', 'environment.prod.ts');
let envContent = fs.readFileSync(envFilePath, 'utf8');

// Get the backend URL from environment variable
const backendUrl = process.env.BACKEND_API_URL || process.env.NG_APP_API_URL;

let finalUrl;

if (!backendUrl || backendUrl === 'YOUR_BACKEND_URL') {
  console.warn('⚠️  WARNING: BACKEND_API_URL environment variable not set!');
  console.warn('⚠️  Using relative path "/api" as fallback.');
  console.warn('⚠️  This will only work if you have a proxy/rewrite configured in Vercel.');
  console.warn('');
  console.warn('📝 To configure your backend URL:');
  console.warn('   1. Go to: Vercel Dashboard > Your Project > Settings > Environment Variables');
  console.warn('   2. Add: BACKEND_API_URL = https://your-backend-url.com/api');
  console.warn('   3. Example: https://your-backend.railway.app/api');
  console.warn('   4. Redeploy your application');
  console.warn('');
  // Use relative path as fallback (requires Vercel proxy/rewrite configuration)
  finalUrl = '/api';
} else {
  // Ensure the URL ends with /api
  finalUrl = backendUrl.endsWith('/api') ? backendUrl : `${backendUrl}/api`;
  console.log(`✅ Using backend URL from environment variable: ${finalUrl}`);
}

// Replace the placeholder
envContent = envContent.replace(/YOUR_BACKEND_URL\/api/g, finalUrl);

// Write back the file
fs.writeFileSync(envFilePath, envContent, 'utf8');

console.log(`✅ Environment file updated with backend URL: ${finalUrl}`);

