package com.neo.springapp.service;

import com.neo.springapp.model.FastagLoginHistory;
import com.neo.springapp.repository.FastagLoginHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FastagLoginHistoryService {

    @Autowired
    private FastagLoginHistoryRepository repository;

    public void record(String gmailId, String status, String loginMethod, String failureReason,
                       String ipAddress, String deviceInfo) {
        try {
            FastagLoginHistory history = new FastagLoginHistory();
            history.setGmailId(gmailId == null ? "" : gmailId.trim().toLowerCase());
            history.setStatus(status);
            history.setLoginMethod(loginMethod);
            history.setFailureReason(failureReason);
            history.setIpAddress(ipAddress == null ? "Unknown" : ipAddress);
            history.setDeviceInfo(deviceInfo == null ? "Unknown" : deviceInfo);
            repository.save(history);
        } catch (Exception ignored) {
            // A history failure must never prevent a user from logging in.
        }
    }

    public List<FastagLoginHistory> getAll() {
        return repository.findAllByOrderByLoginTimeDesc();
    }

    public List<FastagLoginHistory> getByGmail(String gmailId) {
        return repository.findByGmailIdOrderByLoginTimeDesc(gmailId.trim().toLowerCase());
    }
}
