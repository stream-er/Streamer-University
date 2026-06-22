package com.example.Streamer.University;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "user_bot")
public class UserBot {
    @Id
    private Long chatId;
    private String roles;
    private String dob;
    private String fullName;
    private String email;
    private String locationType;
    private String socialMediaHandle;
    @Enumerated(EnumType.STRING)
    private UseState state;
    @Enumerated(EnumType.STRING)
    private SocialMediaState socialMediaState;

}
