"""Package the complete repository plus the runnable Sprint 5 Windows assets.
Usage: python scripts/package_project.py --output /path/CareerUp-Sprint5.zip
Private runtime data, credentials, caches, .git and build intermediates are excluded.
"""
from pathlib import Path
import argparse,subprocess,zipfile
app=Path(__file__).resolve().parents[1]
parser=argparse.ArgumentParser();parser.add_argument('--output',required=True)
output=Path(parser.parse_args().output).resolve();output.parent.mkdir(parents=True,exist_ok=True)
repo=Path(subprocess.check_output(['git','-C',str(app),'rev-parse','--show-toplevel'],text=True).strip())
tracked=subprocess.check_output(['git','-C',str(repo),'ls-files','-z']).decode().split('\0')
files={repo/name for name in tracked if name}
# Git checkouts reuse Sprint 4's tracked Java runtime. The ZIP includes a self-contained Sprint 5 launcher too.
for relative in ['runtime','app','CareerUp.exe']:
 source=app/relative
 if source.is_file():files.add(source)
 elif source.is_dir():files.update(p for p in source.rglob('*') if p.is_file())
required=[app/'release/careerup-1.0.0.jar',app/'launcher/careerup-launcher.jar',app/'Start-CareerUp.bat']
assert all(p.exists() for p in required),'Build Sprint 5 before packaging'
files.update(required)
for p in files:
 rel=p.relative_to(repo)
 assert not any(part in {'.local','.git','node_modules','target','__pycache__'} for part in rel.parts),str(rel)
 assert p.is_file(),str(p)
with zipfile.ZipFile(output,'w',zipfile.ZIP_DEFLATED,compresslevel=6) as z:
 for p in sorted(files):
  name='CareerUp/'+p.relative_to(repo).as_posix()
  if p.suffix in {'.sh','.command'}:
   info=zipfile.ZipInfo(name);info.create_system=3;info.external_attr=0o100755<<16;info.compress_type=zipfile.ZIP_DEFLATED
   z.writestr(info,p.read_bytes().replace(b'\r\n',b'\n'))
  else:z.write(p,name)
with zipfile.ZipFile(output) as z:assert z.testzip() is None
print('Created',output,'with',len(files),'files; no private runtime data included.')
