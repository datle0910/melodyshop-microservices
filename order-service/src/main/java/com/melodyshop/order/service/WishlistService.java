package com.melodyshop.order.service;

import com.melodyshop.common.exception.BadRequestException;
import com.melodyshop.order.dto.WishlistDTO;
import com.melodyshop.order.entity.Wishlist;
import com.melodyshop.order.repository.WishlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WishlistService {

    private final WishlistRepository wishlistRepository;

    public List<WishlistDTO> getWishlist(String userId) {
        return wishlistRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional
    public WishlistDTO addToWishlist(String userId, String productId) {
        if (wishlistRepository.existsByUserIdAndProductId(userId, productId)) {
            throw new BadRequestException("Sản phẩm đã có trong danh sách yêu thích");
        }
        Wishlist wishlist = Wishlist.builder().userId(userId).productId(productId).build();
        return toDTO(wishlistRepository.save(wishlist));
    }

    @Transactional
    public void removeFromWishlist(String userId, String productId) {
        wishlistRepository.deleteByUserIdAndProductId(userId, productId);
    }

    private WishlistDTO toDTO(Wishlist w) {
        return WishlistDTO.builder().id(w.getId()).productId(w.getProductId()).createdAt(w.getCreatedAt()).build();
    }
}
