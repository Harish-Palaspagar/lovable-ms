package com.harish.entity;

import com.harish.enums.MessageRole;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.List;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "chat_messages")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumns({
            @JoinColumn(name = "project_id", referencedColumnName = "projectId", nullable = false),
            @JoinColumn(name = "user_id", referencedColumnName = "userId", nullable = false)
    })
    ChatSession chatSession;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    MessageRole role;
    @OneToMany(mappedBy = "chatMessage", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("sequenceOrder ASC")
    List<ChatEvent> events;
    @Column(columnDefinition = "text")
    String content;
    Integer tokensUsed = 0;
    @CreationTimestamp
    Instant createdAt;

}
