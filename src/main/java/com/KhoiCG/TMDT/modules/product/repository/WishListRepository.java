package com.KhoiCG.TMDT.modules.product.repository;

import com.KhoiCG.TMDT.modules.product.entity.WishList;
import com.KhoiCG.TMDT.modules.product.entity.WishListId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WishListRepository extends JpaRepository<WishList, WishListId> {
    Page<WishList> findByUserId(Long userId, Pageable pageable);
}