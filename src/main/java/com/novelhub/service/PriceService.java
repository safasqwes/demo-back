package com.novelhub.service;

import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

/**
 * 价格服务 - 从 CoinGecko API 获取实时代币价格
 */
@Slf4j
@Service
public class PriceService {
    
    @Value("${coingecko.api-url}")
    private String coingeckoApiUrl;
    
    @Value("${coingecko.price-cache-ttl:300}")
    private int priceCacheTtl;
    
    private final RestTemplate restTemplate;
    
    // 简单的内存缓存
    private final Map<String, PriceCache> priceCache = new HashMap<>();
    
    public PriceService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    /**
     * 获取代币价格信息
     */
    public PriceInfoDTO getTokenPrice(String currency, double fiatAmount) {
        try {
            // 检查缓存
            String cacheKey = currency + "_" + fiatAmount;
            PriceCache cached = priceCache.get(cacheKey);
            if (cached != null && !cached.isExpired()) {
                return cached.getPriceInfo();
            }
            
            // 从 CoinGecko 获取价格
            String price = getTokenPriceFromCoinGecko(currency);
            if (price == null) {
                throw new RuntimeException("Failed to get token price from CoinGecko");
            }
            BigDecimal tokenPrice = new BigDecimal(price);
            
            BigDecimal tokenAmount = new BigDecimal(fiatAmount).divide(tokenPrice, 18, RoundingMode.HALF_UP);
            
            long currentTime = System.currentTimeMillis() / 1000;
            long ttl = currentTime + priceCacheTtl;
            
            PriceInfoDTO priceInfo = new PriceInfoDTO();
            priceInfo.setCurrency(currency);
            priceInfo.setFiatAmount(fiatAmount);
            priceInfo.setTokenAmount(tokenAmount.toPlainString());
            priceInfo.setPriceTTL(ttl);
            priceInfo.setExchangeRate(tokenPrice.doubleValue());
            
            // 缓存结果
            priceCache.put(cacheKey, new PriceCache(priceInfo, currentTime + priceCacheTtl));
            
            log.info("Got token price: {} {} = {} USD", tokenAmount.toPlainString(), currency, fiatAmount);
            return priceInfo;
            
        } catch (Exception e) {
            log.error("Error getting token price for {}: {}", currency, e.getMessage(), e);
            throw new RuntimeException("Failed to get token price: " + e.getMessage(), e);
        }
    }
    
    /**
     * 从 CoinGecko API 获取代币价格
     */
    private String getTokenPriceFromCoinGecko(String currency) {
        try {
//            String coinId = currency;
            String coinId = getCoinGeckoId(currency);
            String url = coingeckoApiUrl + "/simple/price?ids=" + coinId + "&vs_currencies=usd";
            
            String response = restTemplate.getForObject(url, String.class);
            log.info("=====restTemplate response: {}", response);
            JSONObject from = JSONObject.from(response);

            return from.getJSONObject(coinId).getString("usd");
        } catch (Exception e) {
            log.error("Error fetching price from CoinGecko for {}: {}", currency, e.getMessage());
            return null;
        }
    }
    
    /**
     * 获取 CoinGecko 代币 ID
     */
    private String getCoinGeckoId(String currency) {
        switch (currency.toUpperCase()) {
            case "MATIC":
                return "matic-network";
            case "USDT":
                return "tether";
            case "USDC":
                return "usd-coin";
            case "DAI":
                return "dai";
            default:
                throw new IllegalArgumentException("Unsupported currency: " + currency);
        }
    }
    
    /**
     * 价格缓存类
     */
    private static class PriceCache {
        private final PriceInfoDTO priceInfo;
        private final long expiryTime;
        
        public PriceCache(PriceInfoDTO priceInfo, long expiryTime) {
            this.priceInfo = priceInfo;
            this.expiryTime = expiryTime;
        }
        
        public PriceInfoDTO getPriceInfo() {
            return priceInfo;
        }
        
        public boolean isExpired() {
            return System.currentTimeMillis() / 1000 > expiryTime;
        }
    }
    
    /**
     * 价格信息 DTO
     */
    public static class PriceInfoDTO {
        private String currency;
        private double fiatAmount;
        private String tokenAmount;
        private long priceTTL;
        private double exchangeRate;
        
        // Getters and Setters
        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }
        
        public double getFiatAmount() { return fiatAmount; }
        public void setFiatAmount(double fiatAmount) { this.fiatAmount = fiatAmount; }
        
        public String getTokenAmount() { return tokenAmount; }
        public void setTokenAmount(String tokenAmount) { this.tokenAmount = tokenAmount; }
        
        public long getPriceTTL() { return priceTTL; }
        public void setPriceTTL(long priceTTL) { this.priceTTL = priceTTL; }
        
        public double getExchangeRate() { return exchangeRate; }
        public void setExchangeRate(double exchangeRate) { this.exchangeRate = exchangeRate; }
    }
}
