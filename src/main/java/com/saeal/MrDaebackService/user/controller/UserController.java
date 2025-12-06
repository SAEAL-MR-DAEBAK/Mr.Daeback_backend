package com.saeal.MrDaebackService.user.controller;

import com.saeal.MrDaebackService.user.dto.request.RegisterDto;
import com.saeal.MrDaebackService.user.dto.request.AddAddressRequest;
import com.saeal.MrDaebackService.user.dto.request.AddCardRequest;
import com.saeal.MrDaebackService.user.dto.request.UpdateUserProfileRequest;
import com.saeal.MrDaebackService.user.dto.response.UserCardResponseDto;
import com.saeal.MrDaebackService.user.dto.response.UserResponseDto;
import com.saeal.MrDaebackService.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
@Tag(name = "User API", description = "User API 입니다.")
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<UserResponseDto> register(@Valid @RequestBody RegisterDto registerDto) {
        UserResponseDto result = userService.register(registerDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PatchMapping("/{username}/make-admin")
    public ResponseEntity<UserResponseDto> makeAdmin(@PathVariable String username) {
        UserResponseDto result = userService.makeAdmin(username);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/addresses")
    @Operation(summary = "현재 로그인한 사용자 주소 추가", description = "현재 인증된 사용자의 주소 리스트에 새 주소를 추가합니다.")
    public ResponseEntity<List<String>> addNewUserAddress(@Valid @RequestBody AddAddressRequest request) {
        List<String> addresses = userService.addAddressToCurrentUser(request.getAddress());
        return ResponseEntity.status(HttpStatus.CREATED).body(addresses);
    }

    @PostMapping("/cards")
    @Operation(summary = "카드 추가", description = "현재 로그인한 사용자의 카드 정보를 저장합니다. 카트 정보는 필요하지 않습니다.")
    public ResponseEntity<UserCardResponseDto> addUserCard(@Valid @RequestBody AddCardRequest request) {
        UserCardResponseDto response = userService.addCardForCurrentUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/cards")
    @Operation(summary = "현재 로그인한 사용자의 카드 목록 반환", description = "현재 인증된 사용자의 카드 리스트를 반환합니다.")
    public ResponseEntity<List<UserCardResponseDto>> getCurrentUserCards() {
        List<UserCardResponseDto> response = userService.getCardsForCurrentUser();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/addresses")
    @Operation(summary = "현재 로그인한 사용자의 주소 목록 반환", description = "현재 인증된 사용자의 주소 리스트를 반환합니다.")
    public ResponseEntity<List<String>> getUserAllAddress() {
        List<String> addresses = userService.getCurrentUserAddresses();
        return ResponseEntity.ok(addresses);
    }

    @DeleteMapping("/addresses")
    @Operation(summary = "주소 삭제", description = "현재 로그인한 사용자의 주소를 삭제합니다.")
    public ResponseEntity<List<String>> deleteUserAddress(@Valid @RequestBody AddAddressRequest request) {
        List<String> addresses = userService.deleteAddressFromCurrentUser(request.getAddress());
        return ResponseEntity.ok(addresses);
    }

    @DeleteMapping("/cards/{cardId}")
    @Operation(summary = "결제수단 삭제", description = "현재 로그인한 사용자의 결제수단을 삭제합니다.")
    public ResponseEntity<Void> deleteUserCard(@PathVariable UUID cardId) {
        userService.deleteCardForCurrentUser(cardId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/me")
    @Operation(summary = "회원정보 수정", description = "현재 로그인한 사용자의 회원정보를 수정합니다.")
    public ResponseEntity<UserResponseDto> updateCurrentUserProfile(@Valid @RequestBody UpdateUserProfileRequest request) {
        UserResponseDto response = userService.updateCurrentUserProfile(request);
        return ResponseEntity.ok(response);
    }

}
