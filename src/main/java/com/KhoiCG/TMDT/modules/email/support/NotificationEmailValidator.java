package com.KhoiCG.TMDT.modules.email.support;

import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NotificationEmailValidator {

    public static boolean isValidRecipientEmail(String email) {
        if (email == null) {
            return false;
        }
        String trimmed = email.trim();
        if (trimmed.isEmpty()) {
            return false;
        }
        try {
            var addr = new InternetAddress(trimmed);
            addr.validate();
            return true;
        } catch (AddressException e) {
            return false;
        }
    }
}
