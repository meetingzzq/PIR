package com.example.email.entity;

import lombok.Data;
import javax.persistence.*;

@Data
@Entity
@Table(name = "email")
public class Email {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String subject;
    private String sender;
}