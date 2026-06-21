package com.fitness.aiservice.service;

import com.fitness.aiservice.model.Activity;
import com.fitness.aiservice.model.Recommendation;
import com.fitness.aiservice.repository.RecommendationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ActivityMessageListener {

    private final ActivityAIService aiService;
    private final RecommendationRepository recommendationRepository;

    @RabbitListener(queues = "${rabbitmq.queue.name}")
    public void processActivity(Activity activity) {
        log.info("Received activity for processing: {}", activity.getId());
        if (recommendationRepository.existsByActivityId(activity.getId())) {
            log.info("Recommendation already exists for activity {}. Skipping duplicate message.", activity.getId());
            return;
        }

        Recommendation recommendation = aiService.generateRecommendation(activity);
        try {
            recommendationRepository.save(recommendation);
        } catch (DuplicateKeyException duplicateKeyException) {
            log.info("Duplicate recommendation write detected for activity {}. Treating as already processed.", activity.getId());
        }
    }
}
