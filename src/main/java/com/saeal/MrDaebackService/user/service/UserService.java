package com.saeal.MrDaebackService.user.service;

import com.saeal.MrDaebackService.user.domain.User;
import com.saeal.MrDaebackService.user.domain.UserCard;
import com.saeal.MrDaebackService.user.dto.request.AddCardRequest;
import com.saeal.MrDaebackService.user.dto.request.RegisterDto;
import com.saeal.MrDaebackService.user.dto.response.UserCardResponseDto;
import com.saeal.MrDaebackService.user.dto.response.UserResponseDto;
import com.saeal.MrDaebackService.user.enums.Authority;
import com.saeal.MrDaebackService.user.enums.LoyaltyLevel;
import com.saeal.MrDaebackService.user.repository.UserCardRepository;
import com.saeal.MrDaebackService.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.saeal.MrDaebackService.security.JwtUserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserCardRepository userCardRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponseDto register(RegisterDto registerDto) {
        if (userRepository.existsByEmailIgnoreCase(registerDto.getEmail())) {
            throw new IllegalArgumentException("Email already exist");
        }
        if (userRepository.existsByUsernameIgnoreCase(registerDto.getUsername())) {
            throw new IllegalArgumentException("Username already exist");
        }

        List<String> addresses = new ArrayList<>();
        if (registerDto.getAddress() != null && !registerDto.getAddress().isBlank()) {
            addresses.add(registerDto.getAddress());
        }

        User user = User.builder()
                .email(registerDto.getEmail())
                .username(registerDto.getUsername())
                .password(passwordEncoder.encode(registerDto.getPassword()))
                .displayName(registerDto.getDisplayName())
                .phoneNumber(registerDto.getPhoneNumber())
                .addresses(addresses)
                .authority(Authority.ROLE_USER)
                .loyaltyLevel(LoyaltyLevel.BRONZE)
                .visitCount(0L)
                .totalSpent(BigDecimal.ZERO)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        userRepository.save(user);

        return UserResponseDto.from(user);
    }

    @Transactional
    public UserResponseDto makeAdmin(String username) {
        User user = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        user.setAuthority(Authority.ROLE_ADMIN);
        user.setUpdatedAt(LocalDateTime.now());

        return UserResponseDto.from(user);
    }

    public UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new IllegalStateException("No authenticated user found");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof JwtUserDetails jwtUserDetails) {
            return jwtUserDetails.getId();
        }
        throw new IllegalStateException("Unsupported principal type: " + principal.getClass().getName());
    }

    @Transactional
    public List<String> getCurrentUserAddresses() {
        UUID userId = getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        return user.getAddresses() == null
                ? Collections.emptyList()
                : new ArrayList<>(user.getAddresses());
    }

    @Transactional
    public List<String> addAddressToCurrentUser(String address) {
        if (address == null || address.isBlank()) {
            throw new IllegalArgumentException("Address must not be blank");
        }

        UUID userId = getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        List<String> addresses = user.getAddresses();
        if (addresses == null) {
            addresses = new ArrayList<>();
            user.setAddresses(addresses);
        }
        addresses.add(address);

        return new ArrayList<>(addresses);
    }

    @Transactional
    public UserCardResponseDto addCardForCurrentUser(AddCardRequest request) {
        UUID userId = getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        UserCard newCard = UserCard.builder()
                .user(user)
                .cardBrand(request.getCardBrand())
                .cardNumber(request.getCardNumber())
                .expiryMonth(request.getExpiryMonth())
                .expiryYear(request.getExpiryYear())
                .cardHolderName(request.getCardHolderName())
                .cvv(request.getCvv())
                .isDefault(request.getIsDefault() != null && request.getIsDefault())
                .build();

        // If new card is set as default, unset existing defaults.
        if (newCard.isDefault() && user.getUserCards() != null) {
            user.getUserCards().forEach(c -> c.setDefault(false));
        }

        UserCard saved = userCardRepository.save(newCard);

        if (user.getUserCards() == null) {
            user.setUserCards(new ArrayList<>());
        }
        user.getUserCards().add(saved);

        return UserCardResponseDto.from(saved);
    }

    @Transactional
    public List<UserCardResponseDto> getCardsByUserId(UUID userId) {
        List<UserCard> cards = userCardRepository.findByUserId(userId);
        return cards.stream()
                .map(UserCardResponseDto::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public List<UserCardResponseDto> getCardsForCurrentUser() {
        UUID userId = getCurrentUserId();
        return getCardsByUserId(userId);
    }
}
