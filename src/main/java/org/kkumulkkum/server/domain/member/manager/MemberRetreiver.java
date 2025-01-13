package org.kkumulkkum.server.domain.member.manager;

import lombok.RequiredArgsConstructor;
import org.kkumulkkum.server.api.meeting.dto.projection.MemberProjection;
import org.kkumulkkum.server.domain.member.Member;
import org.kkumulkkum.server.common.exception.MemberException;
import org.kkumulkkum.server.common.exception.code.MemberErrorCode;
import org.kkumulkkum.server.domain.member.repository.MemberRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class MemberRetreiver {

    private final MemberRepository memberRepository;

    public Member findById(final Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.NOT_FOUND_MEMBER));
    }

    public boolean existsByMeetingIdAndUserId(
            final Long meetingId,
            final Long userId
    ) {
        return memberRepository.existsByMeetingIdAndUserId(meetingId, userId);
    }

    public List<MemberProjection> findAllByMeetingId(final Long meetingId) {
        return memberRepository.findAllByMeetingId(meetingId);
    }

    public boolean existsByPromiseIdAndUserId(
            final Long promiseId,
            final Long userId
    ) {
        return memberRepository.existsByPromiseIdAndUserId(promiseId, userId);
    }

    public Member findByMeetingIdAndUserId(Long meetingId, Long userId) {
        return memberRepository.findByMeetingIdAndUserId(meetingId, userId);
    }

    public List<Member> findByUserId(final Long userId) {
        return memberRepository.findByUserId(userId);
    }

    public Member findByUserIdAndPromiseId(
            final Long userId,
            final Long promiseId
    ) {
        return memberRepository.findByUserIdAndPromiseId(userId, promiseId);
    }

    public List<MemberProjection> findAllByPromiseId(final Long promiseId) {
        return memberRepository.findAllByPromiseId(promiseId);
    }
}
