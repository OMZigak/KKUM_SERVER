package org.kkumulkkum.server.domain.promise.repository.custom;

import org.kkumulkkum.server.domain.promise.Promise;

import java.util.List;

public interface PromiseRepositoryCustom {
    List<Promise> findPromiseByConditions(Long userId, Long meetingId, Boolean done, Boolean isParticipant);
}
