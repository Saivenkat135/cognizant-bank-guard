package com.cts.sarreport.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Date;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SarReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int sarId;
    private Date localDate;


    private Long caseId;
    private Long customerId;
    private String status;
    private double riskScore;
    @Column(length = 3000)
    private String reason;


    private Double amount;
    private String city;
    private String state;
    private LocalDateTime time;
    private String customerName;
    private String customerEmail;
    private String customerAccountNo;
}
