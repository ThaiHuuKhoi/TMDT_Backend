package com.KhoiCG.TMDT.modules.chatbot.entity;

import com.KhoiCG.TMDT.modules.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
    name = "chat_sessions",
    indexes = @Index(name = "idx_chat_sessions_user_created", columnList = "user_id, created_at")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatSession {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    @Builder.Default
    private List<ChatMessage> messages = new ArrayList<>();

    public static ChatSession newAnonymous() {
        return ChatSession.builder()
                .id(UUID.randomUUID().toString())
                .build();
    }

    public void addMessage(ChatMessage message) {
        messages.add(message);
        message.setSession(this);
    }
}

