package com.harish.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Entity
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "usage_logs", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "date"})
})
public class UsageLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @Column(name = "user_id", nullable = false)
    Long userId;
    @Column(nullable = false)
    LocalDate date;
    Integer tokensUsed;

}