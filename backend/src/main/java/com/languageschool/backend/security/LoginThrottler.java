package com.languageschool.backend.security;

import com.languageschool.backend.error.ApiException;
import com.languageschool.backend.error.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LoginThrottler {

    private static final long WINDOW_SECONDS = 600;
    private static final int MAX_PER_IP = 50;
    private static final int MAX_PER_ID = 10;

    private final Map<String, Counter> ip = new ConcurrentHashMap<>();
    private final Map<String, Counter> id = new ConcurrentHashMap<>();

    public void assertAllowed(HttpServletRequest req, String loginOrEmail) {
        String ipKey = req.getRemoteAddr() == null ? "unknown" : req.getRemoteAddr();
        String idKey = loginOrEmail == null ? "" : loginOrEmail.trim().toLowerCase();
        if (!within(ip.compute(ipKey, LoginThrottler::bump), MAX_PER_IP)
                || !within(id.compute(idKey, LoginThrottler::bump), MAX_PER_ID)) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, ErrorCode.TOO_MANY_REQUESTS);
        }
    }

    private static Counter bump(String k, Counter c) {
        long now = Instant.now().getEpochSecond();
        if (c == null || now - c.windowStart >= WINDOW_SECONDS) return new Counter(now, 1);
        c.count += 1;
        return c;
    }

    private static boolean within(Counter c, int max) {
        return c != null && c.count <= max;
    }

    private static final class Counter {
        final long windowStart;
        int count;

        Counter(long s, int c) {
            this.windowStart = s;
            this.count = c;
        }
    }
}
