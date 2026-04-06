package com.harish.entity;

import lombok.*;

import java.io.Serializable;

@Builder
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class ChatSessionId implements Serializable {

    Long projectId;
    Long userId;

}
