package org.kkumulkkum.server.domain.promise.repository.custom;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.kkumulkkum.server.domain.promise.Promise;
import org.springframework.stereotype.Repository;

import java.util.List;

import static org.kkumulkkum.server.domain.member.QMember.member;
import static org.kkumulkkum.server.domain.participant.QParticipant.participant;
import static org.kkumulkkum.server.domain.promise.QPromise.promise;

@Repository
@RequiredArgsConstructor
public class PromiseRepositoryCustomImpl implements PromiseRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Promise> findPromiseByConditions(Long userId, Long meetingId, Boolean done, Boolean isParticipant) {
        return queryFactory
                .selectFrom(promise)
                .leftJoin(promise.participants, participant).fetchJoin()
                .leftJoin(participant.member, member).fetchJoin()
                .where(
                        meetingIdEq(meetingId),
                        isParticipantEq(userId, meetingId, isParticipant),
                        doneEq(done)
                )
                .orderBy(promise.time.asc(), promise.createdAt.asc())
                .fetch();
    }

    private BooleanExpression meetingIdEq(Long meetingId) {
        return meetingId != null ? promise.meeting.id.eq(meetingId) : null;
    }

    private BooleanExpression isParticipantEq(Long userId, Long meetingId, Boolean isParticipant) {
        if (isParticipant == null) return null;

        if (isParticipant) {
            return promise.id.in(
                    JPAExpressions.select(participant.promise.id)
                            .from(participant)
                            .join(participant.member, member)
                            .where(member.user.id.eq(userId),
                                    participant.promise.meeting.id.eq(meetingId)
                            )
            );
        } else {
            return promise.id.notIn(
                    JPAExpressions.select(participant.promise.id)
                            .from(participant)
                            .join(participant.member, member)
                            .where(member.user.id.eq(userId),
                                    participant.promise.meeting.id.eq(meetingId)
                            )
            );
        }
    }

    private BooleanExpression doneEq(Boolean done) {
        return done != null ? promise.isCompleted.eq(done) : null;
    }

}
