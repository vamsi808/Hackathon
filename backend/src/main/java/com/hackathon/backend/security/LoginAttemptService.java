package com.hackathon.backend.security;

import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class LoginAttemptService {

    private final int MAX_ATTEMPT = 5;
    private final long LOCK_TIME_DURATION = TimeUnit.MINUTES.toMillis(15); 

    private ConcurrentHashMap<String, FailedAttempt> attemptsCache = new ConcurrentHashMap<>();

    private static class FailedAttempt {
        int attempts;
        long lastModified;

        FailedAttempt(int attempts, long lastModified) {
            this.attempts = attempts;
            this.lastModified = lastModified;
        }
    }

    public void loginSucceeded(String key) {
        attemptsCache.remove(key);
    }

    public void loginFailed(String key) {
        FailedAttempt attempt = attemptsCache.get(key);
        if (attempt == null) {
            attempt = new FailedAttempt(1, System.currentTimeMillis());
        } else {
            attempt.attempts++;
            attempt.lastModified = System.currentTimeMillis();
        }
        attemptsCache.put(key, attempt);
    }

    public boolean isBlocked(String key) {
        FailedAttempt attempt = attemptsCache.get(key);
        if (attempt != null) {
            if (System.currentTimeMillis() - attempt.lastModified > LOCK_TIME_DURATION) {
                // Unlock after 15 minutes of inactivity
                attemptsCache.remove(key);
                return false;
            }
            return attempt.attempts >= MAX_ATTEMPT;
        }
        return false;
    }
}
