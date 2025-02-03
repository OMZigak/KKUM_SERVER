package org.kkumulkkum.server.domain.promise.manager;

import lombok.RequiredArgsConstructor;
import org.kkumulkkum.server.domain.promise.Promise;
import org.kkumulkkum.server.common.exception.PromiseException;
import org.kkumulkkum.server.common.exception.code.PromiseErrorCode;
import org.kkumulkkum.server.domain.promise.repository.PromiseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PromiseRetriever {

    private final PromiseRepository promiseRepository;

    public List<Promise> findAllByMeetingId(final Long meetingId) {
        return promiseRepository.findAllByMeetingIdOrderByTimeAscCreatedAtAsc(meetingId);
    }

    public Promise findById(final Long id) {
        return promiseRepository.findById(id)
                .orElseThrow(() -> new PromiseException(PromiseErrorCode.NOT_FOUND_PROMISE));
    }

    public List<Promise> findNextPromiseByUserId(
            final Long userId,
            final LocalDateTime startOfDay,
            final LocalDateTime startOfNextDay
    ) {
        return promiseRepository.findNextPromiseByUserId(
                userId,
                startOfDay,
                startOfNextDay,
                PageRequest.of(0,1)
        );

    }

    public List<Promise> findUpcomingPromisesExcludingNext(
            final Long userId,
            final Promise nextPromise,
            final int limit
    ) {
        Page<Promise> promisePage = promiseRepository.findUpcomingPromisesExcludingNext(
                userId,
                nextPromise.getId(),
                PageRequest.of(0, limit)
        );
        return promisePage.stream().collect(Collectors.toList());
    }

    public List<Promise> findUpcomingPromises(
            final Long userId,
            final int limit
    ) {
        Page<Promise> promisePage = promiseRepository.findUpcomingPromises(
                userId,
                PageRequest.of(0, limit)
        );
        return promisePage.stream().collect(Collectors.toList());
    }

    public boolean existsByArrivedAtIsNull(final Long promiseId) {
        return promiseRepository.existsByArrivedAtIsNull(promiseId);
    }

    public Promise findByUserIdAndPromiseId(final Long userId, final Long promiseId) {
        return promiseRepository.findByUserIdAndPromiseId(userId, promiseId);
    }

    public List<Promise> findPromisesByConditions(final Long userId, final Long meetingId, final Boolean done, final Boolean isParticipant) {
        return promiseRepository.findPromiseByConditions(userId, meetingId, done, isParticipant);
    }
}
