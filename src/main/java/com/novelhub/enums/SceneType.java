package com.novelhub.enums;

/**
 * Scene Type Enum
 * Defines business scenarios/categories for functions
 */
public enum SceneType {
    // Communication & Chat
    CHAT("chat", "Chat", "Text-based chat and conversation"),
    DIALOGUE("dialogue", "Dialogue", "Multi-turn dialogue systems"),
    
    // Content Generation
    TEXT_GENERATION("text-generation", "Text Generation", "Generate articles, stories, content"),
    WRITING("writing", "Writing Assistance", "Writing aid, editing, proofreading"),
    TRANSLATION("translation", "Translation", "Language translation services"),
    SUMMARIZATION("summarization", "Summarization", "Text summarization and abstraction"),
    
    // Image & Media
    IMAGE("image", "Image Generation", "Create images from text descriptions"),
    IMAGE_EDIT("image-edit", "Image Editing", "Edit and modify images"),
    IMAGE_ANALYSIS("image-analysis", "Image Analysis", "Analyze and describe images"),
    VIDEO("video", "Video Processing", "Video generation or processing"),
    AUDIO("audio", "Audio Processing", "Audio generation or processing"),
    
    // Data & Analysis
    ANALYSIS("analysis", "Data Analysis", "Analyze data and generate insights"),
    RESEARCH("research", "Research", "Research assistance and information gathering"),
    CODE("code", "Code Generation", "Programming and code assistance"),
    MATH("math", "Mathematics", "Mathematical computations and problem solving"),
    
    // Search & Retrieval
    SEARCH("search", "Search", "Information search and retrieval"),
    QA("qa", "Question Answering", "Answer questions based on knowledge"),
    RAG("rag", "RAG", "Retrieval-Augmented Generation"),
    
    // Business Applications
    CUSTOMER_SERVICE("customer-service", "Customer Service", "Customer support and service"),
    MARKETING("marketing", "Marketing", "Marketing content and campaigns"),
    EDUCATION("education", "Education", "Educational content and tutoring"),
    HEALTHCARE("healthcare", "Healthcare", "Healthcare information and assistance"),
    
    // Specialized
    EMBEDDING("embedding", "Embedding", "Text embedding and vectorization"),
    MODERATION("moderation", "Content Moderation", "Content safety and moderation"),
    CLASSIFICATION("classification", "Classification", "Text or data classification"),
    
    // General
    DEMO("demo", "Demo/Test", "Demo and testing purposes"),
    OTHER("other", "Other", "Other uncategorized scenarios");

    private final String code;          // Scene identifier code
    private final String name;          // Scene display name
    private final String description;   // Scene description

    SceneType(String code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Find SceneType by code
     * @param code Scene code (e.g., "chat", "image", "analysis")
     * @return SceneType or null if not found
     */
    public static SceneType findByCode(String code) {
        if (code == null) {
            return null;
        }
        for (SceneType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }

    /**
     * Check if this scene requires real-time interaction
     * @return true if real-time interaction is expected
     */
    public boolean isRealTime() {
        return this == CHAT || this == DIALOGUE || this == CUSTOMER_SERVICE;
    }

    /**
     * Check if this scene involves content creation
     * @return true if primary purpose is content creation
     */
    public boolean isCreative() {
        return this == TEXT_GENERATION || this == WRITING || this == IMAGE 
                || this == IMAGE_EDIT || this == VIDEO || this == AUDIO;
    }

    /**
     * Check if this scene involves data/analysis
     * @return true if primary purpose is analysis
     */
    public boolean isAnalytical() {
        return this == ANALYSIS || this == RESEARCH || this == MATH 
                || this == IMAGE_ANALYSIS || this == CLASSIFICATION;
    }
}

