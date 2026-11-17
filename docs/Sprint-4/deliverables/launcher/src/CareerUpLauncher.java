import java.awt.*;
import java.io.*;
import java.net.*;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.time.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.zip.*;
import javax.swing.*;

public class CareerUpLauncher {
  // 1. PROJECT FILES AND STARTUP OPTIONS
  private Path root, local, java, mysql;
  private final Properties state = new Properties(), dependencies = new Properties();
  private JFrame frame;
  private JLabel message;
  private boolean headless, noBrowser, portableMysql;
  private int appPort, dbPort;
  private String mode = "start";

  public static void main(String[] args) {
    CareerUpLauncher app = new CareerUpLauncher();
    try { app.start(args); }
    catch (Exception error) {
      error.printStackTrace();
      if (!app.headless) JOptionPane.showMessageDialog(app.frame,
          error.getMessage() + "\n\nDetails are saved in .local/launcher.log.",
          "CareerUp could not start", JOptionPane.ERROR_MESSAGE);
      if (app.frame != null) app.frame.dispose();
      System.exit(1);
    }
  }

  private void start(String[] args) throws Exception {
    Path location = Path.of(getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
    root = location.getParent().getParent();
    for (String arg : args) {
      if (arg.startsWith("--root=")) root = Path.of(arg.substring(7)).toAbsolutePath();
      else if (arg.equals("--headless")) headless = true;
      else if (arg.equals("--no-browser")) noBrowser = true;
      else if (arg.equals("--portable-mysql")) portableMysql = true;
      else if (arg.startsWith("--app-port=")) appPort = Integer.parseInt(arg.substring(11));
      else if (arg.startsWith("--db-port=")) dbPort = Integer.parseInt(arg.substring(10));
      else if (arg.equals("--rebuild")) mode = "rebuild";
      else if (arg.equals("--update")) mode = "update";
      else if (arg.equals("--stop")) mode = "stop";
    }
    if (!Files.isRegularFile(root.resolve("backend/pom.xml"))) throw new IOException("Extract the entire CareerUp folder before opening CareerUp.exe.");
    local = root.resolve(".local"); Files.createDirectories(local);
    PrintStream log = new PrintStream(Files.newOutputStream(local.resolve("launcher.log"), StandardOpenOption.CREATE, StandardOpenOption.APPEND), true, StandardCharsets.UTF_8);
    System.setOut(log); System.setErr(log);
    System.out.println("\nCareerUp launcher " + Instant.now() + " mode=" + mode);
    if (Files.exists(local.resolve("launcher.properties"))) try (InputStream in = Files.newInputStream(local.resolve("launcher.properties"))) { state.load(in); }
    try (InputStream in = Files.newInputStream(root.resolve("launcher/dependencies.properties"))) { dependencies.load(in); }
    java = Path.of(System.getProperty("java.home"), "bin", "java.exe");
    if (!headless) showWindow();
    try (FileChannel channel = FileChannel.open(local.resolve("launcher.lock"), StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
      FileLock lock = channel.tryLock();
      if (lock == null) throw new IOException("CareerUp is already starting. Wait for its browser window.");
      try (lock) {
        if (mode.equals("stop")) { stop("backend"); stopDatabase(); status("CareerUp stopped. Your demo data is saved."); finish(); return; }
        appPort = appPort == 0 ? Integer.parseInt(state.getProperty("app.port", "0")) : appPort;
        dbPort = dbPort == 0 ? Integer.parseInt(state.getProperty("db.port", "0")) : dbPort;
        if (appPort == 0) appPort = availablePort(8080);
        if (dbPort == 0) dbPort = availablePort(3307);
        state.setProperty("app.port", "" + appPort); state.setProperty("db.port", "" + dbPort);
        state.putIfAbsent("app.password", randomPassword()); state.putIfAbsent("root.password", randomPassword()); save();
        if (mode.equals("update")) {
          status("Pulling the team's current code...");
          Path git = findExecutable("git.exe", List.of(Path.of("C:/Program Files/Git/cmd/git.exe")));
          if (git == null || !Files.isDirectory(root.resolve(".git"))) throw new IOException("Pull Latest must be run in your team's Git checkout. The ZIP is a runnable copy; see START HERE.txt for the one-time team import.");
          if (!run(List.of(git.toString(), "status", "--porcelain"), null, Map.of()).isBlank()) throw new IOException("You have local edits. Commit or save them before pulling. No edits were discarded.");
          run(List.of(git.toString(), "pull", "--ff-only"), null, Map.of());
        }
        if (!mode.equals("start")) { stop("backend"); build(); }
        else if (!Files.exists(appJar())) build();
        if (owned("backend") && ready()) { openBrowser(); finish(); return; }
        if (portOpen(appPort)) throw new IOException("Another application is using port " + appPort + ". Close that copy of CareerUp and try again.");
        status("Preparing your MySQL demo database...");
        mysql = mysqlHome(); state.setProperty("mysql.home", mysql.toString()); save();
        prepareDatabase();
        status("Starting CareerUp...");
        ProcessBuilder backend = new ProcessBuilder(java.toString(), "-jar", appJar().toString(), "--spring.profiles.active=demo");
        backend.environment().put("DB_URL", "jdbc:mysql://127.0.0.1:" + dbPort + "/careerup_sprint4?connectionTimeZone=UTC");
        backend.environment().put("DB_USER", "careerup_app"); backend.environment().put("DB_PASSWORD", state.getProperty("app.password")); backend.environment().put("PORT", "" + appPort);
        spawn("backend", backend);
        for (int n = 0; n < 120 && !ready(); n++) { if (!owned("backend")) throw new IOException("The application stopped while starting. Open .local/backend.log for the error."); Thread.sleep(500); }
        if (!ready()) throw new IOException("CareerUp did not become ready. Open .local/backend.log for the error.");
        writeDatabaseConnection();
        System.out.println("READY " + url()); Files.writeString(local.resolve("READY.txt"), url());
        status("CareerUp is ready."); openBrowser(); finish();
      }
    }
  }

  // 2. SMALL STARTUP WINDOW (NO POWERSHELL OR SECURITY-SETTING CHANGES)
  private void showWindow() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      frame = new JFrame("CareerUp"); frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
      JPanel panel = new JPanel(new BorderLayout(14, 22)); panel.setBackground(new Color(15, 15, 26)); panel.setBorder(BorderFactory.createEmptyBorder(30, 32, 30, 32));
      JLabel title = new JLabel("CareerUp"); title.setForeground(Color.WHITE); title.setFont(new Font("SansSerif", Font.BOLD, 30));
      JLabel tagline = new JLabel("From Resumes to Reels"); tagline.setForeground(new Color(190, 174, 230));
      JPanel brand = new JPanel(new GridLayout(2, 1)); brand.setOpaque(false); brand.add(title); brand.add(tagline); panel.add(brand, BorderLayout.NORTH);
      message = new JLabel("Starting your application..."); message.setForeground(Color.WHITE); message.setFont(new Font("SansSerif", Font.PLAIN, 13)); panel.add(message, BorderLayout.CENTER);
      JProgressBar progress = new JProgressBar(); progress.setIndeterminate(true); progress.setForeground(new Color(168, 85, 247)); panel.add(progress, BorderLayout.SOUTH);
      frame.setContentPane(panel); frame.setSize(520, 230); frame.setLocationRelativeTo(null); frame.setVisible(true);
    });
  }
  private void status(String text) { System.out.println(text); if (message != null) SwingUtilities.invokeLater(() -> message.setText(text)); }
  private void finish() { if (frame != null) SwingUtilities.invokeLater(() -> frame.dispose()); }
  private void openBrowser() throws Exception { if (!noBrowser) Desktop.getDesktop().browse(URI.create(url())); }
  private String url() { return "http://127.0.0.1:" + appPort; }
  private Path appJar() { Path built = root.resolve("backend/target/careerup-1.0.0.jar"); return Files.exists(built) ? built : root.resolve("release/careerup-1.0.0.jar"); }

  // 3. PRIVATE MYSQL SETUP (NEVER RESET THE USER'S NORMAL DATABASE)
  private Path mysqlHome() throws Exception {
    List<Path> candidates = new ArrayList<>();
    if (!portableMysql) {
      if (System.getenv("MYSQL_HOME") != null) candidates.add(Path.of(System.getenv("MYSQL_HOME")));
      candidates.add(Path.of("C:/Program Files/MySQL/MySQL Server 8.0")); candidates.add(Path.of("C:/Program Files/MySQL/MySQL Server 8.4"));
    }
    candidates.add(local.resolve("tools/mysql-8.4.13-winx64"));
    for (Path p : candidates) if (Files.isRegularFile(p.resolve("bin/mysqld.exe")) && Files.isRegularFile(p.resolve("bin/mysql.exe"))) return p;
    status("Downloading a private MySQL copy (first run only)...");
    return installZip("mysql", "mysql-8.4.13-winx64");
  }
  private void prepareDatabase() throws Exception {
    Path data = local.resolve("native-mysql-data");
    if (!owned("mysql")) {
      if (portOpen(dbPort)) throw new IOException("Another database is using port " + dbPort + ". No existing database was changed.");
      if (!Files.isDirectory(data.resolve("mysql"))) {
        Files.createDirectories(data);
        status("Creating your demo database (first run only)...");
        run(List.of(mysql.resolve("bin/mysqld.exe").toString(), "--no-defaults", "--initialize-insecure", "--datadir=" + data, "--console"), null, Map.of());
      }
      spawn("mysql", new ProcessBuilder(mysql.resolve("bin/mysqld.exe").toString(), "--no-defaults", "--datadir=" + data, "--port=" + dbPort, "--bind-address=127.0.0.1", "--mysqlx=0", "--console"));
      for (int i=0; i<100 && !portOpen(dbPort); i++) { if (!owned("mysql")) throw new IOException("MySQL could not start. See .local/mysql.log."); Thread.sleep(300); }
    }
    if (!Boolean.parseBoolean(state.getProperty("root.configured", "false"))) {
      try { sql("SELECT 1;"); }
      catch (IOException notConfigured) { sql("ALTER USER 'root'@'localhost' IDENTIFIED BY '" + state.getProperty("root.password") + "';", ""); }
      state.setProperty("root.configured", "true"); save();
    }
    String exists = sql("SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name='careerup_sprint4';").strip();
    if (exists.equals("0")) {
      sql("CREATE DATABASE careerup_sprint4 CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;");
      sql("USE careerup_sprint4;\n" + Files.readString(root.resolve("db/sprint4_schema.sql")) + "\n" + Files.readString(root.resolve("db/sprint4_seed.sql")));
    }
    sql("CREATE USER IF NOT EXISTS 'careerup_app'@'localhost' IDENTIFIED BY '" + state.getProperty("app.password") + "'; GRANT SELECT,INSERT,UPDATE ON careerup_sprint4.* TO 'careerup_app'@'localhost';");
  }
  private String sql(String text) throws Exception { return sql(text, state.getProperty("root.password")); }
  private String sql(String text, String password) throws Exception {
    return run(List.of(mysql.resolve("bin/mysql.exe").toString(), "--no-defaults", "--host=127.0.0.1", "--port=" + dbPort, "--protocol=TCP", "-u", "root", "--default-character-set=utf8mb4", "-N"), text, Map.of("MYSQL_PWD", password));
  }
  private void writeDatabaseConnection() throws IOException {
    Files.writeString(local.resolve("IntelliJ MySQL Connection.txt"), "Host: 127.0.0.1\nPort: " + dbPort + "\nDatabase: careerup_sprint4\nUser: careerup_app\nPassword: " + state.getProperty("app.password") + "\n\nPrivate local connection. Do not upload this file.\n");
  }

  // 4. REBUILD REACT AND SPRING BOOT AFTER CODE CHANGES
  private void build() throws Exception {
    status("Installing build tools if needed...");
    Path node = findExecutable("node.exe", List.of(Path.of("C:/Program Files/nodejs/node.exe"), local.resolve("tools/node-v22.23.3-win-x64/node.exe")));
    if (node == null) node = installZip("node", "node-v22.23.3-win-x64").resolve("node.exe");
    Path npm = node.getParent().resolve("node_modules/npm/bin/npm-cli.js");
    if (!Files.exists(npm)) throw new IOException("This Node installation does not include npm. Install Node.js with npm, then retry Rebuild.");
    status("Building the React frontend...");
    Path frontend = root.resolve("frontend");
    run(List.of(node.toString(), npm.toString(), "ci", "--ignore-scripts", "--no-audit", "--no-fund", "--cache", local.resolve("npm-cache").toString()), null, Map.of(), frontend);
    Path esbuild = frontend.resolve("node_modules/@esbuild/win32-x64/esbuild.exe");
    run(List.of(esbuild.toString(), "src/main.jsx", "--bundle", "--minify", "--format=esm", "--target=es2022", "--outdir=dist/assets", "--loader:.woff2=file", "--loader:.woff=file", "--asset-names=[name]-[hash]"), null, Map.of(), frontend);
    copyTree(frontend.resolve("public"), frontend.resolve("dist"));
    String html = Files.readString(frontend.resolve("index.html")).replace("<script type=\"module\" src=\"/src/main.jsx\"></script>", "<link rel=\"stylesheet\" href=\"/assets/main.css\"/><script type=\"module\" src=\"/assets/main.js\"></script>");
    Files.writeString(frontend.resolve("dist/index.html"), html);
    copyTree(frontend.resolve("dist"), root.resolve("backend/src/main/resources/static"));
    Path maven = local.resolve("tools/apache-maven-3.9.11");
    if (!Files.exists(maven.resolve("bin/m2.conf"))) maven = installZip("maven", "apache-maven-3.9.11");
    Path boot;
    try (var stream = Files.list(maven.resolve("boot"))) { boot = stream.filter(p -> p.getFileName().toString().startsWith("plexus-classworlds-") && p.toString().endsWith(".jar")).findFirst().orElseThrow(); }
    status("Building the Java backend...");
    run(List.of(java.toString(), "-Dmaven.home=" + maven, "-Dmaven.multiModuleProjectDirectory=" + root.resolve("backend"), "-Dclassworlds.conf=" + maven.resolve("bin/m2.conf"), "-classpath", boot.toString(), "org.codehaus.plexus.classworlds.launcher.Launcher", "-B", "-ntp", "-Dmaven.repo.local=" + local.resolve("m2"), "-f", root.resolve("backend/pom.xml").toString(), "package"), null, Map.of());
  }
  private static void copyTree(Path source, Path target) throws IOException {
    try (var paths = Files.walk(source)) {
      for (Path p : paths.toList()) { Path destination = target.resolve(source.relativize(p)); if (Files.isDirectory(p)) Files.createDirectories(destination); else Files.copy(p, destination, StandardCopyOption.REPLACE_EXISTING); }
    }
  }

  // 5. VERIFIED OFFICIAL DOWNLOADS AND SAFE ZIP EXTRACTION
  private Path installZip(String name, String directory) throws Exception {
    Path downloads = local.resolve("downloads"), tools = local.resolve("tools"); Files.createDirectories(downloads); Files.createDirectories(tools);
    String link = dependencies.getProperty(name + ".url"), checksum = dependencies.getProperty(name + ".sha256");
    if (link == null || checksum == null || checksum.length() != 64) throw new IOException("Missing verified download configuration for " + name);
    Path archive = downloads.resolve(name + ".zip");
    if (!Files.exists(archive) || !sha256(archive).equalsIgnoreCase(checksum)) {
      Path partial = downloads.resolve(name + ".part"); URLConnection connection = URI.create(link).toURL().openConnection(); connection.setConnectTimeout(20000); connection.setReadTimeout(60000);
      long total = connection.getContentLengthLong(), done = 0, last = 0;
      try (InputStream in = connection.getInputStream(); OutputStream out = Files.newOutputStream(partial)) {
        byte[] buffer = new byte[1024*1024]; int count;
        while ((count=in.read(buffer)) >= 0) { out.write(buffer,0,count); done+=count; if (System.currentTimeMillis()-last>1200) { status("Downloading " + name + "... " + (total>0 ? (done*100/total)+"%" : (done/1048576)+" MB")); last=System.currentTimeMillis(); } }
      }
      if (!sha256(partial).equalsIgnoreCase(checksum)) throw new IOException("Download verification failed for " + name + ". Retry without using the partial download.");
      Files.move(partial, archive, StandardCopyOption.REPLACE_EXISTING);
    }
    status("Preparing " + name + "..."); Path stage = tools.resolve("unpack-" + name + "-" + System.currentTimeMillis()); Files.createDirectories(stage);
    try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(archive))) {
      ZipEntry entry;
      while ((entry=zip.getNextEntry()) != null) {
        Path destination=stage.resolve(entry.getName()).normalize();
        if (!destination.startsWith(stage)) throw new IOException("Unsafe archive entry was rejected.");
        if (entry.isDirectory()) Files.createDirectories(destination); else { Files.createDirectories(destination.getParent()); Files.copy(zip,destination,StandardCopyOption.REPLACE_EXISTING); }
      }
    }
    Path result = tools.resolve(directory);
    if (Files.exists(result)) throw new IOException("A partial " + name + " tool folder already exists: " + result);
    Files.move(stage.resolve(directory),result); return result;
  }
  private static String sha256(Path path) throws Exception {
    MessageDigest hash=MessageDigest.getInstance("SHA-256"); try(InputStream in=Files.newInputStream(path)){byte[] b=new byte[1048576];int n;while((n=in.read(b))>=0)hash.update(b,0,n);}return HexFormat.of().formatHex(hash.digest());
  }

  // 6. PROCESS OWNERSHIP, LOGGING AND LOCAL READINESS
  private String run(List<String> command, String input, Map<String,String> env) throws Exception { return run(command,input,env,root); }
  private String run(List<String> command, String input, Map<String,String> env, Path directory) throws Exception {
    ProcessBuilder builder=new ProcessBuilder(command).directory(directory.toFile()).redirectErrorStream(true);builder.environment().putAll(env);
    Path output=local.resolve("command-"+System.nanoTime()+".log");builder.redirectOutput(output.toFile());
    Process process=builder.start();try(OutputStream in=process.getOutputStream()){if(input!=null)in.write(input.getBytes(StandardCharsets.UTF_8));}
    int code=process.waitFor();String text=Files.readString(output);System.out.println(text);
    if(code!=0)throw new IOException(Path.of(command.getFirst()).getFileName()+" failed. "+text.substring(Math.max(0,text.length()-1600)));
    return text;
  }
  private void spawn(String name, ProcessBuilder builder) throws Exception {
    builder.directory(root.toFile()).redirectErrorStream(true).redirectOutput(local.resolve(name+".log").toFile());Process child=builder.start();
    state.setProperty(name+".pid",""+child.pid());state.setProperty(name+".started",child.info().startInstant().orElseThrow().toString());save();
  }
  private boolean owned(String name) {
    try { var process=ProcessHandle.of(Long.parseLong(state.getProperty(name+".pid","0")));return process.isPresent()&&process.get().isAlive()&&process.get().info().startInstant().map(t->t.toString().equals(state.getProperty(name+".started"))).orElse(false); }catch(Exception e){return false;}
  }
  private void stop(String name) throws Exception {
    if(owned(name)){ProcessHandle p=ProcessHandle.of(Long.parseLong(state.getProperty(name+".pid"))).orElseThrow();p.destroy();for(int n=0;n<40&&p.isAlive();n++)Thread.sleep(250);if(p.isAlive())throw new IOException("CareerUp could not stop its "+name+" process.");}
  }
  private void stopDatabase() throws Exception {
    if(!owned("mysql"))return;mysql=Path.of(state.getProperty("mysql.home"));dbPort=Integer.parseInt(state.getProperty("db.port"));sql("SHUTDOWN;");
  }
  private boolean ready() {
    try { HttpURLConnection c=(HttpURLConnection)URI.create(url()+"/api/health").toURL().openConnection();c.setConnectTimeout(500);c.setReadTimeout(1000);String body;try(InputStream in=c.getInputStream()){body=new String(in.readAllBytes(),StandardCharsets.UTF_8);}finally{c.disconnect();}return body.replace(" ","").contains("\"database\":\"UP\""); }catch(Exception e){return false;}
  }
  private static boolean portOpen(int port) {try(Socket s=new Socket()){s.connect(new InetSocketAddress("127.0.0.1",port),250);return true;}catch(Exception e){return false;}}
  private static int availablePort(int start) throws IOException {for(int p=start;p<start+100;p++)if(!portOpen(p))return p;throw new IOException("No free local port was found.");}
  private void save() throws IOException {try(OutputStream out=Files.newOutputStream(local.resolve("launcher.properties"))){state.store(out,"Private CareerUp runtime state. Do not upload.");}}
  private static String randomPassword(){byte[] bytes=new byte[24];new SecureRandom().nextBytes(bytes);return HexFormat.of().formatHex(bytes);}
  private static Path findExecutable(String name,List<Path> candidates){for(Path p:candidates)if(Files.isRegularFile(p))return p;for(String folder:System.getenv().getOrDefault("PATH","").split(File.pathSeparator)){Path p=Path.of(folder).resolve(name);if(Files.isRegularFile(p))return p;}return null;}
}
