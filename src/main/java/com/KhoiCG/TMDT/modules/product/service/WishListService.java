package com.KhoiCG.TMDT.modules.product.service;

import com.KhoiCG.TMDT.modules.product.dto.ProductResponse;
import com.KhoiCG.TMDT.modules.product.entity.Product;
import com.KhoiCG.TMDT.modules.product.entity.WishList;
import com.KhoiCG.TMDT.modules.product.entity.WishListId;
import com.KhoiCG.TMDT.modules.product.mapper.ProductMapper;
import com.KhoiCG.TMDT.modules.product.repository.ProductRepository;
import com.KhoiCG.TMDT.modules.product.repository.WishListRepository;
import com.KhoiCG.TMDT.modules.user.entity.User;
import com.KhoiCG.TMDT.modules.user.repository.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WishListService {

    private final WishListRepository wishListRepository;
    private final UserRepo userRepo;
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    @Transactional
    public String toggleWishlist(Long userId, Long productId) {
        WishListId id = new WishListId(userId, productId);

        if (wishListRepository.existsById(id)) {
            wishListRepository.deleteById(id);
            return "Đã xóa khỏi danh sách yêu thích";
        }

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        WishList wishList = WishList.builder()
                .id(id)
                .user(user)
                .product(product)
                .build();

        wishListRepository.save(wishList);
        return "Đã thêm vào danh sách yêu thích";
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> getUserWishlist(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<WishList> wishListPage = wishListRepository.findByUserId(userId, pageable);

        return wishListPage.map(wishList -> productMapper.toProductResponse(wishList.getProduct()));
    }
}