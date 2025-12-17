const fs = require('fs');
const path = require('path');

const backendUrl = process.env.BACKEND_API_URL;

if (!backendUrl) {
  console.error('❌ BACKEND_API_URL is not set');
  process.exit(1);
}

// sanitize URL
const finalUrl = backendUrl.replace(/\/$/, '').replace(/\/api$/, '');

const files = [
  path.join(__dirname, 'src/environment/environment.ts'),
  path.join(__dirname, 'src/environment/environment.prod.ts')
];

files.forEach(file => {
  let content = fs.readFileSync(file, 'utf8');

  content = content.replace(
    /apiBaseUrl:\s*'[^']*'/,
    `apiBaseUrl: '${finalUrl}'`
  );

  fs.writeFileSync(file, content, 'utf8');
  console.log(`✅ Updated ${file} with ${finalUrl}`);
});

console.log('✅ Environment replacement complete');
