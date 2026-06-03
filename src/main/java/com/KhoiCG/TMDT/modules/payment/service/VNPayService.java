package com.KhoiCG.TMDT.modules.payment.service;

import com.KhoiCG.TMDT.common.exception.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
@Slf4j
public class VNPayService {

    @Value("${vnpay.tmnCode}")
    private String vnpTmnCode;

    @Value("${vnpay.hashSecret}")
    private String vnpHashSecret;

    @Value("${vnpay.url}")
    private String vnpPayUrl;

    @Value("${vnpay.returnUrl}")
    private String vnpReturnUrl;

    public String createOrder(Long amountVND, String orderInfo, String txnRef, String ipAddress) {
        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", "2.1.0");
        vnp_Params.put("vnp_Command", "pay");
        vnp_Params.put("vnp_TmnCode", vnpTmnCode.trim()); // Đã thêm .trim() để chống lỗi khoảng trắng
        vnp_Params.put("vnp_Amount", String.valueOf(amountVND * 100));
        vnp_Params.put("vnp_CurrCode", "VND");
        vnp_Params.put("vnp_TxnRef", txnRef);
        vnp_Params.put("vnp_OrderInfo", orderInfo);
        vnp_Params.put("vnp_OrderType", "other");
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", vnpReturnUrl.trim());
        vnp_Params.put("vnp_IpAddr", (ipAddress == null || ipAddress.isBlank()) ? "127.0.0.1" : ipAddress);

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        formatter.setTimeZone(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        vnp_Params.put("vnp_CreateDate", formatter.format(cld.getTime()));

        cld.add(Calendar.MINUTE, 15);
        vnp_Params.put("vnp_ExpireDate", formatter.format(cld.getTime()));

        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();

        try {
            Iterator<String> itr = fieldNames.iterator();
            while (itr.hasNext()) {
                String fieldName = itr.next();
                String fieldValue = vnp_Params.get(fieldName);
                if ((fieldValue != null) && (fieldValue.length() > 0)) {
                    // Đã sửa US_ASCII thành UTF_8 để không bị lỗi chữ ký khi có tiếng Việt
                    hashData.append(fieldName).append('=').append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8.toString()));
                    query.append(URLEncoder.encode(fieldName, StandardCharsets.UTF_8.toString())).append('=')
                            .append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8.toString()));
                    if (itr.hasNext()) {
                        query.append('&');
                        hashData.append('&');
                    }
                }
            }
            String queryUrl = query.toString();
            String vnp_SecureHash = hmacSHA512(vnpHashSecret.trim(), hashData.toString());
            queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;

            String finalUrl = vnpPayUrl.trim() + "?" + queryUrl;

            log.info("Đã tạo VNPay URL cho txnRef={}", txnRef);

            return finalUrl;
        } catch (Exception e) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "VNPAY_URL_CREATE_FAILED", "Lỗi tạo URL VNPay");
        }
    }

    // 2. Hàm xác thực chữ ký (Dùng cho cả Return URL và IPN)
    public boolean verifySignature(Map<String, String> fields) {
        Map<String, String> signFields = new HashMap<>(fields);
        String vnp_SecureHash = signFields.get("vnp_SecureHash");
        if (signFields.containsKey("vnp_SecureHashType")) {
            signFields.remove("vnp_SecureHashType");
        }
        signFields.remove("vnp_SecureHash");

        // Hash lại data và so sánh
        List<String> fieldNames = new ArrayList<>(signFields.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        try {
            Iterator<String> itr = fieldNames.iterator();
            while (itr.hasNext()) {
                String fieldName = itr.next();
                String fieldValue = signFields.get(fieldName);
                if ((fieldValue != null) && (fieldValue.length() > 0)) {
                    hashData.append(fieldName).append('=').append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8.toString()));
                    if (itr.hasNext()) {
                        hashData.append('&');
                    }
                }
            }
            String signValue = hmacSHA512(vnpHashSecret.trim(), hashData.toString());
            return signValue.equals(vnp_SecureHash);
        } catch (Exception e) {
            return false;
        }
    }

    // Thuật toán mã hóa của VNPay
    private String hmacSHA512(String key, String data) throws Exception {
        Mac hmac512 = Mac.getInstance("HmacSHA512");
        SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
        hmac512.init(secretKey);
        byte[] result = hmac512.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder(2 * result.length);
        for (byte b : result) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }
}