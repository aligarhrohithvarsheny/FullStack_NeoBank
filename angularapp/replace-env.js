const fs = require('fs');
const path = require('path');

// Step 1: Validate BACKEND_API_URL exists
const backendUrl = process.env.BACKEND_API_URL;

if (!backendUrl) {
  console.error('❌ ERROR: BACKEND_API_URL environment variable is not set');
  console.error('   Build cannot proceed without a backend URL.');
  process.exit(1);
}

// Step 2: Validate URL doesn't contain variable name (common misconfiguration)
if (backendUrl.includes('BACKEND_API_URL=') || backendUrl.includes('BACKEND_API_URL =')) {
  console.error('❌ ERROR: BACKEND_API_URL contains the variable name itself');
  console.error('   Current value:', backendUrl);
  console.error('   The value should be ONLY the URL, not "BACKEND_API_URL=URL"');
  console.error('   In Vercel, set BACKEND_API_URL to: https://fullstack-neobank.onrender.com');
  process.exit(1);
}

// Step 3: Sanitize URL - trim, remove trailing slashes and /api
let sanitizedUrl = backendUrl.trim();
// Remove trailing slashes
sanitizedUrl = sanitizedUrl.replace(/\/+$/, '');
// Remove trailing /api if present
sanitizedUrl = sanitizedUrl.replace(/\/api$/, '');

// Step 4: Validate sanitized URL is not empty
if (!sanitizedUrl) {
  console.error('❌ ERROR: BACKEND_API_URL is empty after sanitization');
  process.exit(1);
}

// Step 5: Ensure URL contains no newlines or control characters that could break TypeScript
if (sanitizedUrl.includes('\n') || sanitizedUrl.includes('\r')) {
  console.error('❌ ERROR: BACKEND_API_URL contains newline characters which would break TypeScript');
  process.exit(1);
}

// Step 6: Escape single quotes in URL to prevent string breaking
const escapedUrl = sanitizedUrl.replace(/'/g, "\\'");

// Step 6: Define files to update
const files = [
  path.join(__dirname, 'src/environment/environment.ts'),
  path.join(__dirname, 'src/environment/environment.prod.ts')
];

// Step 8: Process each file
files.forEach(file => {
  // Check file exists
  if (!fs.existsSync(file)) {
    console.error(`❌ ERROR: File not found: ${file}`);
    process.exit(1);
  }

  // Read file content
  let content = fs.readFileSync(file, 'utf8');

  // Step 9: Use safe regex that matches single quotes, double quotes, or backticks
  // Regex: /apiBaseUrl:\s*['"`][^'"`\n]*['"`]/
  // This explicitly excludes newlines and matches any quote type
  const regex = /apiBaseUrl:\s*['"`][^'"`\n]*['"`]/;
  
  // Check if pattern exists
  if (!regex.test(content)) {
    console.error(`❌ ERROR: Could not find apiBaseUrl pattern in ${file}`);
    console.error('   Expected pattern: apiBaseUrl: \'...\' or apiBaseUrl: "..." or apiBaseUrl: `...`');
    process.exit(1);
  }

  // Step 10: Replace with single-line value (always use single quotes for consistency)
  // The replacement string is guaranteed to be on a single line
  const replacement = `apiBaseUrl: '${escapedUrl}'`;
  content = content.replace(regex, replacement);

  // Step 11: Validate the result is valid TypeScript
  // Check that apiBaseUrl line is properly terminated
  const apiBaseUrlLine = content.match(/apiBaseUrl:\s*'[^']*'/);
  if (!apiBaseUrlLine) {
    console.error(`❌ ERROR: Replacement failed - apiBaseUrl not found after replacement in ${file}`);
    process.exit(1);
  }
  
  // Ensure the line doesn't contain newlines (should be impossible, but double-check)
  if (apiBaseUrlLine[0].includes('\n') || apiBaseUrlLine[0].includes('\r')) {
    console.error(`❌ ERROR: Replacement introduced newline in ${file}`);
    process.exit(1);
  }
  
  // Validate file structure is intact
  if (!content.includes('export const environment')) {
    console.error(`❌ ERROR: File structure corrupted in ${file}`);
    process.exit(1);
  }

  // Step 12: Write file back
  fs.writeFileSync(file, content, 'utf8');
  console.log(`✅ Updated ${path.basename(file)} with apiBaseUrl: '${sanitizedUrl}'`);
});

console.log('✅ Environment replacement complete - all files are valid TypeScript');
