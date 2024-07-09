package org.kkumulkkum.server.service.userInfo;

import lombok.RequiredArgsConstructor;
import org.kkumulkkum.server.domain.UserInfo;
import org.kkumulkkum.server.repository.UserInfoRepository;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserInfoSaver {

    private final UserInfoRepository userInfoRepository;

    public UserInfo save(final UserInfo user){
        return userInfoRepository.save(user);
    }

}