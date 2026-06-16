package com.moviebooking.service;

import com.moviebooking.common.BusinessException;
import com.moviebooking.common.PageResult;
import com.moviebooking.entity.Notification;
import com.moviebooking.entity.enums.NotificationStatus;
import com.moviebooking.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Autowired
    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public PageResult<Map<String, Object>> getNotificationList(Long userId, int page, int size) {
        Page<Notification> notificationPage = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page - 1, size));

        var list = notificationPage.getContent().stream().map(n -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", n.getId());
            map.put("title", n.getTitle());
            map.put("content", n.getContent());
            map.put("type", n.getType().name());
            map.put("status", n.getStatus().name());
            map.put("createdAt", n.getCreatedAt());
            return map;
        }).toList();

        return new PageResult<>(list, notificationPage.getTotalElements(), page, size);
    }

    public void markAsRead(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> BusinessException.notFound("通知不存在"));

        if (!notification.getUserId().equals(userId)) {
            throw BusinessException.badRequest("无权操作此通知");
        }

        notification.setStatus(NotificationStatus.read);
        notificationRepository.save(notification);
    }
}
