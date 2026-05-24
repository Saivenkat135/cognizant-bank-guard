package com.cts.AlertCaseService.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Entity
@Data
public class CaseCustomer {
    @Id
    private Long customerId;

    // The mappedBy = "customer" tells JPA that the 'customer' field
    // inside CaseEntity is the owner of this relationship.
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CaseEntity> cases = new ArrayList<>();
}
