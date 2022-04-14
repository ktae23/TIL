package com.shop.application.order;

import com.shop.application.order.dto.CartDetailDto;
import com.shop.application.order.dto.CartItemDto;
import com.shop.domain.member.model.Member;
import com.shop.domain.member.repository.MemberRepository;
import com.shop.domain.order.model.Cart;
import com.shop.domain.order.model.CartItem;
import com.shop.domain.order.model.Item;
import com.shop.domain.order.repository.CartItemRepository;
import com.shop.domain.order.repository.CartRepository;
import com.shop.domain.order.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class CartService {

    private final ItemRepository itemRepository;
    private final MemberRepository memberRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;

    public Long addCart(CartItemDto cartItemDto, String email) {
        final Item item = itemRepository.findById(cartItemDto.getItemId()).orElseThrow(EntityNotFoundException::new);
        final Member member = memberRepository.findByEmail(email).orElseThrow(EntityNotFoundException::new);
        final Cart cart = cartRepository.findByMemberId(member.getId()).orElseGet(() -> cartRepository.save(Cart.createCart(member)));

        final Optional<CartItem> savedCartItem = cartItemRepository.findByCartIdAndItemId(cart.getId(), item.getId());
        if (savedCartItem.isEmpty()) {
            final CartItem cartItem = CartItem.createCartItem(cart, item, cartItemDto.getCount());
            cartItemRepository.save(cartItem);
            return cartItem.getId();
        }
        savedCartItem.get().addCount(cartItemDto.getCount());
        return savedCartItem.get().getId();

    }

    @Transactional(readOnly = true)
    public List<CartDetailDto> getCartList(String email) {
        final Member member = memberRepository.findByEmail(email).orElseThrow(EntityNotFoundException::new);
        final Optional<Cart> cart = cartRepository.findByMemberId(member.getId());
        if (cart.isEmpty()) {
            return new ArrayList<>();
        }
        return cartItemRepository.findCartDetailDtoList(cart.get().getId());
    }

    @Transactional(readOnly = true)
    public boolean validateCartItem(Long cartItemId, String email) {
        final Member currentMember = memberRepository.findByEmail(email).orElseThrow(EntityNotFoundException::new);
        final CartItem cartItem = cartItemRepository.findById(cartItemId).orElseThrow(EntityNotFoundException::new);
        final Member savedMember = cartItem.getCart().getMember();

        return currentMember.getEmail().equals(savedMember.getEmail());
    }

    public void updateCartItemCount(Long cartItemId, int count) {
        final CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(EntityNotFoundException::new);
        cartItem.updateCount(count);
    }

}