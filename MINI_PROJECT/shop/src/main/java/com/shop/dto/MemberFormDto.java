package com.shop.dto;

import lombok.*;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class MemberFormDto {
    private String name;
    private String email;
    private String password;
    private String address;
}
