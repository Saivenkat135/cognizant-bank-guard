package com.cts.apiGateway.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.relational.core.mapping.Column;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("users")
public class User {

    @Id
    private Long id;

    @Column("username")
    private String username;

    @Column("password")
    private String password;

    @Column("role")
    private String role;

    // SuperAdmin must approve FraudAnalyst / RiskManager signups before they can log in.
    // SUPER_ADMIN rows are seeded as approved.
    @Column("is_approved")
    private Boolean isApproved;
}
