package com.novelhub.enums;

/**
 * AI Model Type Enum
 * Defines various AI models used in business functions
 */
public enum ModelType {
    // OpenAI GPT Models
    GPT_3_5_TURBO("gpt-3.5-turbo", "GPT-3.5 Turbo", "OpenAI", "Fast and cost-effective"),
    GPT_4("gpt-4", "GPT-4", "OpenAI", "Most capable GPT-4 model"),
    GPT_4_TURBO("gpt-4-turbo", "GPT-4 Turbo", "OpenAI", "Latest GPT-4 Turbo with improved capabilities"),
    GPT_4O("gpt-4o", "GPT-4o", "OpenAI", "Optimized GPT-4 model"),
    
    // OpenAI Image Models
    DALL_E_2("dall-e-2", "DALL-E 2", "OpenAI", "Image generation model"),
    DALL_E_3("dall-e-3", "DALL-E 3", "OpenAI", "Advanced image generation"),
    
    // OpenAI Embedding Models
    TEXT_EMBEDDING_ADA_002("text-embedding-ada-002", "Text Embedding Ada 002", "OpenAI", "Text embedding model"),
    TEXT_EMBEDDING_3_SMALL("text-embedding-3-small", "Text Embedding 3 Small", "OpenAI", "Smaller embedding model"),
    TEXT_EMBEDDING_3_LARGE("text-embedding-3-large", "Text Embedding 3 Large", "OpenAI", "Larger embedding model"),
    
    // Anthropic Claude Models
    CLAUDE_3_OPUS("claude-3-opus", "Claude 3 Opus", "Anthropic", "Most capable Claude model"),
    CLAUDE_3_SONNET("claude-3-sonnet", "Claude 3 Sonnet", "Anthropic", "Balanced performance"),
    CLAUDE_3_HAIKU("claude-3-haiku", "Claude 3 Haiku", "Anthropic", "Fast and compact"),
    
    // Google Models
    GEMINI_PRO("gemini-pro", "Gemini Pro", "Google", "Google's advanced AI model"),
    GEMINI_PRO_VISION("gemini-pro-vision", "Gemini Pro Vision", "Google", "Multimodal model"),
    NANO_BANANA("google/nano-banana", "Nano Banana", "Google", "Google's state-of-the-art image generation and editing model"),
    
    // Stability AI
    STABLE_DIFFUSION_XL("stable-diffusion-xl", "Stable Diffusion XL", "Stability AI", "Image generation"),
    
    // Custom/Internal Models
    CUSTOM("custom", "Custom Model", "Internal", "Custom or internal model"),
    NONE("none", "No Model", "N/A", "No AI model required");

    private final String code;          // Model identifier code
    private final String name;          // Model display name
    private final String provider;      // Model provider (OpenAI, Anthropic, etc.)
    private final String description;   // Model description

    ModelType(String code, String name, String provider, String description) {
        this.code = code;
        this.name = name;
        this.provider = provider;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getProvider() {
        return provider;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Find ModelType by code
     * @param code Model code (e.g., "gpt-4", "dall-e-3")
     * @return ModelType or null if not found
     */
    public static ModelType findByCode(String code) {
        if (code == null) {
            return null;
        }
        for (ModelType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }

    /**
     * Check if this is a chat model
     * @return true if this is a chat/text generation model
     */
    public boolean isChatModel() {
        return this == GPT_3_5_TURBO || this == GPT_4 || this == GPT_4_TURBO || this == GPT_4O
                || this == CLAUDE_3_OPUS || this == CLAUDE_3_SONNET || this == CLAUDE_3_HAIKU
                || this == GEMINI_PRO;
    }

    /**
     * Check if this is an image model
     * @return true if this is an image generation model
     */
    public boolean isImageModel() {
        return this == DALL_E_2 || this == DALL_E_3 || this == STABLE_DIFFUSION_XL || this == GEMINI_PRO_VISION || this == NANO_BANANA;
    }

    /**
     * Check if this is an embedding model
     * @return true if this is an embedding model
     */
    public boolean isEmbeddingModel() {
        return this == TEXT_EMBEDDING_ADA_002 || this == TEXT_EMBEDDING_3_SMALL || this == TEXT_EMBEDDING_3_LARGE;
    }
}

