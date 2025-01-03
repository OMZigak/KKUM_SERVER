package org.kkumulkkum.server.api.participant.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kkumulkkum.server.api.participant.dto.projection.ParticipantStatusUserInfoProjection;
import org.kkumulkkum.server.domain.participant.manager.ParticipantEditor;
import org.kkumulkkum.server.domain.participant.manager.ParticipantRemover;
import org.kkumulkkum.server.domain.participant.manager.ParticipantRetriever;
import org.kkumulkkum.server.api.participant.dto.response.*;
import org.kkumulkkum.server.domain.member.Member;
import org.kkumulkkum.server.domain.participant.Participant;
import org.kkumulkkum.server.domain.promise.Promise;
import org.kkumulkkum.server.api.meeting.dto.response.MemberDto;
import org.kkumulkkum.server.api.participant.dto.request.PreparationInfoDto;
import org.kkumulkkum.server.common.exception.ParticipantException;
import org.kkumulkkum.server.common.exception.code.ParticipantErrorCode;
import org.kkumulkkum.server.external.service.fcm.FcmService;
import org.kkumulkkum.server.external.service.fcm.dto.FcmMessageDto;
import org.kkumulkkum.server.external.service.fcm.FcmContent;
import org.kkumulkkum.server.domain.member.manager.MemberRetreiver;
import org.kkumulkkum.server.domain.promise.manager.PromiseRemover;
import org.kkumulkkum.server.domain.promise.manager.PromiseRetriever;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ParticipantService {

    private final ParticipantRetriever participantRetriever;
    private final ParticipantEditor participantEditor;
    private final PromiseRetriever promiseRetriever;
    private final MemberRetreiver memberRetreiver;
    private final ParticipantRemover participantRemover;
    private final PromiseRemover promiseRemover;
    private final FcmService fcmService;

    @Transactional
    public void preparePromise(
            final Long userId,
            final Long promiseId
    ) {
        Participant participant = participantRetriever.findByPromiseIdAndUserId(promiseId, userId);
        if (!validateState(participant, "preparation")) {
            throw new ParticipantException(ParticipantErrorCode.INVALID_STATE);
        }
        participantEditor.preparePromise(participant);

        int preparationCount = participantRetriever.countFirstPreparationByPromiseId(promiseId);
        if (preparationCount == 1) {
            List<String> fcmTokens = participantRetriever.findFcmTokenByPromiseId(promiseId, userId);
            fcmService.sendBulk(fcmTokens, FcmMessageDto.of(FcmContent.FIRST_PREPARATION, promiseId));
        }
    }

    @Transactional
    public void departurePromise(
            final Long userId,
            final Long promiseId
    ) {
        Participant participant = participantRetriever.findByPromiseIdAndUserId(promiseId, userId);
        if (!validateState(participant, "departure")) {
            throw new ParticipantException(ParticipantErrorCode.INVALID_STATE);
        }
        participantEditor.departurePromise(participant);

        int departureCount = participantRetriever.countFirstDepartureByPromiseId(promiseId);
        if (departureCount == 1) {
            List<String> fcmTokens = participantRetriever.findFcmTokenByPromiseId(promiseId, userId);
            fcmService.sendBulk(fcmTokens, FcmMessageDto.of(FcmContent.FIRST_DEPARTURE, promiseId));
        }
    }

    @Transactional
    public void arrivalPromise(
            final Long userId,
            final Long promiseId
    ) {
        Participant participant = participantRetriever.findByPromiseIdAndUserId(promiseId, userId);
        if (!validateState(participant, "arrival")) {
            throw new ParticipantException(ParticipantErrorCode.INVALID_STATE);
        }
        participantEditor.arrivalPromise(participant);

        int arrivalCount = participantRetriever.countFirstArrivalByPromiseId(promiseId);
        if (arrivalCount == 1) {
            List<String> fcmTokens = participantRetriever.findFcmTokenByPromiseId(promiseId, userId);
            fcmService.sendBulk(fcmTokens, FcmMessageDto.of(FcmContent.FIRST_ARRIVAL, promiseId));
        }
    }

    @Transactional(readOnly = true)
    public PreparationStatusDto getPreparation(
            final Long userId,
            final Long promiseId
    ) {
        Participant participant = participantRetriever.findByPromiseIdAndUserId(promiseId, userId);
        return PreparationStatusDto.from(participant);
    }

    @Transactional(readOnly = true)
    public ParticipantsDto getParticipants(final Long promiseId) {
        List<ParticipantStatusUserInfoProjection> participants = participantRetriever.findAllByPromiseIdWithUserInfo(promiseId);

        List<ParticipantDto> sortedParticipants = participants.stream()
                .map(participant -> ParticipantDto.of(
                        participant.participantId(),
                        participant.memberId(),
                        participant.name(),
                        participant.profileImg(),
                        participant.state()
                ))
                .collect(Collectors.toList());

        return ParticipantsDto.from(sortedParticipants);
    }

    @Transactional(readOnly = true)
    public AvailableParticipantsDto getAvailableParticipants(
            final Long userId,
            final Long promiseId
    ) {
        //모임 내 멤버 목록
        List<MemberDto> members = memberRetreiver.findAllByPromiseId(promiseId);
        //나 제외
        Member authenticatedMember = memberRetreiver.findByUserIdAndPromiseId(userId, promiseId);
        members.removeIf(member -> member.memberId().equals(authenticatedMember.getId()));

        //약속에 참여 중인 멤버 id들 가져오기
        List<Long> participantIds = participantRetriever.findAllByPromiseId(promiseId).stream()
                                                                .map(participant -> participant.getMember().getId())
                                                                .toList();

        return AvailableParticipantsDto.of(members, participantIds);
    }

    @Transactional
    public void insertPreparationInfo(
            final Long userId,
            final Long promiseId,
            final PreparationInfoDto preparationInfoDto
    ) {
        Participant participant = participantRetriever.findByPromiseIdAndUserId(promiseId, userId);
        participantEditor.updatePreparationTime(participant, preparationInfoDto);
        participantEditor.updateTravelTime(participant, preparationInfoDto);
    }

    @Transactional(readOnly = true)
    public LateComersDto getLateComers(final Long promiseId) {
        Promise promise = promiseRetriever.findById(promiseId);
        
        if (promise.getTime().isAfter(LocalDateTime.now())) {
            return LateComersDto.of(promise, Collections.emptyList());
        }

        List<LateComerDto> lateComers = participantRetriever.findAllLateComersByPromiseId(promiseId);
        return LateComersDto.of(
                promise,
                lateComers.stream()
                        .map(lateComer -> LateComerDto.of(
                                lateComer.participantId(),
                                lateComer.name(),
                                lateComer.profileImg())
                        )
                        .collect(Collectors.toList())
        );
    }

    @Transactional
    public void leavePromise(
            final Long userId,
            final Long promiseId
    ) {
        Participant participant = participantRetriever.findByPromiseIdAndUserId(promiseId, userId);
        participantRemover.deleteById(participant.getId());

        List<Participant> remainingParticipants = participantRetriever.findAllByPromiseId(promiseId);
        if(remainingParticipants.isEmpty()) {
            promiseRemover.deleteById(promiseId);
        }
    }

    private boolean validateState(
            final Participant participant,
            final String status
    ) {
        switch (status) {
            case "preparation":
                return isNull(participant.getPreparationStartAt())
                        && isNull(participant.getDepartureAt())
                        && isNull(participant.getArrivalAt());
            case "departure":
                return isNotNull(participant.getPreparationStartAt())
                        && isNull(participant.getDepartureAt())
                        && isNull(participant.getArrivalAt());
            case "arrival":
                return isNotNull(participant.getPreparationStartAt())
                        && isNotNull(participant.getDepartureAt())
                        && isNull(participant.getArrivalAt());
            default:
                throw new IllegalArgumentException("Unknown status");
        }
    }

    private boolean isNull(final LocalDateTime time) {
        return time == null;
    }

    private boolean isNotNull(final LocalDateTime time) {
        return time != null;
    }
}
