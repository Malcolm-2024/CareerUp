package io.careerup;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.dao.DataAccessException;

@RestControllerAdvice
public class ApiErrors {
  // ERROR MESSAGES: user-facing text only; never return SQL, paths or Java stack traces.
  @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
  ResponseEntity<?> tooLarge() {return ResponseEntity.status(413).body(Map.of("message","That upload is too large. Pictures must be under 10 MB; videos under 50 MB."));}
  @ExceptionHandler(org.springframework.web.multipart.MultipartException.class)
  ResponseEntity<?> multipart() {return ResponseEntity.badRequest().body(Map.of("message","The upload was incomplete. Choose your file again and retry."));}
  @ExceptionHandler(java.io.IOException.class)
  ResponseEntity<?> storage() {return ResponseEntity.status(503).body(Map.of("message","CareerUp could not store this file. Check available disk space and try again."));}
  @ExceptionHandler(ResponseStatusException.class)
  ResponseEntity<?> expected(ResponseStatusException error) {
    return ResponseEntity.status(error.getStatusCode()).body(Map.of("message",error.getReason()==null?"Unable to complete this action.":error.getReason()));
  }
  @ExceptionHandler(HttpMessageNotReadableException.class)
  ResponseEntity<?> invalid() {return ResponseEntity.badRequest().body(Map.of("message","Enter valid submission data."));}
  @ExceptionHandler(DataAccessException.class)
  ResponseEntity<?> database() {return ResponseEntity.status(503).body(Map.of("message","Unable to save right now. Please retry; your submission will not be duplicated."));}
}
