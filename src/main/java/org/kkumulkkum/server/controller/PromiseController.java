package org.kkumulkkum.server.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.kkumulkkum.server.annotation.UserId;
import org.kkumulkkum.server.dto.promise.PromiseCreateDto;
import org.kkumulkkum.server.dto.promise.response.PromiseDto;
import org.kkumulkkum.server.dto.promise.response.PromisesDto;
import org.kkumulkkum.server.service.promise.PromiseService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PromiseController {

    private final PromiseService promiseService;

    @PostMapping("/meetings/{meetingId}/promises")
    public ResponseEntity<Void> createPromise(
            @UserId Long userId,
            @PathVariable Long meetingId,
            @Valid @RequestBody PromiseCreateDto createPromiseDto
    ) {
        Long promiseId = promiseService.createPromise(userId, meetingId, createPromiseDto);
        return ResponseEntity.created(URI.create(promiseId.toString())).build();
    }

    @PatchMapping("/promises/{promiseId}/completion")
    public ResponseEntity<Void> completePromise(
            @UserId Long userId,
            @PathVariable Long promiseId
    ) {
        promiseService.completePromise(userId, promiseId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/meetings/{meetingId}/promises")
    public ResponseEntity<PromisesDto> getPromises(
            @UserId final Long userId,
            @PathVariable("meetingId") final Long meetingId,
            @RequestParam(required = false) final Boolean done
    ) {
        return ResponseEntity.ok().body(promiseService.getPromises(userId, meetingId, done));
    }

    @GetMapping("/promises/{promiseId}")
    public ResponseEntity<PromiseDto> getPromise(
            @UserId final Long userId,
            @PathVariable("promiseId") final Long promiseId
    ) {
        return ResponseEntity.ok().body(promiseService.getPromise(userId, promiseId));
    }

}
