const fs = require('fs');
const path = require('path');

// Read the environment file
const envFilePath = path.join(__dirname, 'src', 'environment', 'environment.prod.ts');
let envContent = fs.readFileSync(envFilePath, 'utf8');

// Get the backend URL from environment variable
const backendUrl = process.env.BACKEND_API_URL || process.env.NG_APP_API_URL;

if (!backendUrl || backendUrl === 'YOUR_BACKEND_URL') {
  console.error('❌ ERROR: BACKEND_API_URL environment variable not set!');
  console.error('❌ Please set BACKEND_API_URL in Vercel environment variables.');
  console.error('❌ Go to: Vercel Dashboard > Your Project > Settings > Environment Variables');
  console.error('❌ Add: BACKEND_API_URL = https://your-backend-url.com/api');
  console.error('❌ Example: https://your-backend.railway.app/api');
  process.exit(1);
}

// Ensure the URL ends with /api
const finalUrl = backendUrl.endsWith('/api') ? backendUrl : `${backendUrl}/api`;

// Replace the placeholder
envContent = envContent.replace(/YOUR_BACKEND_URL\/api/g, finalUrl);

// Write back the file
fs.writeFileSync(envFilePath, envContent, 'utf8');

console.log(`✅ Environment file updated with backend URL: ${finalUrl}`);

