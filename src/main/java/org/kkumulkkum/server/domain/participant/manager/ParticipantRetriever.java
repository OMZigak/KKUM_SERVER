package org.kkumulkkum.server.domain.participant.manager;

import lombok.RequiredArgsConstructor;
import org.kkumulkkum.server.api.participant.dto.projection.ParticipantStatusUserInfoProjection;
import org.kkumulkkum.server.domain.participant.Participant;
import org.kkumulkkum.server.api.participant.dto.response.LateComerDto;
import org.kkumulkkum.server.common.exception.ParticipantException;
import org.kkumulkkum.server.common.exception.code.ParticipantErrorCode;
import org.kkumulkkum.server.domain.participant.repository.ParticipantRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ParticipantRetriever {

    private final ParticipantRepository participantRepository;

    public Participant findByPromiseIdAndUserId(
            final Long promiseId,
            final Long userId
    ) {
        return participantRepository.findByPromiseIdAndUserId(promiseId, userId)
                .orElseThrow(() -> new ParticipantException(ParticipantErrorCode.NOT_JOINED_PROMISE));
    }

    public List<Participant> findAllByPromiseId(final Long promiseId) {
        return participantRepository.findAllByPromiseId(promiseId);
    }

    public List<ParticipantStatusUserInfoProjection> findAllByPromiseIdWithUserInfo(final Long promiseId) {
        return participantRepository.findAllByPromiseIdWithUserInfo(promiseId);
    }

    public List<LateComerDto> findAllLateComersByPromiseId(final Long promiseId) {
        return participantRepository.findAllLateComersByPromiseId(promiseId);
    }

    public boolean existsByPromiseIdAndUserId(
            final Long promiseId,
            final Long userId
    ) {
        return participantRepository.existsByPromiseIdAndUserId(promiseId, userId);
    }

    public int countFirstPreparationByPromiseId(final Long promiseId) {
        return participantRepository.countFirstPreparationByPromiseId(promiseId);
    }

    public int countFirstDepartureByPromiseId(final Long promiseId) {
        return participantRepository.countFirstDepartureByPromiseId(promiseId);
    }

    public int countFirstArrivalByPromiseId(final Long promiseId) {
        return participantRepository.countFirstArrivalByPromiseId(promiseId);
    }

    public List<String> findFcmTokenByPromiseId(final Long promiseId, final Long userId) {
        return participantRepository.findFcmTokenByPromiseId(promiseId, userId);
    }

    public Participant findByMemberIdAndPromiseId(final Long memberId, final Long promiseId) {
        return participantRepository.findByMemberIdAndPromiseId(memberId, promiseId);
    }
}
