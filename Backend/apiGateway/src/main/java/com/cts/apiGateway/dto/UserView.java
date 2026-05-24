package com.cts.apiGateway.dto;

import com.cts.apiGateway.model.User;
import lombok.*;

/**
 * User row sent to the frontend (password never leaves the server).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserView {
    private Long id;
    private String username;
    private String role;
    private Boolean isApproved;

    public static UserView from(User u) {
        return UserView.builder()
                .id(u.getId())
                .username(u.getUsername())
                .role(u.getRole())
                .isApproved(Boolean.TRUE.equals(u.getIsApproved()))
                .build();
    }
}
