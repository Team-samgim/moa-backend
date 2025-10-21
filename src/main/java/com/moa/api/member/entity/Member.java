package com.moa.api.member.entity;

import com.moa.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "members",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_members_email", columnNames = "email"),
                @UniqueConstraint(name = "uk_members_login_id", columnNames = "login_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class Member extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "login_id", nullable = false, length = 32)
    private String loginId;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable=false)
    private String nickname;

    @Enumerated(EnumType.STRING) @Column(nullable=false)
    private Role role = Role.ROLE_USER;

    @Column(nullable=false)
    private boolean enabled = true;
}