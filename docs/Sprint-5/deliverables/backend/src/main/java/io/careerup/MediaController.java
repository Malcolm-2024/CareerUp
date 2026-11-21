package io.careerup;

import jakarta.servlet.MultipartConfigElement;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageInputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.*;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.*;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/** Local media stays outside the JAR and Git; MySQL stores the normal post/media rows. */
@RestController
public class MediaController {
  // 1. UPLOAD LIMITS — keep these matched with media rules in frontend/src/requests.js.
  static final long IMAGE_LIMIT = 10L * 1024 * 1024;
  static final long VIDEO_LIMIT = 50L * 1024 * 1024;
  private final Path uploads, staging;
  private final NamedParameterJdbcTemplate db;
  private final TransactionTemplate transaction;

  public MediaController(NamedParameterJdbcTemplate db, PlatformTransactionManager manager,
      @Value("${careerup.upload-dir:.local/uploads}") String directory) throws IOException {
    this.db = db;
    this.transaction = new TransactionTemplate(manager);
    uploads = Path.of(directory).toAbsolutePath().normalize();
    staging = uploads.resolveSibling("upload-staging");
    Files.createDirectories(uploads);
    Files.createDirectories(staging);
  }

  // Multipart data is spooled to disk immediately, rather than buffering videos in RAM.
  @Bean
  MultipartConfigElement multipartConfigElement() {
    return new MultipartConfigElement(staging.toString(), VIDEO_LIMIT, VIDEO_LIMIT + 1024 * 1024, 0);
  }

  private static ResponseStatusException invalid(String message) {
    return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
  }

  // 2. FILE VALIDATION — check actual bytes as well as the extension; never trust MIME alone.
  private String validate(MultipartFile file) throws IOException {
    if (file == null || file.isEmpty()) throw invalid("Choose a picture or video from your computer.");
    String name = Objects.toString(file.getOriginalFilename(), "").toLowerCase(Locale.ROOT);
    String extension = name.substring(name.lastIndexOf('.') + 1);
    if (extension.equals("jpeg")) extension = "jpg";
    boolean image = Set.of("jpg", "png", "gif").contains(extension);
    if (!image && !Set.of("mp4", "webm").contains(extension))
      throw invalid("Choose a JPG, PNG or GIF picture, or an MP4 or WebM video. Convert HEIC/MOV files first.");
    if (file.getSize() > (image ? IMAGE_LIMIT : VIDEO_LIMIT))
      throw invalid(image ? "That picture is too large. Choose one under 10 MB." : "That video is too large. Choose one under 50 MB.");
    byte[] header;
    try (var input = file.getInputStream()) { header = input.readNBytes(4096); }
    String ascii = new String(header, StandardCharsets.ISO_8859_1);
    boolean signature = switch (extension) {
      case "jpg" -> header.length > 3 && (header[0] & 255) == 255 && (header[1] & 255) == 216 && (header[2] & 255) == 255;
      case "png" -> header.length >= 8 && Arrays.equals(Arrays.copyOf(header, 8), new byte[]{(byte)137,80,78,71,13,10,26,10});
      case "gif" -> ascii.startsWith("GIF87a") || ascii.startsWith("GIF89a");
      case "mp4" -> header.length >= 24 && ascii.substring(4,8).equals("ftyp") &&
          List.of("isom", "iso2", "mp41", "mp42", "avc1", "M4V ", "dash").stream().anyMatch(ascii::contains);
      case "webm" -> header.length >= 32 && (header[0] & 255) == 26 && (header[1] & 255) == 69 &&
          (header[2] & 255) == 223 && (header[3] & 255) == 163 && ascii.contains("webm");
      default -> false;
    };
    if (!signature) throw invalid("That file does not match its picture/video format. Choose a valid file, not a renamed document.");
    if (image) {
      try (var input = new MemoryCacheImageInputStream(file.getInputStream())) {
        var readers = ImageIO.getImageReaders(input);
        if (!readers.hasNext()) throw invalid("This picture cannot be read. Export it as JPG or PNG and try again.");
        var reader = readers.next();
        try {
          reader.setInput(input, true, true);
          if ((long)reader.getWidth(0) * reader.getHeight(0) > 16_000_000)
            throw invalid("This picture has very large dimensions. Resize it to 16 megapixels or less.");
          if (reader.read(0) == null) throw new IOException("Unreadable picture");
        } finally { reader.dispose(); }
      } catch (IOException corrupted) {
        throw invalid("This picture is damaged or incomplete. Export a fresh JPG or PNG and try again.");
      }
    }
    return extension;
  }

  // 3. PUBLISH FROM COMPUTER — one transaction saves the post, media reference and retry receipt.
  @PostMapping(value="/api/student/posts/upload", consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
  Map<String,Object> publish(@AuthenticationPrincipal SecurityConfig.Account account,
      @RequestHeader(value="Idempotency-Key", required=false) String key,
      @RequestParam(defaultValue="") String title, @RequestParam(defaultValue="") String description,
      @RequestParam(required=false) MultipartFile file) throws IOException, NoSuchAlgorithmException {
    if (key == null || !key.matches("[a-zA-Z0-9-]{16,80}")) throw invalid("Please refresh the page and submit your post again.");
    title = title.strip(); description = description.strip();
    if (title.isEmpty()) throw invalid("Enter a title for your portfolio post.");
    if (title.length() > 255) throw invalid("Keep the title to 255 characters or fewer.");
    if (description.isEmpty()) throw invalid("Describe your portfolio project before publishing.");
    if (description.length() > 10000) throw invalid("Keep the description to 10,000 characters or fewer.");
    String extension = validate(file), mediaType = Set.of("mp4","webm").contains(extension) ? "VIDEO" : "IMAGE";
    Path saved = uploads.resolve(UUID.randomUUID() + "." + extension);
    String url = "/uploads/" + saved.getFileName(), route = "/api/student/posts/upload";
    boolean keep = false;
    try {
      file.transferTo(saved.toFile());
      if (extension.equals("mp4")) validateMp4(saved);
      var digest = MessageDigest.getInstance("SHA-256");
      digest.update((title + "\0" + description + "\0" + mediaType + "\0").getBytes(StandardCharsets.UTF_8));
      try (var stream = Files.newInputStream(saved)) {
        byte[] chunk = new byte[8192]; int count;
        while ((count = stream.read(chunk)) != -1) digest.update(chunk,0,count);
      }
      String hash = HexFormat.of().formatHex(digest.digest());
      var args = new HashMap<String,Object>();
      args.put("user",account.id); args.put("key",key); args.put("path",route); args.put("hash",hash);
      args.put("title",title); args.put("description",description); args.put("url",url); args.put("type",mediaType);
      long id = Objects.requireNonNull(transaction.execute(status -> {
        try { db.update("INSERT INTO submission_keys(user_id,request_key,path,body_hash) VALUES(:user,:key,:path,:hash)",args); }
        catch (DuplicateKeyException duplicate) {
          var receipt = db.queryForMap("SELECT path,body_hash,response_body FROM submission_keys WHERE user_id=:user AND request_key=:key",args);
          if (!route.equals(receipt.get("path")) || !hash.equals(receipt.get("body_hash")))
            throw new ResponseStatusException(HttpStatus.CONFLICT,"This submission changed. Choose Publish again to start a new submission.");
          return Long.parseLong((String)receipt.get("response_body"));
        }
        var generated = new GeneratedKeyHolder();
        db.update("INSERT INTO posts(author_id,major_id,post_type,title,description,status) SELECT id,major_id,'PORTFOLIO',:title,:description,'PUBLISHED' FROM users WHERE id=:user",
            new MapSqlParameterSource(args),generated,new String[]{"id"});
        long post = Objects.requireNonNull(generated.getKey()).longValue(); args.put("post",post);
        db.update("INSERT INTO post_media(post_id,media_type,url) VALUES(:post,:type,:url)",args);
        db.update("UPDATE submission_keys SET response_body=:post,response_status=200 WHERE user_id=:user AND request_key=:key",args);
        return post;
      }));
      keep = db.queryForObject("SELECT COUNT(*) FROM post_media WHERE url=:url",Map.of("url",url),Integer.class) > 0;
      return Map.of("id",id,"message","Portfolio post published.");
    } catch (RuntimeException failure) {
      // If a connection dies during COMMIT, retain media unless rollback can be confirmed.
      try { keep = db.queryForObject("SELECT COUNT(*) FROM post_media WHERE url=:url",Map.of("url",url),Integer.class) > 0; }
      catch (RuntimeException unavailable) { keep = true; }
      throw failure;
    } finally { if (!keep) Files.deleteIfExists(saved); }
  }

  // Reject truncated MP4 containers; codec compatibility is reported by the video player.
  private static void validateMp4(Path file) throws IOException {
    try (var input = new RandomAccessFile(file.toFile(), "r")) {
      boolean movie = false, data = false; int boxes = 0;
      while (input.getFilePointer() < input.length()) {
        long start = input.getFilePointer(), remaining = input.length() - start;
        if (remaining < 8 || ++boxes > 10000) throw invalid("This MP4 is damaged or incomplete. Export a fresh video and try again.");
        long size = Integer.toUnsignedLong(input.readInt());
        byte[] type = new byte[4]; input.readFully(type);
        String name = new String(type, StandardCharsets.US_ASCII);
        int header = 8;
        if (size == 1) { if (remaining < 16) throw invalid("This MP4 is incomplete."); size = input.readLong(); header = 16; }
        if (size == 0) size = remaining;
        if (size < header || size > remaining) throw invalid("This MP4 is damaged or incomplete. Export a fresh video and try again.");
        movie |= name.equals("moov") && size > header;
        data |= name.equals("mdat") && size > header;
        input.seek(start + size);
      }
      if (!movie || !data) throw invalid("This MP4 is missing its video data. Export a fresh video and try again.");
    }
  }

  // 4. MEDIA DISPLAY — stable filenames survive logout/restart; Spring supports video range requests.
  @GetMapping("/uploads/{name:[a-f0-9-]+\\.(?:jpg|png|gif|mp4|webm)}")
  ResponseEntity<Resource> media(@PathVariable String name) throws IOException {
    Path file = uploads.resolve(name).normalize();
    if (!file.startsWith(uploads) || !Files.isRegularFile(file) || Files.isSymbolicLink(file) ||
        db.queryForObject("SELECT COUNT(*) FROM post_media m JOIN posts p ON p.id=m.post_id JOIN users u ON u.id=p.author_id WHERE m.url=:url AND p.status='PUBLISHED' AND u.status='ACTIVE'",Map.of("url","/uploads/"+name),Integer.class) == 0)
      throw new ResponseStatusException(HttpStatus.NOT_FOUND,"This project media is no longer available.");
    String type = switch (name.substring(name.lastIndexOf('.')+1)) {
      case "jpg" -> "image/jpeg"; case "png" -> "image/png"; case "gif" -> "image/gif";
      case "mp4" -> "video/mp4"; default -> "video/webm";
    };
    return ResponseEntity.ok().contentType(MediaType.parseMediaType(type))
        .header("X-Content-Type-Options","nosniff").header("Cache-Control","private, max-age=86400")
        .body(new FileSystemResource(file));
  }
}
