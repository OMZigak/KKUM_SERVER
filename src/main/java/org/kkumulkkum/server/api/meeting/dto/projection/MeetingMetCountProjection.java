package org.kkumulkkum.server.api.meeting.dto.projection;

import java.time.LocalDateTime;

public interface MeetingMetCountProjection {
    Long getId();
    String getName();
    LocalDateTime getCreatedAt();
    String getInvitationCode();
    Long getMetCount();
}
