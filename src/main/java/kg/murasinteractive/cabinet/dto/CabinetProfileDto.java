package kg.murasinteractive.cabinet.dto;

import java.time.Instant;

public record CabinetProfileDto(String displayName, String email, String role,
                                Instant registeredAt, int totalPoints) {
}
