package com.hmdp.dto;

import lombok.Data;

import java.util.List;

/**
 * @author xbhog
 * @since 2023年2月27日
 */
@Data
public class ScrollResult {
    private List<?> list;
    private Long minTime;
    private Integer offset;
}
