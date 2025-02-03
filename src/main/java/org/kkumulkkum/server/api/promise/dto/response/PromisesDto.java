package org.kkumulkkum.server.api.promise.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.kkumulkkum.server.domain.promise.Promise;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

public record PromisesDto(
        List<PromiseDto> promises
) {
    public static PromisesDto of(List<Promise> promises) {
        return new PromisesDto(
                promises.stream()
                        .map(PromisesDto.PromiseDto::from)
                        .toList()
        );
    }

    public record PromiseDto(
            Long promiseId,
            String name,
            int dDay,
            @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
            LocalDateTime time,
            String placeName
    ) {
        public static PromiseDto from(Promise promise) {
            int dday = (int) ChronoUnit.DAYS.between(promise.getTime().toLocalDate(), LocalDateTime.now().toLocalDate());
            return new PromiseDto(
                    promise.getId(),
                    promise.getName(),
                    dday,
                    promise.getTime(),
                    promise.getPlaceName()
            );
        }
    }
}
