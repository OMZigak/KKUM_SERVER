package org.kkumulkkum.server.domain.member.repository;

import org.kkumulkkum.server.domain.member.Member;
import org.kkumulkkum.server.dto.member.response.MemberDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MemberRepository extends JpaRepository<Member, Long> {
    @Query("""
            SELECT CASE WHEN EXISTS 
            (SELECT m FROM Member m WHERE m.user.id = :userId AND m.meeting.id = :meetingId) 
            THEN TRUE ELSE FALSE END FROM Member m""")
    boolean existsByMeetingIdAndUserId(Long meetingId, Long userId);

    @Query("""
            SELECT new org.kkumulkkum.server.dto.member.response.MemberDto
            (m.id, ui.name, ui.profileImg)
            FROM Member m
            JOIN FETCH UserInfo ui ON m.user.id = ui.user.id
            WHERE m.meeting.id = :meetingId""")
    List<MemberDto> findAllByMeetingId(Long meetingId);

    @Query("""
            SELECT CASE WHEN EXISTS (
                SELECT m FROM Member m
                JOIN m.meeting mt
                JOIN m.user u
                JOIN Promise p ON p.meeting.id = mt.id
                WHERE p.id = :promiseId AND u.id = :userId
            ) THEN TRUE ELSE FALSE END
            FROM Member m""")
    boolean existsByPromiseIdAndUserId(Long promiseId, Long userId);

    Member findByMeetingIdAndUserId(Long meetingId, Long userId);

    @Query("""
            SELECT m from Member m
            JOIN FETCH m.meeting
            WHERE m.user.id = :userId
            """)
    List<Member> findByUserId(Long userId);

    @Query("""
            SELECT m FROM Member m
            JOIN m.meeting mt
            JOIN m.user u
            JOIN Promise p ON p.meeting.id = mt.id
            WHERE p.id = :promiseId AND u.id = :userId""")
    Member findByUserIdAndPromiseId(Long userId, Long promiseId);

    @Query("""
            SELECT new org.kkumulkkum.server.dto.member.response.MemberDto
            (m.id, ui.name, ui.profileImg)
            FROM Member m
            JOIN m.meeting mt
            JOIN FETCH UserInfo ui ON m.user.id = ui.user.id
            JOIN Promise p ON p.meeting.id = mt.id
            WHERE p.id = :promiseId""")
    List<MemberDto> findAllByPromiseId(Long promiseId);
}