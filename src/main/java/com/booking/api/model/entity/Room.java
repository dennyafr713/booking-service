package com.booking.api.model.entity;

import com.booking.api.model.entity.base.Base;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@Builder
@Table(name = "rooms")
@AllArgsConstructor
@NoArgsConstructor
public class Room extends Base {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private Integer capacity;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}
