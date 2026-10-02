package com.neo.springapp.service;

import com.neo.springapp.model.User;
import com.neo.springapp.model.UserEmailHistory;
import com.neo.springapp.repository.UserEmailHistoryRepository;
import com.neo.springapp.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

/**
 * Reproduces the exact scenarios reported after the retired-email ledger fix:
 * 1) A user must be able to switch back to an email they previously changed away from.
 * 2) isEmailUnique(email, excludeUserId) must not block the owner from using their own
 *    current (unchanged) email.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceEmailHistoryTest {

    @Mock private UserRepository userRepository;
    @Mock private UserEmailHistoryRepository userEmailHistoryRepository;

    @InjectMocks private UserService userService;

    // In-memory fakes so save()/find() behave like a real table for this test.
    private final Map<String, User> usersByEmail = new HashMap<>();
    private final Map<String, UserEmailHistory> historyByEmail = new HashMap<>();

    @BeforeEach
    void setUp() {
        lenient().when(userRepository.findByEmailIgnoreCase(anyString())).thenAnswer(inv ->
                Optional.ofNullable(usersByEmail.get(((String) inv.getArgument(0)).toLowerCase())));
        lenient().when(userRepository.findById(any(Long.class))).thenAnswer(inv ->
                usersByEmail.values().stream().filter(u -> u.getId().equals(inv.getArgument(0))).findFirst());
        lenient().when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            usersByEmail.values().removeIf(existing -> existing.getId().equals(u.getId()));
            usersByEmail.put(u.getEmail().toLowerCase(), u);
            return u;
        });
        lenient().when(userEmailHistoryRepository.existsByEmailIgnoreCase(anyString())).thenAnswer(inv ->
                historyByEmail.containsKey(((String) inv.getArgument(0)).toLowerCase()));
        lenient().when(userEmailHistoryRepository.findByEmailIgnoreCase(anyString())).thenAnswer(inv ->
                Optional.ofNullable(historyByEmail.get(((String) inv.getArgument(0)).toLowerCase())));
        lenient().when(userEmailHistoryRepository.save(any(UserEmailHistory.class))).thenAnswer(inv -> {
            UserEmailHistory h = inv.getArgument(0);
            historyByEmail.put(h.getEmail().toLowerCase(), h);
            return h;
        });
    }

    private User newUser(Long id, String email) {
        User u = new User();
        u.setId(id);
        u.setEmail(email);
        return u;
    }

    @Test
    void ownerCanSwitchBackToTheirOwnPreviouslyRetiredEmail() {
        User alice = newUser(1L, "old@x.com");
        usersByEmail.put("old@x.com", alice);

        // Alice changes old@x.com -> new@x.com
        User updated = userService.updateUser(1L, newUser(1L, "new@x.com"));
        assertThat(updated.getEmail()).isEqualTo("new@x.com");

        // old@x.com is now retired, owned by Alice (id=1)
        assertThat(userEmailHistoryRepository.existsByEmailIgnoreCase("old@x.com")).isTrue();

        // Alice should be allowed to switch back to old@x.com
        boolean aliceCanReuse = userService.isEmailUnique("old@x.com", 1L);
        assertThat(aliceCanReuse).as("owner should be able to switch back to their own retired email").isTrue();

        // Actually perform the switch back and confirm it succeeds end-to-end
        User switchedBack = userService.updateUser(1L, newUser(1L, "old@x.com"));
        assertThat(switchedBack.getEmail()).isEqualTo("old@x.com");
    }

    @Test
    void someoneElseCannotClaimAnotherUsersRetiredEmail() {
        User alice = newUser(1L, "old@x.com");
        usersByEmail.put("old@x.com", alice);
        userService.updateUser(1L, newUser(1L, "new@x.com"));

        // Bob (id=2) should NOT be able to register/reuse old@x.com
        boolean bobCanUse = userService.isEmailUnique("old@x.com", 2L);
        assertThat(bobCanUse).isFalse();

        boolean newRegistrationBlocked = !userService.isEmailUnique("old@x.com");
        assertThat(newRegistrationBlocked).isTrue();
    }

    @Test
    void ownerEditingProfileWithUnchangedEmailIsNotBlocked() {
        User alice = newUser(1L, "alice@x.com");
        usersByEmail.put("alice@x.com", alice);

        // isEmailUnique(sameEmail, ownId) must be true since the live row belongs to the same user
        boolean result = userService.isEmailUnique("alice@x.com", 1L);
        assertThat(result).as("owner must not be blocked from keeping/using their own current email").isTrue();

        // Also verify via the actual update path: submitting the same email should not throw
        User resaved = userService.updateUser(1L, newUser(1L, "alice@x.com"));
        assertThat(resaved.getEmail()).isEqualTo("alice@x.com");
    }
}
