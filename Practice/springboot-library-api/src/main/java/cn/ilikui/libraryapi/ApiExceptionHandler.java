package cn.ilikui.libraryapi;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(BookNotFoundException.class)
    public ProblemDetail handleNotFound(BookNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "BOOK_NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + "：" + error.getDefaultMessage())
                .distinct().collect(Collectors.joining("；"));
        return problem(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", detail);
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
    public ProblemDetail handleInvalidQuery(Exception ex) {
        return problem(HttpStatus.BAD_REQUEST, "INVALID_QUERY", "查询参数缺失或格式不正确");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleUnreadableBody(HttpMessageNotReadableException ex) {
        return problem(HttpStatus.BAD_REQUEST, "INVALID_REQUEST_BODY", "请求体不是有效的 JSON，或字段类型不匹配");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleBadQuery(IllegalArgumentException ex) {
        return problem(HttpStatus.BAD_REQUEST, "INVALID_QUERY", ex.getMessage());
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ProblemDetail handleDuplicateKey(DuplicateKeyException ex) {
        return problem(HttpStatus.CONFLICT, "DUPLICATE_ISBN", "ISBN 已存在，请使用唯一的 ISBN");
    }

    @ExceptionHandler(LoanNotFoundException.class)
    public ProblemDetail handleLoanNotFound(LoanNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "LOAN_NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(LibraryConflictException.class)
    public ProblemDetail handleLibraryConflict(LibraryConflictException ex) {
        return problem(HttpStatus.CONFLICT, ex.code(), ex.getMessage());
    }

    private ProblemDetail problem(HttpStatus status, String code, String detail) {
        ProblemDetail result = ProblemDetail.forStatusAndDetail(status, detail);
        result.setProperty("code", code);
        return result;
    }
}
