package com.shop.interfaces.order;

import com.shop.application.order.CartService;
import com.shop.application.order.dto.CartDetailDto;
import com.shop.application.order.dto.CartItemDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.validation.Valid;
import java.security.Principal;
import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @PostMapping("/cart")
    public @ResponseBody
    ResponseEntity order(@RequestBody @Valid CartItemDto cartItemDto, BindingResult bindingResult, Principal principal) {
        if (bindingResult.hasErrors()) {
            final StringBuilder sb = new StringBuilder();
            final List<FieldError> fieldErrors = bindingResult.getFieldErrors();
            fieldErrors.stream().map(DefaultMessageSourceResolvable::getDefaultMessage).forEach(sb::append);
            return new ResponseEntity(sb.toString(), HttpStatus.BAD_REQUEST);
        }

        final String email = principal.getName();
        Long cartItemId;
        try {
            cartItemId = cartService.addCart(cartItemDto, email);
        } catch (Exception e) {
            return new ResponseEntity(e.getMessage(), HttpStatus.BAD_REQUEST);
        }

        return new ResponseEntity(cartItemId, HttpStatus.OK);
    }

    @GetMapping("/cart")
    public String orderHistory(Principal principal, Model model) {
        final List<CartDetailDto> cartList = cartService.getCartList(principal.getName());
        model.addAttribute("cartItems", cartList);
        return "/cart/cartList";
    }

    @PatchMapping("/cartItem/{cartItemId}")
    public @ResponseBody
    ResponseEntity updateCartItem(@PathVariable Long cartItemId, int count, Principal principal) {
        if (count <= 0) {
            return new ResponseEntity("최소 1개 이상 담아주세요", HttpStatus.BAD_REQUEST);
        }
        if (!cartService.validateCartItem(cartItemId, principal.getName())) {
            new ResponseEntity("수정 권한이 없습니다.", HttpStatus.FORBIDDEN);
        }
        cartService.updateCartItemCount(cartItemId, count);
        return new ResponseEntity(cartItemId, HttpStatus.OK);
    }


}
