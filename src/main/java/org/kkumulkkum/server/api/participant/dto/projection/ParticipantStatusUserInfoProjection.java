package org.kkumulkkum.server.api.participant.dto.projection;

import java.time.LocalDateTime;

public record ParticipantStatusUserInfoProjection(
        Long participantId,
        Long memberId,
        String name,
        String profileImg,
        LocalDateTime preparationAt,
        LocalDateTime departureAt,
        LocalDateTime arrivalAt,
        String state
) {
}
