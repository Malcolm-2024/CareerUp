import {writeFile,rename} from 'node:fs/promises';
const [url,destination]=process.argv.slice(2);
if(!url||!destination||new URL(url).protocol!=='https:')throw Error('An HTTPS URL and output path are required.');
const response=await fetch(url);
if(!response.ok)throw Error(`Download failed: HTTP ${response.status}`);
await writeFile(destination+'.download',Buffer.from(await response.arrayBuffer()));
await rename(destination+'.download',destination);
console.log('Build tool downloaded.');
