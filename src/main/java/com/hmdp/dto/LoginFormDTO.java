package com.hmdp.dto;

import lombok.Data;

/**
 * @author xbhog
 * @since 2023年2月27日
 */
@Data
public class LoginFormDTO {
    private String phone;
    private String code;
    private String password;
}
