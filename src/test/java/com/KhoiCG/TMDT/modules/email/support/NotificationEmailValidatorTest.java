package com.KhoiCG.TMDT.modules.email.support;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationEmailValidatorTest {

    @Test
    void acceptsSimpleAddress() {
        assertTrue(NotificationEmailValidator.isValidRecipientEmail("user@example.com"));
    }

    @Test
    void rejectsNullBlankInvalid() {
        assertFalse(NotificationEmailValidator.isValidRecipientEmail(null));
        assertFalse(NotificationEmailValidator.isValidRecipientEmail(""));
        assertFalse(NotificationEmailValidator.isValidRecipientEmail("   "));
        assertFalse(NotificationEmailValidator.isValidRecipientEmail("no-at-sign"));
    }
}
