package org.kkumulkkum.server.domain.meeting.manager;

import lombok.RequiredArgsConstructor;
import org.kkumulkkum.server.api.meeting.dto.projection.MeetingMetCountProjection;
import org.kkumulkkum.server.domain.meeting.Meeting;
import org.kkumulkkum.server.common.exception.MeetingException;
import org.kkumulkkum.server.common.exception.code.MeetingErrorCode;
import org.kkumulkkum.server.domain.meeting.repository.MeetingRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class MeetingRetriever {

    private final MeetingRepository meetingRepository;

    public boolean existsByInvitationCode(final String invitationCode) {
        return meetingRepository.existsByInvitationCode(invitationCode);
    }

    public Meeting findByInvitationCode(final String invitationCode) {
        return meetingRepository.findByInvitationCode(invitationCode)
                .orElseThrow(() -> new MeetingException(MeetingErrorCode.NOT_FOUND_MEETING));
    }

    public List<Meeting> findAllByUserId(final Long userId) {
        return meetingRepository.findAllByUserId(userId);
    }

    public Meeting findById(final Long meetingId) {
        return meetingRepository.findById(meetingId)
                .orElseThrow(() -> new MeetingException(MeetingErrorCode.NOT_FOUND_MEETING));
    }

    public MeetingMetCountProjection findByIdWithMetCount(final Long meetingId) {
        return meetingRepository.findByIdWithMetCount(meetingId)
                .orElseThrow(() -> new MeetingException(MeetingErrorCode.NOT_FOUND_MEETING));
    }
}
