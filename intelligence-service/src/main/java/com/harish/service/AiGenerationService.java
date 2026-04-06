package com.harish.service;

import com.harish.dto.chat.StreamResponse;
import reactor.core.publisher.Flux;

public interface AiGenerationService {

    Flux<StreamResponse> streamResponse(String message, Long projectId);

}
