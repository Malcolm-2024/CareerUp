"""Build the native Windows app image with a redistributable Java runtime."""
from pathlib import Path
import os,subprocess,shutil
root=Path(__file__).resolve().parents[1]
jdk=Path(os.environ['JAVA_HOME']);local=root/'.local'
classes=local/'launcher-classes';inputs=local/'native-input';runtime=local/'native-runtime'
classes.mkdir(parents=True,exist_ok=True);inputs.mkdir(parents=True,exist_ok=True)
subprocess.run([str(jdk/'bin/javac.exe'),'--release','21','-d',str(classes),str(root/'launcher/src/CareerUpLauncher.java')],check=True)
subprocess.run([str(jdk/'bin/jar.exe'),'--create','--file',str(inputs/'careerup-launcher.jar'),'--main-class','CareerUpLauncher','-C',str(classes),'.'],check=True)
shutil.copy2(inputs/'careerup-launcher.jar',root/'launcher/careerup-launcher.jar')
if not runtime.exists():
    subprocess.run([str(jdk/'bin/jlink.exe'),'--add-modules','java.se,jdk.compiler,jdk.zipfs,jdk.unsupported,jdk.crypto.ec','--strip-debug','--no-header-files','--no-man-pages','--compress=zip-6','--output',str(runtime)],check=True)
    shutil.copy2(jdk/'lib/ct.sym',runtime/'lib/ct.sym')
dest=local/'windows-app'
if (dest/'CareerUp').exists():raise SystemExit('Use a fresh output directory for jpackage, preserving any current test image.')
subprocess.run([str(jdk/'bin/jpackage.exe'),'--type','app-image','--name','CareerUp','--app-version','1.0.0','--input',str(inputs),'--main-jar','careerup-launcher.jar','--main-class','CareerUpLauncher','--runtime-image',str(runtime),'--dest',str(dest)],check=True)
print('Native app image built with Java included.')
