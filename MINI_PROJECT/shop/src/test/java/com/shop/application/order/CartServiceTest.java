package com.shop.application.order;

import com.shop.application.order.dto.CartItemDto;
import com.shop.domain.member.model.Member;
import com.shop.domain.member.repository.MemberRepository;
import com.shop.domain.order.model.CartItem;
import com.shop.domain.order.model.Item;
import com.shop.domain.order.repository.CartItemRepository;
import com.shop.domain.order.repository.ItemRepository;
import com.shop.infrastructure.constant.order.ItemSellStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(args = "--spring.profile.active=test")
@Transactional
class CartServiceTest {

    @Autowired ItemRepository itemRepository;
    @Autowired MemberRepository memberRepository;
    @Autowired CartService cartService;
    @Autowired CartItemRepository cartItemRepository;


    public Item saveItem() {
        final Item item = Item.builder()
                .itemName("테스트 상품")
                .price(10000)
                .itemDetail("테스트 상품 상세 설명")
                .itemSellStatus(ItemSellStatus.SELL)
                .stockNumber(100)
                .build();
        return itemRepository.save(item);
    }

    public Member saveMember(){
        final Member member = Member.builder()
                .email("test@test.com")
                .build();
        return memberRepository.save(member);
    }

    @Test
    @DisplayName("장바구니 담기 테스트")
    void addCart () throws Exception {
        // given
        final Item item = saveItem();
        final Member member = saveMember();
        final CartItemDto cartItemDto = new CartItemDto(item.getId(), 5);

        // when
        final Long cartItemId = cartService.addCart(cartItemDto, member.getEmail());
        final CartItem cartItem = cartItemRepository.findById(cartItemId).orElseThrow(EntityNotFoundException::new);
        // then
        assertThat(item.getId()).isEqualTo(cartItem.getItem().getId());
        assertThat(cartItemDto.getCount()).isEqualTo(cartItem.getCount());
    }
}