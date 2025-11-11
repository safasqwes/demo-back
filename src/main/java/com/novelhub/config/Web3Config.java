package com.novelhub.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Web3 配置类
 * 支持多个区块链网络
 */
@Configuration
@Getter
public class Web3Config {

    // 收款地址配置
    @Value("${web3.recipient.sepolia:}")
    private String sepoliaRecipientAddress;

    @Value("${web3.recipient.polygon-amoy:}")
    private String polygonAmoyRecipientAddress;

    @Value("${web3.recipient.polygon:}")
    private String polygonRecipientAddress;

    @Value("${web3.recipient.ethereum:}")
    private String ethereumRecipientAddress;

    @Value("${web3.recipient.bsc:}")
    private String bscRecipientAddress;

    // 支持的链 ID
    public static final int CHAIN_ID_ETHEREUM = 1;        // Ethereum 主网
    public static final int CHAIN_ID_BSC = 56;            // BSC 主网
    public static final int CHAIN_ID_POLYGON = 137;       // Polygon 主网
    public static final int CHAIN_ID_SEPOLIA = 11155111;  // Sepolia 测试网
    public static final int CHAIN_ID_POLYGON_AMOY = 80002; // Polygon Amoy 测试网

    // 网络配置映射
    private static final Map<Integer, NetworkConfig> NETWORK_CONFIGS = new HashMap<>();

    static {
        // Ethereum 主网
        NETWORK_CONFIGS.put(CHAIN_ID_ETHEREUM, NetworkConfig.builder()
            .chainId(CHAIN_ID_ETHEREUM)
            .name("Ethereum Mainnet")
            .symbol("ETH")
            .rpcUrl("https://eth-mainnet.g.alchemy.com/v2/your-api-key")
            .explorerUrl("https://etherscan.io")
            .isTestnet(false)
            .build());

        // BSC 主网
        NETWORK_CONFIGS.put(CHAIN_ID_BSC, NetworkConfig.builder()
            .chainId(CHAIN_ID_BSC)
            .name("BNB Smart Chain")
            .symbol("BNB")
            .rpcUrl("https://bsc-dataseed.binance.org")
            .explorerUrl("https://bscscan.com")
            .isTestnet(false)
            .build());

        // Polygon 主网
        NETWORK_CONFIGS.put(CHAIN_ID_POLYGON, NetworkConfig.builder()
            .chainId(CHAIN_ID_POLYGON)
            .name("Polygon Mainnet")
            .symbol("MATIC")
            .rpcUrl("https://polygon-rpc.com")
            .explorerUrl("https://polygonscan.com")
            .isTestnet(false)
            .build());

        // Sepolia 测试网
        NETWORK_CONFIGS.put(CHAIN_ID_SEPOLIA, NetworkConfig.builder()
            .chainId(CHAIN_ID_SEPOLIA)
            .name("Sepolia Testnet")
            .symbol("ETH")
            .rpcUrl("https://sepolia.infura.io/v3/your-api-key")
            .explorerUrl("https://sepolia.etherscan.io")
            .isTestnet(true)
            .build());

        // Polygon Amoy 测试网
        NETWORK_CONFIGS.put(CHAIN_ID_POLYGON_AMOY, NetworkConfig.builder()
            .chainId(CHAIN_ID_POLYGON_AMOY)
            .name("Polygon Amoy Testnet")
            .symbol("MATIC")
            .rpcUrl("https://rpc-amoy.polygon.technology")
            .explorerUrl("https://amoy.polygonscan.com")
            .isTestnet(true)
            .build());
    }

    /**
     * 根据 Chain ID 获取收款地址
     *
     * @param chainId 链 ID
     * @return 收款地址
     */
    public String getRecipientAddress(int chainId) {
        return switch (chainId) {
            case CHAIN_ID_ETHEREUM -> {
                if (ethereumRecipientAddress == null || ethereumRecipientAddress.isEmpty()) {
                    throw new IllegalArgumentException("Ethereum recipient address not configured");
                }
                yield ethereumRecipientAddress;
            }
            case CHAIN_ID_BSC -> {
                if (bscRecipientAddress == null || bscRecipientAddress.isEmpty()) {
                    throw new IllegalArgumentException("BSC recipient address not configured");
                }
                yield bscRecipientAddress;
            }
            case CHAIN_ID_POLYGON -> {
                if (polygonRecipientAddress == null || polygonRecipientAddress.isEmpty()) {
                    throw new IllegalArgumentException("Polygon recipient address not configured");
                }
                yield polygonRecipientAddress;
            }
            case CHAIN_ID_SEPOLIA -> {
                if (sepoliaRecipientAddress == null || sepoliaRecipientAddress.isEmpty()) {
                    throw new IllegalArgumentException("Sepolia recipient address not configured");
                }
                yield sepoliaRecipientAddress;
            }
            case CHAIN_ID_POLYGON_AMOY -> {
                if (polygonAmoyRecipientAddress == null || polygonAmoyRecipientAddress.isEmpty()) {
                    throw new IllegalArgumentException("Polygon Amoy recipient address not configured");
                }
                yield polygonAmoyRecipientAddress;
            }
            default -> throw new IllegalArgumentException(
                String.format("Unsupported chain ID: %d. Supported chains: Ethereum(1), BSC(56), Polygon(137), Sepolia(11155111), Polygon Amoy(80002)", chainId)
            );
        };
    }

    /**
     * 获取网络配置
     *
     * @param chainId 链 ID
     * @return 网络配置
     */
    public NetworkConfig getNetworkConfig(int chainId) {
        NetworkConfig config = NETWORK_CONFIGS.get(chainId);
        if (config == null) {
            throw new IllegalArgumentException("Unsupported chain ID: " + chainId);
        }
        return config;
    }

    /**
     * 检查链 ID 是否支持
     *
     * @param chainId 链 ID
     * @return 是否支持
     */
    public boolean isSupportedChain(int chainId) {
        return NETWORK_CONFIGS.containsKey(chainId);
    }

    /**
     * 检查是否为测试网
     *
     * @param chainId 链 ID
     * @return 是否为测试网
     */
    public boolean isTestnet(int chainId) {
        NetworkConfig config = NETWORK_CONFIGS.get(chainId);
        return config != null && config.isTestnet();
    }

    /**
     * 网络配置类
     */
    @lombok.Builder
    @lombok.Data
    public static class NetworkConfig {
        private int chainId;
        private String name;
        private String symbol;
        private String rpcUrl;
        private String explorerUrl;
        private boolean isTestnet;
    }
}

