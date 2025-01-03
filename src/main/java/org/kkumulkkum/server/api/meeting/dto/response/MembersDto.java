package org.kkumulkkum.server.api.meeting.dto.response;

import org.kkumulkkum.server.api.meeting.dto.projection.MemberProjection;

import java.util.List;

public record MembersDto(
        int memberCount,
        List<MemberProjection> members
) {
    public static MembersDto from(List<MemberProjection> members) {
        return new MembersDto(
                members.size(),
                members
        );
    }
}
