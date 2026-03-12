package com.KhoiCG.TMDT.modules.chatbot.provider;

import com.KhoiCG.TMDT.modules.chatbot.entity.ChatMessage;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class RuleBasedChatAdvisor implements ChatAdvisor {

    @Override
    public String getCode() {
        return "RULES";
    }

    @Override
    public String reply(String userMessage, List<ChatMessage> recentHistory) {
        String msg = userMessage == null ? "" : userMessage.toLowerCase(Locale.ROOT).trim();

        if (msg.isBlank()) {
            return "Bạn gửi giúp mình câu hỏi cụ thể nhé (ví dụ: phí ship, theo dõi đơn, đổi trả, thanh toán).";
        }

        if (containsAny(msg, "ship", "phí ship", "vận chuyển", "giao hàng")) {
            return """
                    Về vận chuyển, bạn có thể:
                    - Nhập địa chỉ để hệ thống tính phí (tuỳ đối tác GHN/GHTK).
                    - Sau khi đặt hàng, hệ thống sẽ có mã vận đơn để bạn theo dõi.
                    Nếu bạn cho mình biết quận/huyện + phường/xã + khối lượng (gram), mình gợi ý phí dự kiến nhé.
                    """.trim();
        }

        if (containsAny(msg, "theo dõi", "tracking", "vận đơn", "mã vận đơn")) {
            return """
                    Bạn gửi mình mã vận đơn (tracking code) hoặc mã đơn hàng nhé.
                    Nếu bạn đã có tracking code, hệ thống sẽ cập nhật timeline giao hàng khi đối tác (GHN) bắn webhook.
                    """.trim();
        }

        if (containsAny(msg, "đổi trả", "trả hàng", "hoàn tiền", "refund")) {
            return """
                    Về đổi trả/hoàn tiền:
                    - Nếu hàng lỗi/không đúng mô tả: bạn chụp ảnh + mô tả tình trạng.
                    - Mình sẽ hướng dẫn tạo yêu cầu đổi trả theo đơn hàng.
                    Bạn cho mình biết mã đơn và lý do đổi trả nhé.
                    """.trim();
        }

        if (containsAny(msg, "thanh toán", "vnpay", "momo", "stripe", "cod")) {
            return """
                    Về thanh toán, hệ thống hỗ trợ nhiều phương thức (tuỳ cấu hình): COD / VNPay / Stripe.
                    Bạn đang gặp lỗi ở bước nào (tạo session, redirect, hay webhook xác nhận) để mình chỉ đúng chỗ?
                    """.trim();
        }

        if (containsAny(msg, "chào", "hello", "hi", "xin chào")) {
            return "Chào bạn! Bạn muốn mình tư vấn về sản phẩm, đặt hàng, thanh toán hay vận chuyển?";
        }

        return """
                Mình có thể tư vấn:
                - Sản phẩm/danh mục, tồn kho, giá
                - Đặt hàng, mã giảm giá
                - Thanh toán
                - Vận chuyển & theo dõi đơn
                Bạn mô tả ngắn vấn đề hoặc gửi mã đơn/mã vận đơn (nếu có) nhé.
                """.trim();
    }

    private boolean containsAny(String msg, String... needles) {
        for (String n : needles) {
            if (msg.contains(n)) return true;
        }
        return false;
    }
}

