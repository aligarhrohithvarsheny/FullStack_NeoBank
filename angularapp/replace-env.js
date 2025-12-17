const fs = require('fs');
const path = require('path');

// Read the environment file
const envFilePath = path.join(__dirname, 'src', 'environment', 'environment.prod.ts');
let envContent = fs.readFileSync(envFilePath, 'utf8');

// Get the backend URL from environment variable
const backendUrl = process.env.BACKEND_API_URL || process.env.NG_APP_API_URL;

let finalBaseUrl;

if (!backendUrl || backendUrl === 'YOUR_BACKEND_URL' || backendUrl === 'https://your-backend-url.com') {
  console.error('❌ ERROR: BACKEND_API_URL environment variable not set!');
  console.error('❌ This will cause API calls to fail.');
  console.error('');
  console.error('📝 To configure your backend URL:');
  console.error('   1. Go to: Vercel Dashboard > Your Project > Settings > Environment Variables');
  console.error('   2. Add: BACKEND_API_URL = https://your-actual-backend-url.com');
  console.error('   3. Example: https://your-backend.railway.app');
  console.error('   4. IMPORTANT: Do NOT include /api in the URL');
  console.error('   5. Redeploy your application');
  console.error('');
  process.exit(1);
} else {
  // Remove trailing slashes and /api if present
  finalBaseUrl = backendUrl.replace(/\/+$/, '').replace(/\/api$/, '');
  console.log(`✅ Using backend base URL from environment variable: ${finalBaseUrl}`);
}

// Replace the apiBaseUrl value in the environment file
// Match: apiBaseUrl: '' or apiBaseUrl: 'https://...' or apiBaseUrl: "..." or apiBaseUrl: `...`
// This regex handles empty strings and any quoted value
const apiBaseUrlRegex = /apiBaseUrl:\s*['"`]([^'"`]*)['"`]/;
if (apiBaseUrlRegex.test(envContent)) {
  envContent = envContent.replace(apiBaseUrlRegex, `apiBaseUrl: '${finalBaseUrl}'`);
  console.log(`✅ Updated apiBaseUrl to: ${finalBaseUrl}`);
} else {
  console.error('❌ Could not find apiBaseUrl in environment file');
  console.error('❌ Expected format: apiBaseUrl: \'\'');
  process.exit(1);
}

// Write back the file
fs.writeFileSync(envFilePath, envContent, 'utf8');

console.log(`✅ Environment file updated with backend base URL: ${finalBaseUrl}`);
console.log(`✅ API calls will use: ${finalBaseUrl}/api/...`);

