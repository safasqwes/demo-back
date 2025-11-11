package com.novelhub.vo.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建 Binance Pay 订单响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBinancePayOrderResponse {
    /**
     * 是否成功
     */
    private Boolean success;

    /**
     * Binance Pay 预支付 ID
     */
    private String prepayId;

    /**
     * 二维码链接
     */
    private String qrcodeLink;

    /**
     * 二维码内容
     */
    private String qrContent;

    /**
     * 支付页面 URL
     */
    private String checkoutUrl;

    /**
     * 深度链接
     */
    private String deeplink;

    /**
     * 通用链接
     */
    private String universalUrl;

    /**
     * 过期时间（时间戳）
     */
    private Long expireTime;

    /**
     * 错误信息
     */
    private String error;
}

