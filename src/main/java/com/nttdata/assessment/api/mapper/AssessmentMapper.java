package com.nttdata.assessment.api.mapper;

import com.nttdata.assessment.api.api.dto.AttemptResponse;
import com.nttdata.assessment.api.api.dto.ProfileResponse;
import com.nttdata.assessment.api.api.dto.QuestionOptionResponse;
import com.nttdata.assessment.api.api.dto.QuestionResponse;
import com.nttdata.assessment.api.api.dto.TopicResponse;
import com.nttdata.assessment.api.domain.model.AssessmentProfileEntity;
import com.nttdata.assessment.api.domain.model.QuestionEntity;
import com.nttdata.assessment.api.domain.model.QuizAttemptEntity;
import com.nttdata.assessment.api.domain.model.TopicEntity;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * MapStruct mappers for assessment DTOs.
 */
@Mapper(componentModel = "spring")
public interface AssessmentMapper {

    /**
     * @param entity profile
     * @return dto
     */
    ProfileResponse toProfile(AssessmentProfileEntity entity);

    /**
     * @param entity topic
     * @return dto
     */
    TopicResponse toTopic(TopicEntity entity);

    /**
     * @param entity question
     * @return dto
     */
    default QuestionResponse toQuestion(QuestionEntity entity) {
        List<QuestionOptionResponse> options = entity.options() == null ? List.of() : entity.options().stream()
                .map(o -> new QuestionOptionResponse(o.id(), o.label(), o.correct()))
                .toList();
        return new QuestionResponse(
                entity.id(),
                entity.topicId(),
                entity.type(),
                entity.prompt(),
                entity.knowledgeDepth(),
                entity.roleBands(),
                entity.active(),
                options,
                entity.referenceAnswer(),
                entity.keywords(),
                entity.explanation()
        );
    }

    /**
     * @param entity attempt
     * @return dto
     */
    AttemptResponse toAttempt(QuizAttemptEntity entity);
}
