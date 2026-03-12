package com.KhoiCG.TMDT.modules.shipping.provider;

import com.KhoiCG.TMDT.modules.shipping.dto.ShippingOrderRequest;
import com.KhoiCG.TMDT.modules.shipping.dto.ShippingOrderResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "shipping", name = "enabled", havingValue = "true", matchIfMissing = false)
@ConditionalOnProperty(name = {"ghn.api.url", "ghn.api.token", "ghn.shop.id"})
public class GhnShippingProvider implements ShippingProvider {

    // Sử dụng RestTemplate để gọi external API
    private final RestTemplate restTemplate;

    @Value("${ghn.api.url:}")
    private String apiUrl;

    @Value("${ghn.api.token:}")
    private String apiToken;

    @Value("${ghn.shop.id:}")
    private String shopId;

    @Value("${ghn.from.district-id:0}")
    private Integer fromDistrictId;

    @Value("${ghn.from.ward-code:}")
    private String fromWardCode;

    @Value("${ghn.service-type-id:2}")
    private Integer serviceTypeId;

    @Override
    public String getProviderCode() {
        return "GHN"; // Phải khớp với chuỗi lưu trong database
    }

    @Override
    public ShippingOrderResponse createShippingOrder(ShippingOrderRequest request) {
        if (apiUrl == null || apiUrl.isBlank() || apiToken == null || apiToken.isBlank() || shopId == null || shopId.isBlank()) {
            throw new RuntimeException("GHN shipping chưa được cấu hình (ghn.api.url / ghn.api.token / ghn.shop.id)");
        }
        String url = apiUrl + "/v2/shipping-order/create";

        // 1. Cài đặt Header bảo mật theo tài liệu GHN
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Token", apiToken);
        headers.set("ShopId", shopId);

        // 2. Xây dựng Body JSON
        Map<String, Object> body = new HashMap<>();
        body.put("payment_type_id", 2); // 1: Shop trả phí, 2: Khách trả phí (Người nhận trả)
        body.put("note", "Đơn hàng từ hệ thống TMĐT");
        body.put("required_note", "CHOXEMHANGKHONGTHU"); // Cho khách xem hàng nhưng không cho thử
        body.put("to_name", request.getCustomerName());
        body.put("to_phone", request.getCustomerPhone());
        body.put("to_address", request.getAddress());
        body.put("to_ward_code", request.getToWardCode());
        body.put("to_district_id", request.getToDistrictId());
        body.put("weight", request.getWeightInGrams());
        body.put("cod_amount", request.getCodAmount()); // Tiền thu hộ

        // GHN bắt buộc truyền danh sách hàng hóa (Items)
        Map<String, Object> item = new HashMap<>();
        item.put("name", "Đơn hàng TMĐT tổng hợp"); // Có thể bóc tách từ OrderItem thực tế nếu muốn chi tiết
        item.put("quantity", 1);
        item.put("weight", request.getWeightInGrams());
        body.put("items", List.of(item));

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            log.info("Đang gửi yêu cầu tạo đơn sang GHN...");
            // 3. Thực hiện gọi API POST
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            Map<String, Object> responseBody = response.getBody();

            // 4. Xử lý kết quả trả về
            Integer code = responseBody != null ? toInt(responseBody.get("code")) : null;
            if (responseBody != null && code != null && code == 200) {
                Map<String, Object> data = safeMap(responseBody.get("data"));
                String trackingCode = data != null ? toStr(data.get("order_code")) : null;
                String expectedDeliveryTime = data != null ? toStr(data.get("expected_delivery_time")) : null;
                Double totalFee = data != null ? toDouble(data.get("total_fee")) : null;

                log.info("✅ Tạo đơn GHN thành công! Tracking Code: {}", trackingCode);

                return ShippingOrderResponse.builder()
                        .trackingCode(trackingCode)
                        .shippingFee(totalFee != null ? totalFee : 0.0)
                        .expectedDeliveryTime(expectedDeliveryTime)
                        .build();
            } else {
                log.error("❌ Lỗi từ GHN API: {}", responseBody);
                String msg = responseBody != null ? toStr(responseBody.get("message")) : null;
                throw new RuntimeException("GHN từ chối tạo đơn: " + (msg != null ? msg : "Unknown error"));
            }

        } catch (Exception e) {
            log.error("❌ Lỗi mạng/Hệ thống khi kết nối GHN: ", e);
            throw new RuntimeException("Không thể kết nối với đơn vị giao hàng GHN", e);
        }
    }

    @Override
    public double calculateShippingFee(String toWardCode, String toDistrictCode, int weightInGrams) {
        String url = apiUrl + "/v2/shipping-order/fee";

        if (fromDistrictId == null || fromDistrictId <= 0) {
            // Fallback to static fee if shop location is not configured.
            return 35000.0;
        }

        Integer toDistrictId = toInt(toDistrictCode);
        if (toDistrictId == null || toDistrictId <= 0) {
            return 35000.0;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Token", apiToken);
        headers.set("ShopId", shopId);

        Map<String, Object> body = new HashMap<>();
        body.put("from_district_id", fromDistrictId);
        if (fromWardCode != null && !fromWardCode.isBlank()) {
            body.put("from_ward_code", fromWardCode);
        }
        body.put("to_district_id", toDistrictId);
        body.put("to_ward_code", toWardCode);
        body.put("weight", Math.max(1, weightInGrams));
        body.put("service_type_id", serviceTypeId);

        try {
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            Map<String, Object> respBody = response.getBody();
            Integer code = respBody != null ? toInt(respBody.get("code")) : null;
            if (respBody != null && code != null && code == 200) {
                Map<String, Object> data = safeMap(respBody.get("data"));
                Double total = data != null ? toDouble(data.get("total")) : null;
                if (total != null) return total;
            }
            return 35000.0;
        } catch (Exception e) {
            log.warn("GHN fee API failed, fallback to static fee: {}", e.getMessage());
            return 35000.0;
        }
    }

    private Integer toInt(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(String.valueOf(v));
        } catch (Exception e) {
            return null;
        }
    }

    private Double toDouble(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.doubleValue();
        try {
            return Double.parseDouble(String.valueOf(v));
        } catch (Exception e) {
            return null;
        }
    }

    private String toStr(Object v) {
        if (v == null) return null;
        String s = String.valueOf(v);
        return s.isBlank() ? null : s;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> safeMap(Object v) {
        if (v instanceof Map<?, ?> m) {
            return (Map<String, Object>) m;
        }
        return null;
    }
}