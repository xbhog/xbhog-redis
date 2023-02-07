package com.hmdp.utils;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * @author xbhog
 * @describe:非空判断
 * @date 2023/2/7
 */
@Component
public class GetNullChangeUtil {

    public String getBullChange(String arg){
        return StringUtils.isEmpty(arg) ? "-1" : arg;
    }
}
