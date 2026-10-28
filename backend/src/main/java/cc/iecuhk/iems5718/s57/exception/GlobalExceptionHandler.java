package cc.iecuhk.iems5718.s57.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<String> status(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode()).body(ex.getReason());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class,
            ServletRequestBindingException.class, MethodArgumentTypeMismatchException.class,
            IllegalArgumentException.class, ArithmeticException.class, jakarta.validation.ConstraintViolationException.class})
    public ResponseEntity<String> invalid(Exception ex) {
        return ResponseEntity.badRequest().body("Invalid request. Check the submitted fields.");
    }

    @ExceptionHandler({ConcurrencyFailureException.class, DataIntegrityViolationException.class})
    public ResponseEntity<String> conflict(Exception ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body("The data changed. Please refresh and try again.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> unexpected(Exception ex) {
        log.error("Request failed: {}", ex.getClass().getSimpleName());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred. Please try again.");
    }
}
