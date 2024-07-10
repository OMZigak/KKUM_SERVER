package org.kkumulkkum.server.dto.participant.response;

import java.util.List;

public record ParticipantsDto(
        int participantCount,
        List<ParticipantDto> participants
) {
    public static ParticipantsDto from(List<ParticipantDto> participants) {
        return new ParticipantsDto(
                participants.size(),
                participants
        );
    }
}