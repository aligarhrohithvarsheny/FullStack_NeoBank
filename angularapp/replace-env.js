const fs = require('fs');
const path = require('path');

// Get the backend URL from environment variable
const backendUrl = process.env.BACKEND_API_URL;

if (!backendUrl) {
  console.error('❌ BACKEND_API_URL is not set');
  console.error('❌ Set BACKEND_API_URL in Vercel environment variables');
  process.exit(1);
}

// Clean the URL: remove trailing slashes and /api suffix
const finalUrl = backendUrl.replace(/\/$/, '').replace(/\/api$/, '');

// Files to update
const files = [
  'src/environment/environment.ts',
  'src/environment/environment.prod.ts'
];

files.forEach(file => {
  const filePath = path.join(__dirname, file);
  let content = fs.readFileSync(filePath, 'utf8');

  // Replace apiBaseUrl value - matches: apiBaseUrl: '...' or apiBaseUrl: "..." or apiBaseUrl: ''
  // This regex safely matches the entire apiBaseUrl line value without introducing newlines
  content = content.replace(
    /apiBaseUrl:\s*['"][^'"]*['"]/,
    `apiBaseUrl: '${finalUrl}'`
  );

  fs.writeFileSync(filePath, content, 'utf8');
  console.log(`✅ Updated ${file} with ${finalUrl}`);
});

console.log(`✅ All environment files updated`);
console.log(`✅ API calls will use: ${finalUrl}/api/...`);
