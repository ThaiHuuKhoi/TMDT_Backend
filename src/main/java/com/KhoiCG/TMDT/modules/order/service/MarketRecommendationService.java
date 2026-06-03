package com.KhoiCG.TMDT.modules.order.service;

import com.KhoiCG.TMDT.modules.order.dto.MarketRecommendationItemResponse;
import com.KhoiCG.TMDT.modules.order.dto.MarketRecommendationResponse;
import com.KhoiCG.TMDT.modules.order.entity.OrderStatus;
import com.KhoiCG.TMDT.modules.order.repository.OrderRepository;
import com.KhoiCG.TMDT.modules.product.entity.Product;
import com.KhoiCG.TMDT.modules.product.entity.ProductStatus;
import com.KhoiCG.TMDT.modules.product.entity.ProductVariant;
import com.KhoiCG.TMDT.modules.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MarketRecommendationService {
    private static final int DEFAULT_LOOKBACK_DAYS = 30;
    private static final List<OrderStatus> SALES_STATUSES = List.of(
            OrderStatus.PAID, OrderStatus.SHIPPED, OrderStatus.COMPLETED
    );

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    public MarketRecommendationResponse getRecommendations(int limit, int horizonDays) {
        int safeLimit = Math.max(1, Math.min(limit, 100));
        int safeHorizon = Math.max(7, Math.min(horizonDays, 60));
        int lookbackDays = DEFAULT_LOOKBACK_DAYS;

        LocalDateTime startDate = LocalDateTime.now().minusDays(lookbackDays);
        List<Object[]> salesRows = orderRepository.findProductSalesSince(SALES_STATUSES, startDate);

        Map<Long, ProductSalesSnapshot> salesByProduct = new HashMap<>();
        double maxUnits = 0;

        for (Object[] row : salesRows) {
            Long productId = ((Number) row[0]).longValue();
            long units = ((Number) row[4]).longValue();
            BigDecimal revenue = toBigDecimal(row[5]);
            salesByProduct.put(productId, new ProductSalesSnapshot(units, revenue));
            if (units > maxUnits) {
                maxUnits = units;
            }
        }

        List<Product> activeProducts = productRepository.findByStatus(ProductStatus.ACTIVE);
        List<MarketRecommendationItemResponse> ranked = new ArrayList<>();

        for (Product p : activeProducts) {
            ProductSalesSnapshot sales = salesByProduct.getOrDefault(p.getId(), ProductSalesSnapshot.EMPTY);
            int stock = p.getVariants().stream()
                    .map(ProductVariant::getStockQuantity)
                    .filter(v -> v != null && v > 0)
                    .reduce(0, Integer::sum);

            double dailySales = (double) sales.unitsSold / lookbackDays;
            int forecastQty = (int) Math.ceil(dailySales * safeHorizon);
            int safetyStock = Math.max(2, (int) Math.ceil(forecastQty * 0.2));
            int recommendedQty = Math.max(0, forecastQty + safetyStock - stock);

            double daysOfCover = dailySales <= 0 ? 999 : stock / dailySales;
            int marketDemand = marketDemandIndexForCategory(p.getCategory() != null ? p.getCategory().getSlug() : null);
            int internalMomentum = maxUnits <= 0 ? 0 : clampTo100((int) Math.round((sales.unitsSold / maxUnits) * 100));
            int stockRisk = stockRiskIndex(daysOfCover, stock);
            int opportunityScore = clampTo100((int) Math.round(0.4 * marketDemand + 0.4 * internalMomentum + 0.2 * stockRisk));

            if (recommendedQty <= 0 && opportunityScore < 60) {
                continue;
            }

            ranked.add(MarketRecommendationItemResponse.builder()
                    .productId(p.getId())
                    .productName(p.getName())
                    .categorySlug(p.getCategory() != null ? p.getCategory().getSlug() : "unknown")
                    .categoryName(p.getCategory() != null ? p.getCategory().getName() : "Chưa phân loại")
                    .unitsSold30d(sales.unitsSold)
                    .availableStock(stock)
                    .daysOfCover(round(daysOfCover))
                    .marketDemandIndex(marketDemand)
                    .internalSalesMomentum(internalMomentum)
                    .stockRiskIndex(stockRisk)
                    .opportunityScore(opportunityScore)
                    .recommendedQty(recommendedQty)
                    .priority(priority(opportunityScore))
                    .reason(buildReason(marketDemand, internalMomentum, stockRisk, daysOfCover))
                    .build());
        }

        ranked.sort(Comparator
                .comparingInt(MarketRecommendationItemResponse::getOpportunityScore).reversed()
                .thenComparingInt(MarketRecommendationItemResponse::getRecommendedQty).reversed()
                .thenComparingLong(MarketRecommendationItemResponse::getUnitsSold30d).reversed());

        List<MarketRecommendationItemResponse> items = ranked.stream().limit(safeLimit).toList();

        return MarketRecommendationResponse.builder()
                .generatedAt(LocalDateTime.now().toString())
                .lookbackDays(lookbackDays)
                .horizonDays(safeHorizon)
                .totalCandidates(ranked.size())
                .formula("OpportunityScore = 0.4*MarketDemand + 0.4*InternalSalesMomentum + 0.2*StockRisk")
                .items(items)
                .build();
    }

    private int marketDemandIndexForCategory(String categorySlug) {
        if (categorySlug == null) {
            return 50;
        }
        String slug = categorySlug.toLowerCase(Locale.ROOT);
        if (slug.contains("health") || slug.contains("suc-khoe") || slug.contains("medicine")) {
            return 85;
        }
        if (slug.contains("beauty") || slug.contains("lam-dep") || slug.contains("cosmetic")) {
            return 76;
        }
        if (slug.contains("fashion") || slug.contains("thoi-trang")) {
            return 70;
        }
        if (slug.contains("phone") || slug.contains("electronics") || slug.contains("dien-tu")) {
            return 68;
        }
        if (slug.contains("home") || slug.contains("gia-dung")) {
            return 64;
        }
        if (slug.contains("book") || slug.contains("office") || slug.contains("sach")) {
            return 58;
        }
        return 60;
    }

    private int stockRiskIndex(double daysOfCover, int stock) {
        if (stock <= 0) return 100;
        if (daysOfCover <= 7) return 95;
        if (daysOfCover <= 14) return 80;
        if (daysOfCover <= 30) return 50;
        return 20;
    }

    private String priority(int score) {
        if (score >= 75) return "Cao";
        if (score >= 60) return "Vừa";
        return "Thấp";
    }

    private String buildReason(int marketDemand, int internalMomentum, int stockRisk, double daysOfCover) {
        List<String> parts = new ArrayList<>();
        if (marketDemand >= 75) {
            parts.add("Nhu cầu thị trường cao");
        }
        if (internalMomentum >= 70) {
            parts.add("Bán tốt trong 30 ngày");
        }
        if (stockRisk >= 80) {
            parts.add("Nguy cơ thiếu hàng sớm");
        }
        if (parts.isEmpty()) {
            parts.add("Theo dõi và nhập bổ sung theo nhu cầu");
        }
        if (daysOfCover < 999) {
            parts.add("Đủ hàng khoảng " + round(daysOfCover) + " ngày");
        }
        return String.join("; ", parts);
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value instanceof BigDecimal b) {
            return b;
        }
        if (value instanceof Number n) {
            return BigDecimal.valueOf(n.doubleValue());
        }
        return BigDecimal.ZERO;
    }

    private int clampTo100(int value) {
        return Math.max(0, Math.min(100, value));
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    private record ProductSalesSnapshot(long unitsSold, BigDecimal revenue) {
        private static final ProductSalesSnapshot EMPTY = new ProductSalesSnapshot(0, BigDecimal.ZERO);
    }
}
