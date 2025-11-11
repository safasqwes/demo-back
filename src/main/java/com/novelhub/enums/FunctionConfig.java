package com.novelhub.enums;

/**
 * Function Configuration Enum
 * Defines function limits and pricing (not stored in database, code-based configuration)
 * 
 * Each function has:
 * - functionType: Unique integer ID (used in database for tb_function_usage_log.function_type)
 * - functionName: Unique function name
 * - scene: Reserved field for future use
 * - model: AI model used (e.g., "gpt-4", "dall-e-3", etc.)
 * - Pricing rules: Trial (guest), Free points (silver coins), Fixed points (gold coins)
 * 
 * Points System:
 * - Free Points (Silver Coins): Earned through daily login, tasks, etc.
 * - Fixed Points (Gold Coins): Purchased with real money
 * 
 * Pricing Strategy:
 * - If only freePointsCost is set (fixedPointsCost = 0): Only consumes silver coins
 * - If only fixedPointsCost is set (freePointsCost = 0): Only consumes gold coins
 * - If both are set: Prioritize silver coins, then gold coins
 */
public enum FunctionConfig {
    // Demo test function - costs 10 silver coins OR 2 gold coins
    DEMO_TEST(1001, "demo-test", SceneType.CHAT, ModelType.GPT_3_5_TURBO, 5, 10, 2),
    
    // AI Chat - costs 50 silver coins OR 5 gold coins
    AI_CHAT(1002, "ai-chat", SceneType.CHAT, ModelType.GPT_4, 10, 50, 5),
    
    // Image Generation - costs 100 silver coins OR 10 gold coins
    IMAGE_GENERATION(1003, "image-gen", SceneType.IMAGE, ModelType.DALL_E_3, 3, 100, 10),
    
    // Nano Banana - Image Generation & Editing - costs 80 silver coins OR 8 gold coins
    NANO_BANANA(1005, "nano-banana", SceneType.IMAGE_EDIT, ModelType.NANO_BANANA, 50, 80, 8),
    
    // Premium function - only gold coins (no silver coin option)
    PREMIUM_ANALYSIS(1004, "premium-analysis", SceneType.ANALYSIS, ModelType.GPT_4_TURBO, 0, 0, 50);

    private final int functionType;             // Unique integer ID for database storage
    private final String functionName;          // Unique function name (semantic and self-explanatory)
    private final SceneType sceneType;          // Scene/category type
    private final ModelType modelType;          // AI model type
    private final int guestDailyLimit;          // Daily limit for guest users (fingerprint-based)
    private final int freePointsCost;           // Cost in free points (silver coins) for authenticated users
    private final int fixedPointsCost;          // Cost in fixed points (gold coins) for authenticated users

    FunctionConfig(int functionType, String functionName, SceneType sceneType, ModelType modelType, 
                   int guestDailyLimit, int freePointsCost, int fixedPointsCost) {
        this.functionType = functionType;
        this.functionName = functionName;
        this.sceneType = sceneType;
        this.modelType = modelType;
        this.guestDailyLimit = guestDailyLimit;
        this.freePointsCost = freePointsCost;
        this.fixedPointsCost = fixedPointsCost;
    }

    public int getFunctionType() {
        return functionType;
    }

    public String getFunctionName() {
        return functionName;
    }

    public SceneType getSceneType() {
        return sceneType;
    }

    public ModelType getModelType() {
        return modelType;
    }

    /**
     * Get scene code for display/storage
     * @return Scene code (e.g., "chat", "image", "analysis")
     */
    public String getScene() {
        return sceneType.getCode();
    }

    /**
     * Get model code for display/storage
     * @return Model code (e.g., "gpt-4", "dall-e-3")
     */
    public String getModel() {
        return modelType.getCode();
    }

    public int getGuestDailyLimit() {
        return guestDailyLimit;
    }

    public int getFreePointsCost() {
        return freePointsCost;
    }

    public int getFixedPointsCost() {
        return fixedPointsCost;
    }

    /**
     * Get total cost in mixed points (for display purposes)
     * Returns free points cost if available, otherwise fixed points cost
     */
    public String getCostDisplay() {
        if (freePointsCost > 0 && fixedPointsCost > 0) {
            return freePointsCost + " silver coins OR " + fixedPointsCost + " gold coins";
        } else if (freePointsCost > 0) {
            return freePointsCost + " silver coins";
        } else if (fixedPointsCost > 0) {
            return fixedPointsCost + " gold coins";
        } else {
            return "Free";
        }
    }

    /**
     * Check if user has enough points to use this function
     * @param freePoints User's free points (silver coins)
     * @param fixedPoints User's fixed points (gold coins)
     * @return true if user has enough points
     */
    public boolean hasEnoughPoints(int freePoints, int fixedPoints) {
        // Can use free points
        if (freePointsCost > 0 && freePoints >= freePointsCost) {
            return true;
        }
        // Can use fixed points
        if (fixedPointsCost > 0 && fixedPoints >= fixedPointsCost) {
            return true;
        }
        return false;
    }

    /**
     * Determine which type of points to deduct
     * Priority: Free points (silver) > Fixed points (gold)
     * @param freePoints User's free points
     * @param fixedPoints User's fixed points
     * @return PointsType code (1 for free, 2 for fixed) or null if not enough points
     */
    public Integer determinePointsType(int freePoints, int fixedPoints) {
        // Try free points first
        if (freePointsCost > 0 && freePoints >= freePointsCost) {
            return PointsType.FREE.getCode();  // 1
        }
        // Fall back to fixed points
        if (fixedPointsCost > 0 && fixedPoints >= fixedPointsCost) {
            return PointsType.FIXED.getCode();  // 2
        }
        return null;
    }
    
    /**
     * Get points type for guest users
     * @return PointsType.TRIAL code (0)
     */
    public int getGuestPointsType() {
        return PointsType.TRIAL.getCode();  // 0
    }

    /**
     * Find function config by function type (integer ID)
     * @param functionType Function type ID (e.g., 1001, 1002, etc.)
     * @return FunctionConfig or null if not found
     */
    public static FunctionConfig findByType(int functionType) {
        for (FunctionConfig config : values()) {
            if (config.functionType == functionType) {
                return config;
            }
        }
        return null;
    }

    /**
     * Find function config by function name
     * @param functionName Function name (e.g., "demo-test", "ai-chat")
     * @return FunctionConfig or null if not found
     */
    public static FunctionConfig findByName(String functionName) {
        if (functionName == null) {
            return null;
        }
        for (FunctionConfig config : values()) {
            if (config.functionName.equals(functionName)) {
                return config;
            }
        }
        return null;
    }
}

