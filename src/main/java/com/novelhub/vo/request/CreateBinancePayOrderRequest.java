package com.novelhub.vo.request;

import lombok.Data;

/**
 * 创建 Binance Pay 订单请求
 */
@Data
public class CreateBinancePayOrderRequest {
    /**
     * 套餐 ID
     */
    private Long planId;

    /**
     * 成功返回 URL
     */
    private String returnUrl;

    /**
     * 取消返回 URL
     */
    private String cancelUrl;
}

