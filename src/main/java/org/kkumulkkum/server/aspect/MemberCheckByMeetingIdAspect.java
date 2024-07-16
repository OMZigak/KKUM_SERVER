package org.kkumulkkum.server.aspect;

import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.kkumulkkum.server.annotation.IsMemberByMeetingId;
import org.kkumulkkum.server.exception.MemberException;
import org.kkumulkkum.server.exception.code.MemberErrorCode;
import org.kkumulkkum.server.service.member.MemberRetreiver;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class MemberCheckByMeetingIdAspect {

    private final MemberRetreiver memberRetreiver;

    @Before("@annotation(checkUserInMeeting)")
    public void checkUserInMeeting(JoinPoint joinPoint, IsMemberByMeetingId checkUserInMeeting) throws Throwable {
        Long meetingId = (Long) joinPoint.getArgs()[checkUserInMeeting.meetingIdParamIndex()];
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!memberRetreiver.existsByMeetingIdAndUserId(meetingId, userId)) {
            throw new MemberException(MemberErrorCode.NOT_JOINED_MEMBER);
        }
    }
}