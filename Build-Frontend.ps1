$ErrorActionPreference = 'Stop'
$frontend = Join-Path $PSScriptRoot '../frontend'
Push-Location $frontend
try {
    New-Item -ItemType Directory -Force dist/assets | Out-Null
    # Direct CLI also works in environments that disallow Node child-process pipes.
    & './node_modules/@esbuild/win32-x64/esbuild.exe' src/main.jsx --bundle --minify --format=esm --target=es2022 --outdir=dist/assets --loader:.woff2=file --loader:.woff=file '--asset-names=[name]-[hash]'
    if ($LASTEXITCODE -ne 0) { throw 'Frontend build failed.' }
    Copy-Item public/* dist -Recurse -Force
    $html = (Get-Content index.html -Raw).Replace('<script type="module" src="/src/main.jsx"></script>', '<link rel="stylesheet" href="/assets/main.css"/><script type="module" src="/assets/main.js"></script>')
    [IO.File]::WriteAllText((Join-Path (Get-Location) 'dist/index.html'), $html)
} finally { Pop-Location }
