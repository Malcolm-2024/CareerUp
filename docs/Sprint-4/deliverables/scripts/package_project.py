"""Create the source + prebuilt app download. Never package private runtime state."""
from pathlib import Path
import argparse,subprocess,zipfile,hashlib,os

# 1. PROJECT AND OUTPUT FILES
root=Path(__file__).resolve().parents[1]
parser=argparse.ArgumentParser();parser.add_argument('--output',required=True)
args=parser.parse_args();output=Path(args.output).resolve();output.parent.mkdir(parents=True,exist_ok=True)
jar=root/'backend/target/careerup-1.0.0.jar'
if not jar.exists():raise SystemExit('Build CareerUp before packaging.')
native=root/'.local/windows-app/CareerUp'
if not (native/'CareerUp.exe').exists():raise SystemExit('Build the native Windows launcher before packaging.')
commit=subprocess.check_output(['git','-C',str(root),'rev-parse','HEAD']).decode().strip()
if subprocess.check_output(['git','-C',str(root),'status','--porcelain']).strip():raise SystemExit('Commit source changes before packaging.')

# 2. VERIFIED TEAM GIT BUNDLE
bundle=output.parent/'CareerUp-Sprint4-team-change.bundle'
subprocess.run(['git','-C',str(root),'bundle','create',str(bundle),'sprint4-working-app','^origin/main'],check=True)
subprocess.run(['git','-C',str(root),'bundle','verify',str(bundle)],check=True,capture_output=True)

# 3. CODE AND STARTUP FILES (NO DATABASES, PASSWORDS, OR CACHES)
files=subprocess.check_output(['git','-C',str(root),'ls-files','-z']).decode().split('\0')
files=[name for name in files if name and not name.startswith(('docs/','.ai/'))]
for name in files:
    parts=Path(name).parts
    if any(x in parts for x in ('.local','node_modules','target','__pycache__','.git')):raise SystemExit('Private/generated path unexpectedly tracked: '+name)
with zipfile.ZipFile(output,'w',zipfile.ZIP_DEFLATED,compresslevel=6) as z:
    for name in files:
        path=root/name;archive='CareerUp-Sprint4/'+name
        if path.suffix in ['.sh','.command']:
            info=zipfile.ZipInfo(archive);info.create_system=3;info.external_attr=0o100755<<16;info.compress_type=zipfile.ZIP_DEFLATED
            z.writestr(info,path.read_bytes().replace(b'\r\n',b'\n'))
        else:z.write(path,archive)
    z.write(jar,'CareerUp-Sprint4/release/careerup-1.0.0.jar')
    z.write(bundle,'CareerUp-Sprint4/team-change.bundle')
    for path in native.rglob('*'):
        if path.is_file():z.write(path,'CareerUp-Sprint4/'+path.relative_to(native).as_posix())
    z.writestr('CareerUp-Sprint4/PACKAGE-VERSION.txt','Source commit: '+commit+'\nApplication SHA256: '+hashlib.sha256(jar.read_bytes()).hexdigest()+'\nWindows: CareerUp.exe. Mac: Start CareerUp.command (macOS 15+).\nDevelopment shortcuts are in Team Tools. Full source and prebuilt app included.\nWindows includes Java; Mac downloads verified architecture-specific Java/MySQL on first use.\nNo PowerShell is used by the Windows startup or rebuild buttons.\nWindows checked locally; Mac archives/checksums checked, but startup has not been executed on an actual Mac.\n')

# 4. PACKAGE INTEGRITY
with zipfile.ZipFile(output) as z:
    assert z.testzip() is None
    names=z.namelist()
    assert all('/.local/' not in n and '/node_modules/' not in n and '/.git/' not in n for n in names)
    assert 'CareerUp-Sprint4/START HERE.txt' in names and 'CareerUp-Sprint4/frontend/src/main.jsx' in names
    assert 'CareerUp-Sprint4/CareerUp.exe' in names and 'CareerUp-Sprint4/runtime/bin/java.exe' in names
    assert 'CareerUp-Sprint4/Start CareerUp.command' in names
    assert 'CareerUp-Sprint4/Team Tools/Pull Latest and Run.cmd' in names
    assert 'CareerUp-Sprint4/Team Tools/Pull Latest and Run.command' in names
    assert 'CareerUp-Sprint4/launcher/mac-dependencies.properties' in names
    assert not any(n.count('/')==1 and n.endswith(('.cmd','.ps1')) for n in names)
print('Created',output,'('+str(round(output.stat().st_size/1024/1024,1))+' MB)')
print('Source commit:',commit)
