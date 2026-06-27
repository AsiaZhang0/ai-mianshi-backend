package org.example.questions.dao;

import org.example.questions.model.dto.QuestionEsRequest;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

public interface QuestionEsDao extends ElasticsearchRepository<QuestionEsRequest, Long> {

    List<QuestionEsRequest> findByUserId(Long userId);
}