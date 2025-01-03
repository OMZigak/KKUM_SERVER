package org.kkumulkkum.server.domain.participant.repository.custom;

import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.kkumulkkum.server.api.participant.dto.projection.ParticipantStatusUserInfoProjection;
import org.springframework.stereotype.Repository;

import java.util.List;

import static org.kkumulkkum.server.domain.participant.QParticipant.participant;
import static org.kkumulkkum.server.domain.member.QMember.member;
import static org.kkumulkkum.server.domain.userinfo.QUserInfo.userInfo;


@Repository
@RequiredArgsConstructor
public class ParticipantRepositoryCustomImpl implements ParticipantRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<ParticipantStatusUserInfoProjection> findAllByPromiseIdWithUserInfo(Long promiseId) {
        return queryFactory
                .select(
                        Projections.constructor(
                                ParticipantStatusUserInfoProjection.class,
                                participant.id,
                                member.id,
                                userInfo.name,
                                userInfo.profileImg,
                                participant.preparationStartAt,
                                participant.departureAt,
                                participant.arrivalAt,
                                stateExpression
                        )
                )
                .from(participant)
                .join(participant.member, member)
                .join(userInfo).on(member.user.id.eq(userInfo.user.id))
                .where(participant.promise.id.eq(promiseId))
                .orderBy(stateOrderExpression.asc())
                .fetch();
    }

    private static final Expression<String> stateExpression = new CaseBuilder()
            .when(participant.arrivalAt.isNotNull()).then("도착")
            .when(participant.departureAt.isNotNull()).then("이동중")
            .when(participant.preparationStartAt.isNotNull()).then("준비중")
            .otherwise("꾸물중");

    private static final NumberExpression<Integer> stateOrderExpression = new CaseBuilder()
            .when(participant.arrivalAt.isNotNull()).then(1)
            .when(participant.departureAt.isNotNull()).then(2)
            .when(participant.preparationStartAt.isNotNull()).then(3)
            .otherwise(4);
}
