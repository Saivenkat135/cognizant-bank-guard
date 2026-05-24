package com.bankguard.transactionservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id")
    private Long transactionId;

    @Column(name = "amount", nullable = false)
    private Double amount;

    @Column(name = "city")
    private String city;

    @Column(name = "state")
    private String state;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "transaction_time")
    private LocalDateTime time;

    @Column(name = "risk_score")
    private Double riskScore;

    @Column(name = "receiver_account_number", nullable = false)
    private String receiverAccountNumber;

    @Column(name = "status", nullable = false)
    private String status = "pending"; // Default status

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name="reason",columnDefinition = "TEXT")
    private String reason;
}
