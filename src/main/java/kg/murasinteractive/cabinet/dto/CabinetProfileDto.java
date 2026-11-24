package kg.murasinteractive.cabinet.dto;

import java.time.Instant;

public record CabinetProfileDto(String displayName, String email, String role, String avatarUrl,
                                Instant registeredAt, int totalPoints) {
}
