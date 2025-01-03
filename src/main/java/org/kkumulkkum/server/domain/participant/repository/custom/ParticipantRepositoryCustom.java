package org.kkumulkkum.server.domain.participant.repository.custom;

import org.kkumulkkum.server.api.participant.dto.projection.ParticipantStatusUserInfoProjection;

import java.util.List;

public interface ParticipantRepositoryCustom {
    List<ParticipantStatusUserInfoProjection> findAllByPromiseIdWithUserInfo(Long promiseId);
}
