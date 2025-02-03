package org.kkumulkkum.server.api.promise.service;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.kkumulkkum.server.domain.promise.manager.PromiseEditor;
import org.kkumulkkum.server.domain.promise.manager.PromiseRemover;
import org.kkumulkkum.server.domain.promise.manager.PromiseRetriever;
import org.kkumulkkum.server.domain.promise.manager.PromiseSaver;
import org.kkumulkkum.server.api.promise.dto.response.*;
import org.kkumulkkum.server.domain.meeting.Meeting;
import org.kkumulkkum.server.domain.member.Member;
import org.kkumulkkum.server.domain.participant.Participant;
import org.kkumulkkum.server.domain.promise.Promise;
import org.kkumulkkum.server.domain.userinfo.UserInfo;
import org.kkumulkkum.server.api.promise.dto.request.PromiseCreateDto;
import org.kkumulkkum.server.common.exception.PromiseException;
import org.kkumulkkum.server.common.exception.code.PromiseErrorCode;
import org.kkumulkkum.server.domain.member.manager.MemberRetreiver;
import org.kkumulkkum.server.domain.participant.manager.ParticipantRemover;
import org.kkumulkkum.server.domain.participant.manager.ParticipantRetriever;
import org.kkumulkkum.server.domain.participant.manager.ParticipantSaver;
import org.kkumulkkum.server.domain.userinfo.manager.UserInfoRetriever;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PromiseService {

    private final PromiseSaver promiseSaver;
    private final PromiseRetriever promiseRetriever;
    private final PromiseEditor promiseEditor;
    private final ParticipantSaver participantSaver;
    private final ParticipantRetriever participantRetriever;
    private final UserInfoRetriever userInfoRetriever;
    private final EntityManager entityManager;
    private final MemberRetreiver memberRetreiver;
    private final PromiseRemover promiseRemover;
    private final ParticipantRemover participantRemover;

    @Transactional
    public PromiseAddDto createPromise(
            final Long userId,
            final Long meetingId,
            final PromiseCreateDto createPromiseDto
    ) {
        Promise promise = Promise.builder()
                .meeting(entityManager.getReference(Meeting.class, meetingId))
                .name(createPromiseDto.name())
                .placeName(createPromiseDto.placeName())
                .x(createPromiseDto.x())
                .y(createPromiseDto.y())
                .address(createPromiseDto.address())
                .roadAddress(createPromiseDto.roadAddress())
                .time(createPromiseDto.time())
                .dressUpLevel(createPromiseDto.dressUpLevel())
                .penalty(createPromiseDto.penalty())
                .build();
        promiseSaver.save(promise);

        Member member = memberRetreiver.findByMeetingIdAndUserId(meetingId, userId);
        createPromiseDto.participants().add(member.getId());
        participantSaver.saveAll(
                createPromiseDto.participants().stream()
                .map(participantId
                        -> Participant.builder()
                        .promise(promise)
                        .member(entityManager.getReference(Member.class, participantId))
                        .build()).toList()
        );
        return PromiseAddDto.from(promise);
    }

    @Transactional
    public void completePromise(final Long promiseId) {
        Promise promise = promiseRetriever.findById(promiseId);
        if (promise.getTime().isAfter(LocalDateTime.now())) {
            throw new PromiseException(PromiseErrorCode.NOT_PAST_DUE);
        }

        if (promiseRetriever.existsByArrivedAtIsNull(promiseId)) {
            throw new PromiseException(PromiseErrorCode.NOT_ARRIVED_PARTICIPANT_EXISTS);
        }

        promiseEditor.completePromise(promise);
        List<Participant> participants = participantRetriever.findAllByPromiseId(promiseId);
        participants.forEach(participant -> updateUserInfo(participant, promise.getTime()));
    }

    @Transactional(readOnly = true)
    public PromisesDto getPromises(
            final Long userId,
            final Long meetingId,
            final Boolean done,
            final Boolean isParticipant
    ) {
//        List<Promise> allPromises = promiseRetriever.findAllByMeetingId(meetingId);
//        List<Promise> userPromises = promiseRetriever.findPromiseByUserIdAndMeetingId(userId, meetingId);
//        List<Promise> promises;
//
//        if (Boolean.TRUE.equals(isParticipant)) {
//            promises = userPromises;
//        } else if (Boolean.FALSE.equals(isParticipant)) {
//            promises = allPromises.stream()
//                    .filter(promise -> !userPromises.contains(promise))
//                    .collect(Collectors.toList());
//        } else {
//            promises = allPromises;
//        }

        List<Promise> promises = promiseRetriever.findPromisesByConditions(userId, meetingId, done, isParticipant);

        return PromisesDto.of(promises);
    }

    @Transactional(readOnly = true)
    public PromiseDetailDto getPromise(
            final Long userId,
            final Long promiseId
    ) {
        Promise promise = promiseRetriever.findById(promiseId);
        Promise userPromise = promiseRetriever.findByUserIdAndPromiseId(userId, promiseId);

        boolean isParticipant = userPromise != null;

        return PromiseDetailDto.from(promise, isParticipant);
    }

    @Transactional(readOnly = true)
    public MainPromiseDto getNextPromise(final Long userId) {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime startOfNextDay = startOfDay.plusDays(1);
        List<Promise> promise = promiseRetriever.findNextPromiseByUserId(userId, startOfDay, startOfNextDay);
        if (promise.isEmpty()) {
            return null;
        }
        return MainPromiseDto.from(promise.get(0));
    }

    @Transactional(readOnly = true)
    public MainPromisesDto getUpcomingPromises(final Long userId) {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime startOfNextDay = startOfDay.plusDays(1);
        List<Promise> nextPromise = promiseRetriever.findNextPromiseByUserId(userId, startOfDay, startOfNextDay);

        if (!nextPromise.isEmpty()) {
            return MainPromisesDto.from(
                    promiseRetriever.findUpcomingPromisesExcludingNext(userId, nextPromise.get(0), 4)
            );
        }
        return MainPromisesDto.from(promiseRetriever.findUpcomingPromises(userId, 4));
    }

    @Transactional
    public PromiseAddDto updatePromise(
            final Long userId,
            final Long promiseId,
            final PromiseCreateDto updatePromiseDto
    ) {
        // 약속 정보 수정
        Promise promise = promiseRetriever.findById(promiseId);
        promiseEditor.updatePromise(promise, updatePromiseDto);

        // 참여자 목록 수정
        Member member = memberRetreiver.findByUserIdAndPromiseId(userId, promiseId);
        updatePromiseDto.participants().add(member.getId());

        List<Long> currentMembers = participantRetriever.findAllByPromiseId(promiseId).stream()
                                            .map(participant -> participant.getMember().getId())
                                            .toList();
        List<Long> newMembers = updatePromiseDto.participants();

        saveNewParticipants(promise, newMembers, currentMembers);
        removeOldParticipants(promiseId, newMembers, currentMembers);

        return PromiseAddDto.from(promise);
    }

    @Transactional
    public void deletePromise(final Long promiseId) {
        Promise promise = promiseRetriever.findById(promiseId);

        List<Participant> participants = participantRetriever.findAllByPromiseId(promise.getId());
        // 약속에 속한 모든 참가자 삭제
        participantRemover.deleteAll(participants);

        // 약속 삭제
        promiseRemover.deleteById(promise.getId());
    }

    private void updateUserInfo(
            final Participant participant,
            final LocalDateTime promiseTime
    ) {
        UserInfo userInfo = userInfoRetriever.findByParticipantId(participant.getId());
        userInfo.addPromiseCount();
        if (promiseTime.isBefore(participant.getArrivalAt())) {
            userInfo.addLateCount();
            userInfo.addLateTime(Duration.between(promiseTime, participant.getArrivalAt()).getSeconds());
        } else {
            userInfo.levelUp();
        }
    }

    private void saveNewParticipants(
            final Promise promise,
            final List<Long> newMembers,
            final List<Long> currentMembers
    ) {
        participantSaver.saveAll(
                newMembers.stream()
                        .filter(memberId -> !currentMembers.contains(memberId))
                        .map(memberId -> {
                            Member member = memberRetreiver.findById(memberId);
                            return Participant.builder()
                                    .promise(promise)
                                    .member(member)
                                    .build();
                        }).toList()
        );
    }

    private void removeOldParticipants(
            final Long promiseId,
            final List<Long> newMembers,
            final List<Long> currentMembers
    ) {
        participantRemover.deleteAll(
                currentMembers.stream()
                        .filter(memberId -> !newMembers.contains(memberId))
                        .map(memberId -> {
                            memberRetreiver.findById(memberId);
                            return participantRetriever.findByMemberIdAndPromiseId(memberId, promiseId);
                        }).toList()
        );
    }
}
