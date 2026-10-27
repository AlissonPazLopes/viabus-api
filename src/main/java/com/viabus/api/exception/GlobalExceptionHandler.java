package com.viabus.api.exception;

import com.viabus.api.dto.ErroPadraoResponse;
import com.viabus.api.dto.ErroPadraoResponse.ErroCampoValidation;
import jakarta.persistence.OptimisticLockException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErroPadraoResponse> handleResourceNotFound(
            ResourceNotFoundException ex,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI(), null);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErroPadraoResponse> handleBusinessException(
            BusinessException ex,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI(), null);
    }

    @ExceptionHandler(ConflitoConcorrenciaException.class)
    public ResponseEntity<ErroPadraoResponse> handleConflitoConcorrencia(
            ConflitoConcorrenciaException ex,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI(), null);
    }

    @ExceptionHandler({ObjectOptimisticLockingFailureException.class, OptimisticLockException.class})
    public ResponseEntity<ErroPadraoResponse> handleOptimisticLock(
            Exception ex,
            HttpServletRequest request
    ) {
        return buildErrorResponse(
                HttpStatus.CONFLICT,
                "Conflito de concorrência detectado: o assento ou a viagem foi atualizado simultaneamente por outro usuário.",
                request.getRequestURI(),
                null
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroPadraoResponse> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        List<ErroCampoValidation> errosCampos = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(err -> new ErroCampoValidation(err.getField(), err.getDefaultMessage()))
                .toList();

        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Erro de validação nos campos informados.",
                request.getRequestURI(),
                errosCampos
        );
    }

    private ResponseEntity<ErroPadraoResponse> buildErrorResponse(
            HttpStatus status,
            String mensagem,
            String path,
            List<ErroCampoValidation> errosValidacao
    ) {
        ErroPadraoResponse body = new ErroPadraoResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                mensagem,
                path,
                errosValidacao
        );
        return ResponseEntity.status(status).body(body);
    }
}
