package com.trade.entity;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@Table(name = "permissions")
public class Permission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 100)
    private String code;

    private String name;

    private String description;

    private String module;

    private String action;
}