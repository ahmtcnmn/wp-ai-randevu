package com.appointflow.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TwoFactorSetupResponse {
    /** Base32 encoded TOTP secret — kullanici authenticator'a manuel ekleyebilir */
    private String secret;
    /** Data URI formatinda QR code (data:image/png;base64,...) */
    private String qrCodeDataUri;
    /** otpauth:// URL — frontend kendi QR uretmek istiyorsa */
    private String otpauthUrl;
}
