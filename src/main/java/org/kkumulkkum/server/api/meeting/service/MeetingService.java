package org.kkumulkkum.server.api.meeting.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kkumulkkum.server.api.meeting.dto.projection.MeetingMetCountProjection;
import org.kkumulkkum.server.api.meeting.dto.projection.MemberProjection;
import org.kkumulkkum.server.domain.meeting.manager.MeetingEditor;
import org.kkumulkkum.server.domain.meeting.manager.MeetingRemover;
import org.kkumulkkum.server.domain.meeting.manager.MeetingRetriever;
import org.kkumulkkum.server.domain.meeting.manager.MeetingSaver;
import org.kkumulkkum.server.domain.member.manager.MemberRemover;
import org.kkumulkkum.server.domain.member.manager.MemberRetreiver;
import org.kkumulkkum.server.domain.member.manager.MemberSaver;
import org.kkumulkkum.server.domain.meeting.Meeting;
import org.kkumulkkum.server.domain.member.Member;
import org.kkumulkkum.server.api.meeting.dto.request.MeetingCreateDto;
import org.kkumulkkum.server.api.meeting.dto.request.MeetingRegisterDto;
import org.kkumulkkum.server.api.meeting.dto.response.CreatedMeetingDto;
import org.kkumulkkum.server.api.meeting.dto.response.MeetingDto;
import org.kkumulkkum.server.api.meeting.dto.response.MeetingsDto;
import org.kkumulkkum.server.api.meeting.dto.response.MembersDto;
import org.kkumulkkum.server.common.exception.MeetingException;
import org.kkumulkkum.server.common.exception.code.MeetingErrorCode;
import org.kkumulkkum.server.domain.participant.manager.ParticipantRemover;
import org.kkumulkkum.server.domain.participant.manager.ParticipantRetriever;
import org.kkumulkkum.server.domain.promise.manager.PromiseRemover;
import org.kkumulkkum.server.domain.promise.manager.PromiseRetriever;
import org.kkumulkkum.server.domain.user.manager.UserRetriever;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MeetingService {

    private final MeetingSaver meetingSaver;
    private final MeetingRetriever meetingRetriever;
    private final UserRetriever userRetriever;
    private final MemberSaver memberSaver;
    private final MemberRetreiver memberRetreiver;
    private final MeetingEditor meetingEditor;
    private final ParticipantRemover participantRemover;
    private final MemberRemover memberRemover;
    private final PromiseRemover promiseRemover;
    private final MeetingRemover meetingRemover;
    private final PromiseRetriever promiseRetriever;
    private final ParticipantRetriever participantRetriever;

    @Transactional
    public CreatedMeetingDto createMeeting(
            final Long userId,
            final MeetingCreateDto meetingCreateDto
    ) {
        String invitationCode = generateInvitationCode();

        Meeting meeting = Meeting.builder()
                .name(meetingCreateDto.name())
                .invitationCode(invitationCode)
                .build();
        meetingSaver.save(meeting);

        memberSaver.save(Member.builder()
                .meeting(meeting)
                .user(userRetriever.findById(userId))
                .build());

        return new CreatedMeetingDto(meeting.getId(), meeting.getInvitationCode());
    }

    @Transactional
    public Long registerMeeting(
            final Long userId,
            final MeetingRegisterDto meetingRegisterDto
    ) {
        Meeting meeting = meetingRetriever.findByInvitationCode(meetingRegisterDto.invitationCode());
        Member member = Member.builder()
                .meeting(meeting)
                .user(userRetriever.findById(userId))
                .build();
        if (memberRetreiver.existsByMeetingIdAndUserId(meeting.getId(), userId)) {
            throw new MeetingException(MeetingErrorCode.ALREADY_JOINED);
        }
        memberSaver.save(member);

        return meeting.getId();
    }

    @Transactional(readOnly = true)
    public MeetingsDto getMeetings(final Long userId) {
        List<Meeting> meetings = meetingRetriever.findAllByUserId(userId);
        return MeetingsDto.from(meetings);
    }

    @Transactional(readOnly = true)
    public MeetingDto getMeeting(final Long meetingId) {
        MeetingMetCountProjection meeting = meetingRetriever.findByIdWithMetCount(meetingId);
        return MeetingDto.of(
                meeting.getId(),
                meeting.getName(),
                meeting.getCreatedAt(),
                meeting.getInvitationCode(),
                meeting.getMetCount()
        );
    }

    @Transactional(readOnly = true)
    public MembersDto getMembers(final Long meetingId, final String exclude, final Long userId) {
        List<MemberProjection> members = memberRetreiver.findAllByMeetingId(meetingId);
        if (exclude != null) {
            Member authenticatedMember = memberRetreiver.findByMeetingIdAndUserId(meetingId, userId);
            members.removeIf(member -> member.getMemberId().equals(authenticatedMember.getId()));
        }
        return MembersDto.from(members);
    }

    @Transactional
    public void updateMeeting(
            final Long meetingId,
            final MeetingCreateDto meetingCreateDto
    ) {
        Meeting meeting = meetingRetriever.findById(meetingId);
        meetingEditor.updateMeetingName(meeting, meetingCreateDto);
    }

    @Transactional
    public void leaveMeeting(
            final Long userId,
            final Long meetingId) {
        //모임 나가면 참여한 약속도 다 나가기
        Member member = memberRetreiver.findByMeetingIdAndUserId(meetingId, userId);
        participantRemover.deleteByMemberId(member.getId());
        memberRemover.deleteById(member.getId());

        // 모임 내 참여 인원이 전부 탈퇴 or 나가기로 없을 경우(모임 사라지면) 약속도 다 삭제하기
        List<MemberProjection> remainingMembers = memberRetreiver.findAllByMeetingId(meetingId);
        if(remainingMembers.isEmpty()) {
            promiseRemover.deleteByMeetingId(meetingId);
            meetingRemover.deleteById(meetingId);
        }

        //모임 내의 약속들 모두 돌려서, 거기서 참여자가 없는 약속이 있다면 지우기
        removeEmptyPromises(meetingId);
    }

    private String generateInvitationCode() {
        String invitationCode;

        do {
            invitationCode = generateRandomCode();
        } while (meetingRetriever.existsByInvitationCode(invitationCode));

        return invitationCode;
    }

    private String generateRandomCode() {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        SecureRandom random = new SecureRandom();
        StringBuilder codeBuilder = new StringBuilder();

        for (int i = 0; i < 6; i++) {
            int index = random.nextInt(characters.length());
            codeBuilder.append(characters.charAt(index));
        }

        return codeBuilder.toString();
    }

    private void removeEmptyPromises(final Long meetingId) {
        promiseRetriever.findAllByMeetingId(meetingId).stream()
                .filter(promise -> participantRetriever.findAllByPromiseId(promise.getId()).isEmpty())
                .forEach(promise -> promiseRemover.deleteById(promise.getId()));
    }

}
