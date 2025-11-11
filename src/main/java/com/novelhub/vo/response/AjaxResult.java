package com.novelhub.vo.response;

import java.util.HashMap;
import java.util.Map;

/**
 * Standard AJAX Response Object
 */
public class AjaxResult extends HashMap<String, Object> {
    private static final long serialVersionUID = 1L;

    /** Status code */
    public static final String CODE_TAG = "code";

    /** Return content */
    public static final String MSG_TAG = "msg";
    
    /** Success flag */
    public static final String SUCCESS_TAG = "success";

    /** Data object */
    public static final String DATA_TAG = "data";

    /**
     * Initialize a newly created AjaxResult object
     *
     * @param code status code
     * @param msg return message
     * @param data data object
     */
    public AjaxResult(int code, String msg, Object data) {
        super.put(CODE_TAG, code);
        super.put(MSG_TAG, msg);
        super.put(SUCCESS_TAG, code == 200); // success is true only when code is 200
        if (data != null) {
            super.put(DATA_TAG, data);
        }
    }

    /**
     * Initialize a newly created AjaxResult object
     *
     * @param code status code
     * @param msg return message
     */
    public AjaxResult(int code, String msg) {
        super.put(CODE_TAG, code);
        super.put(MSG_TAG, msg);
    }

    /**
     * Return success message
     *
     * @return success message
     */
    public static AjaxResult success() {
        return AjaxResult.success("Operation successful");
    }

    /**
     * Return success data
     *
     * @return success message
     */
    public static AjaxResult success(Object data) {
        return AjaxResult.success("Operation successful", data);
    }

    /**
     * Return success message
     *
     * @param msg return message
     * @return success message
     */
    public static AjaxResult success(String msg) {
        return AjaxResult.success(msg, null);
    }

    /**
     * Return success message
     *
     * @param msg return message
     * @param data data object
     * @return success message
     */
    public static AjaxResult success(String msg, Object data) {
        return new AjaxResult(200, msg, data);
    }

    /**
     * Return error message
     *
     * @return error message
     */
    public static AjaxResult error() {
        return AjaxResult.error("Operation failed");
    }

    /**
     * Return error message
     *
     * @param msg return message
     * @return error message
     */
    public static AjaxResult error(String msg) {
        return AjaxResult.error(msg, null);
    }

    /**
     * Return error message
     *
     * @param msg return message
     * @param data data object
     * @return error message
     */
    public static AjaxResult error(String msg, Object data) {
        return new AjaxResult(500, msg, data);
    }
    
    /**
     * Return error message with custom code
     *
     * @param code status code
     * @param msg return message
     * @param data data object
     * @return error message
     */
    public static AjaxResult error(int code, String msg, Object data) {
        return new AjaxResult(code, msg, data);
    }

    /**
     * Return error message
     *
     * @param code status code
     * @param msg return message
     * @return error message
     */
    public static AjaxResult error(int code, String msg) {
        return new AjaxResult(code, msg, null);
    }

    /**
     * Convenience method to add data to result
     *
     * @param key key
     * @param value value
     * @return AjaxResult
     */
    @Override
    public AjaxResult put(String key, Object value) {
        super.put(key, value);
        return this;
    }

    /**
     * Convenience method to set data
     *
     * @param data data map
     * @return AjaxResult
     */
    public AjaxResult putData(Map<String, Object> data) {
        if (data != null && !data.isEmpty()) {
            super.put(DATA_TAG, data);
        }
        return this;
    }
}

