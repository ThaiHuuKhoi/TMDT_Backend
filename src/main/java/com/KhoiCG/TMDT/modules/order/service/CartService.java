package com.KhoiCG.TMDT.modules.order.service;

import com.KhoiCG.TMDT.modules.order.entity.Cart;
import com.KhoiCG.TMDT.modules.order.entity.CartItem;
import com.KhoiCG.TMDT.modules.order.repository.CartItemRepository;
import com.KhoiCG.TMDT.modules.order.repository.CartRepository;
import com.KhoiCG.TMDT.modules.product.entity.ProductVariant;
import com.KhoiCG.TMDT.modules.product.repository.ProductVariantRepository;
import com.KhoiCG.TMDT.modules.user.entity.User;
import com.KhoiCG.TMDT.modules.user.repository.UserRepo;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductVariantRepository variantRepository;
    private final UserRepo userRepo;

    @Transactional
    public Cart getOrCreateCart(Long userId) {
        return cartRepository.findByUserId(userId).orElseGet(() -> {
            User user = userRepo.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy User"));
            Cart newCart = Cart.builder().user(user).build();
            return cartRepository.save(newCart);
        });
    }

    @Transactional
    public Cart addToCart(Long userId, Long variantId, Integer quantity) {
        Cart cart = getOrCreateCart(userId);

        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại"));

        if (variant.getStockQuantity() < quantity) {
            throw new RuntimeException("Số lượng tồn kho không đủ. Chỉ còn " + variant.getStockQuantity() + " sản phẩm.");
        }

        Optional<CartItem> existingItem = cartItemRepository.findByCartIdAndVariantId(cart.getId(), variantId);

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            int newQuantity = item.getQuantity() + quantity;
            if (newQuantity > variant.getStockQuantity()) {
                throw new RuntimeException("Vượt quá số lượng tồn kho!");
            }
            item.setQuantity(newQuantity);
        } else {
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .variant(variant)
                    .quantity(quantity)
                    .build();
            cart.getItems().add(newItem);
        }

        return cartRepository.save(cart);
    }

    @Transactional
    public Cart updateItemQuantity(Long userId, Long variantId, Integer newQuantity) {
        Cart cart = getOrCreateCart(userId);
        CartItem item = cartItemRepository.findByCartIdAndVariantId(cart.getId(), variantId)
                .orElseThrow(() -> new RuntimeException("Sản phẩm không có trong giỏ hàng"));

        if (newQuantity <= 0) {
            cart.getItems().remove(item);
        } else {
            if (newQuantity > item.getVariant().getStockQuantity()) {
                throw new RuntimeException("Vượt quá số lượng tồn kho!");
            }
            item.setQuantity(newQuantity);
        }

        return cartRepository.save(cart);
    }

    @Transactional
    public Cart removeItem(Long userId, Long variantId) {
        Cart cart = getOrCreateCart(userId);
        cart.getItems().removeIf(item -> item.getVariant().getId().equals(variantId));
        return cartRepository.save(cart);
    }

    @Transactional
    public void clearCart(Long userId) {
        Cart cart = getOrCreateCart(userId);
        cart.getItems().clear();
        cartRepository.save(cart);
    }
}