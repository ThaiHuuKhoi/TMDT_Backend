package com.KhoiCG.TMDT.common.exception;

import com.KhoiCG.TMDT.common.config.RequestCorrelationFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("Missing param trả đúng code và status")
    void handleMissingParam_ReturnsBadRequest() {
        ResponseEntity<?> response = handler.handleMissingParam(
                new MissingServletRequestParameterException("email", "String")
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals("MISSING_REQUEST_PARAMETER", body.get("code"));
    }

    @Test
    @DisplayName("Unhandled exception trả requestId để truy vết")
    void handleUnhandledException_IncludesRequestId() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute(RequestCorrelationFilter.REQUEST_ID_ATTR)).thenReturn("req-123");

        ResponseEntity<?> response = handler.handleUnhandledException(new RuntimeException("boom"), request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals("INTERNAL_SERVER_ERROR", body.get("code"));
        assertEquals("req-123", body.get("requestId"));
    }
}
