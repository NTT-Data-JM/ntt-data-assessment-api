package com.nttdata.assessment.api.service.validation;

import com.nttdata.assessment.api.api.dto.DepthFocusRequest;
import com.nttdata.assessment.api.api.dto.QuizSetupRequest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QuizValidationTest {

    @Test
    void acceptsValidSetup() {
        QuizSetupRequest request = new QuizSetupRequest(
                "java-developer",
                "JUNIOR",
                new DepthFocusRequest("EXACT", List.of("NOVATO")),
                "BOTH",
                List.of("solid")
        );
        assertDoesNotThrow(() -> QuizValidation.validateSetup(request));
    }

    @Test
    void rejectsBadMode() {
        QuizSetupRequest request = new QuizSetupRequest(
                "java-developer",
                "JUNIOR",
                new DepthFocusRequest("RECOMMENDED", List.of()),
                "NOPE",
                List.of()
        );
        assertThrows(IllegalArgumentException.class, () -> QuizValidation.validateSetup(request));
    }
}
