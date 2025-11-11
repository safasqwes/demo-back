package com.novelhub.vo.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * User Response VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private String objectId;
    private String username;
    private String email;
    private Date createdAt;
    private Date updatedAt;
    private String nickname;
    private String avatar;
}

