package com.nttdata.assessment.api.domain.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nttdata.assessment.api.api.dto.CategoryResponse;
import com.nttdata.assessment.api.api.dto.ExperienceResponse;
import com.nttdata.assessment.api.api.dto.StrengthResponse;
import com.nttdata.assessment.api.api.dto.WeaknessResponse;
import com.nttdata.assessment.api.domain.model.AssessmentProfileEntity;
import com.nttdata.assessment.api.domain.model.QuestionEntity;
import com.nttdata.assessment.api.domain.model.QuizAttemptEntity;
import com.nttdata.assessment.api.domain.model.TopicEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * JDBC access to the SQLite assessment database.
 */
@Repository
public class AssessmentRepository {

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    /**
     * @param jdbc JDBC template
     * @param objectMapper JSON helper for attempt blobs
     */
    public AssessmentRepository(JdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
        ensureAttemptStore();
    }

    private void ensureAttemptStore() {
        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS runtime_attempt_store (
                  id TEXT PRIMARY KEY,
                  payload TEXT NOT NULL
                )
                """);
    }

    /**
     * @param id profile id
     * @return profile
     */
    public Optional<AssessmentProfileEntity> findProfile(String id) {
        List<AssessmentProfileEntity> rows = jdbc.query(
                """
                SELECT id, name, role_family, technology, candidate_name, evaluation_date,
                       baseline_seniority, target_knowledge
                FROM assessment_profiles WHERE id = ?
                """,
                (rs, i) -> new AssessmentProfileEntity(
                        rs.getString("id"),
                        rs.getString("name"),
                        rs.getString("role_family"),
                        rs.getString("technology"),
                        rs.getString("candidate_name"),
                        rs.getString("evaluation_date"),
                        rs.getString("baseline_seniority"),
                        rs.getString("target_knowledge")
                ),
                id
        );
        return rows.stream().findFirst();
    }

    /**
     * @param profileId profile
     * @return experiences
     */
    public List<ExperienceResponse> listExperiences(String profileId) {
        return jdbc.query(
                "SELECT id, name, knowledge_result FROM domain_experiences WHERE profile_id = ? ORDER BY sort_order",
                (rs, i) -> new ExperienceResponse(rs.getString("id"), rs.getString("name"), rs.getString("knowledge_result")),
                profileId
        );
    }

    /**
     * @param profileId profile
     * @return strengths
     */
    public List<StrengthResponse> listStrengths(String profileId) {
        return jdbc.query(
                "SELECT id, title, description FROM strengths WHERE profile_id = ? ORDER BY sort_order",
                (rs, i) -> new StrengthResponse(rs.getString("id"), rs.getString("title"), rs.getString("description")),
                profileId
        );
    }

    /**
     * @param profileId profile
     * @return weaknesses
     */
    public List<WeaknessResponse> listWeaknesses(String profileId) {
        // Load related topics after the outer ResultSet is closed — nested jdbc calls
        // inside a RowMapper deadlock Hikari when maximumPoolSize=1.
        List<WeaknessResponse> base = jdbc.query(
                "SELECT id, title, description FROM weaknesses WHERE profile_id = ? ORDER BY sort_order",
                (rs, i) -> new WeaknessResponse(
                        rs.getString("id"),
                        rs.getString("title"),
                        rs.getString("description"),
                        List.of()),
                profileId
        );
        List<WeaknessResponse> out = new ArrayList<>();
        for (WeaknessResponse w : base) {
            List<String> related = jdbc.query(
                    "SELECT topic_id FROM weakness_related_topics WHERE weakness_id = ?",
                    (r2, j) -> r2.getString("topic_id"),
                    w.id()
            );
            out.add(new WeaknessResponse(w.id(), w.title(), w.description(), related));
        }
        return out;
    }

    /**
     * @param profileId profile
     * @return categories
     */
    public List<CategoryResponse> listCategories(String profileId) {
        return jdbc.query(
                "SELECT id, profile_id, name FROM categories WHERE profile_id = ? ORDER BY sort_order",
                (rs, i) -> new CategoryResponse(rs.getString("id"), rs.getString("profile_id"), rs.getString("name")),
                profileId
        );
    }

    /**
     * @param profileId profile
     * @return topics
     */
    public List<TopicEntity> listTopics(String profileId) {
        List<TopicEntity> topics = jdbc.query(
                """
                SELECT id, profile_id, category_id, name, role_band, baseline_level, current_score,
                       current_level, priority_to_level, baseline_notes, generated_notes
                FROM topics WHERE profile_id = ? AND active = 1 ORDER BY sort_order
                """,
                (rs, i) -> {
                    Integer score = rs.getObject("current_score") == null ? null : rs.getInt("current_score");
                    return new TopicEntity(
                            rs.getString("id"),
                            rs.getString("profile_id"),
                            rs.getString("category_id"),
                            rs.getString("name"),
                            rs.getString("role_band"),
                            rs.getString("baseline_level"),
                            score,
                            rs.getString("current_level"),
                            rs.getInt("priority_to_level") == 1,
                            rs.getString("baseline_notes"),
                            rs.getString("generated_notes"),
                            Map.of()
                    );
                },
                profileId
        );
        List<TopicEntity> enriched = new ArrayList<>();
        for (TopicEntity topic : topics) {
            Map<String, String> rel = new LinkedHashMap<>();
            jdbc.query(
                    "SELECT knowledge_level, relevance FROM topic_relevances WHERE topic_id = ?",
                    rs -> {
                        rel.put(rs.getString("knowledge_level"), rs.getString("relevance"));
                    },
                    topic.id()
            );
            enriched.add(new TopicEntity(
                    topic.id(), topic.profileId(), topic.categoryId(), topic.name(), topic.roleBand(),
                    topic.baselineLevel(), topic.currentScore(), topic.currentLevel(), topic.priorityToLevel(),
                    topic.baselineNotes(), topic.generatedNotes(), rel
            ));
        }
        return enriched;
    }

    /**
     * @param topicId id
     * @return topic
     */
    public Optional<TopicEntity> findTopic(String topicId) {
        return listTopics("java-developer").stream().filter(t -> t.id().equals(topicId)).findFirst();
    }

    /**
     * @return active questions
     */
    public List<QuestionEntity> listActiveQuestions() {
        return listQuestions(null, null, null, null, true);
    }

    /**
     * Filtered question catalog (SQL-side filters to avoid loading the full bank).
     *
     * @param topicId optional topic
     * @param type optional type
     * @param depth optional knowledge depth
     * @param roleBand optional role band
     * @param activeOnly when true, only active rows
     * @return questions with bands/options/keywords
     */
    public List<QuestionEntity> listQuestions(
            String topicId,
            String type,
            String depth,
            String roleBand,
            boolean activeOnly
    ) {
        StringBuilder sql = new StringBuilder("""
                SELECT DISTINCT q.id, q.topic_id, q.type, q.prompt, q.knowledge_depth, q.active,
                       q.reference_answer, q.explanation
                FROM questions q
                """);
        List<Object> args = new ArrayList<>();
        if (roleBand != null && !roleBand.isBlank()) {
            sql.append(" INNER JOIN question_role_bands rb ON rb.question_id = q.id AND rb.role_band = ? ");
            args.add(roleBand);
        }
        sql.append(" WHERE 1=1 ");
        if (activeOnly) {
            sql.append(" AND q.active = 1 ");
        }
        if (topicId != null && !topicId.isBlank()) {
            sql.append(" AND q.topic_id = ? ");
            args.add(topicId);
        }
        if (type != null && !type.isBlank()) {
            sql.append(" AND q.type = ? ");
            args.add(type);
        }
        if (depth != null && !depth.isBlank()) {
            sql.append(" AND q.knowledge_depth = ? ");
            args.add(depth);
        }
        sql.append(" ORDER BY q.id ");

        List<QuestionEntity> base = jdbc.query(
                sql.toString(),
                (rs, i) -> new QuestionEntity(
                        rs.getString("id"),
                        rs.getString("topic_id"),
                        rs.getString("type"),
                        rs.getString("prompt"),
                        rs.getString("knowledge_depth"),
                        rs.getInt("active") == 1,
                        rs.getString("reference_answer"),
                        rs.getString("explanation"),
                        List.of(),
                        List.of(),
                        List.of()
                ),
                args.toArray()
        );
        return enrichQuestions(base);
    }

    private List<QuestionEntity> enrichQuestions(List<QuestionEntity> base) {
        List<QuestionEntity> out = new ArrayList<>(base.size());
        for (QuestionEntity q : base) {
            List<String> bands = jdbc.query(
                    "SELECT role_band FROM question_role_bands WHERE question_id = ?",
                    (rs, i) -> rs.getString("role_band"),
                    q.id()
            );
            List<QuestionEntity.OptionEntity> options = jdbc.query(
                    "SELECT id, label, is_correct FROM question_options WHERE question_id = ? ORDER BY sort_order",
                    (rs, i) -> new QuestionEntity.OptionEntity(
                            rs.getString("id"),
                            rs.getString("label"),
                            rs.getInt("is_correct") == 1
                    ),
                    q.id()
            );
            List<String> keywords = jdbc.query(
                    "SELECT keyword FROM question_keywords WHERE question_id = ? ORDER BY sort_order",
                    (rs, i) -> rs.getString("keyword"),
                    q.id()
            );
            out.add(new QuestionEntity(
                    q.id(), q.topicId(), q.type(), q.prompt(), q.knowledgeDepth(), q.active(),
                    q.referenceAnswer(), q.explanation(), bands, options, keywords
            ));
        }
        return out;
    }

    /**
     * @param id question id
     * @return question
     */
    public Optional<QuestionEntity> findQuestion(String id) {
        return listActiveQuestions().stream().filter(q -> q.id().equals(id)).findFirst();
    }

    /**
     * @param attempt attempt aggregate
     */
    public void saveAttempt(QuizAttemptEntity attempt) {
        try {
            String payload = objectMapper.writeValueAsString(attempt);
            jdbc.update(
                    """
                    INSERT INTO runtime_attempt_store(id, payload) VALUES(?, ?)
                    ON CONFLICT(id) DO UPDATE SET payload = excluded.payload
                    """,
                    attempt.id(),
                    payload
            );
        } catch (Exception ex) {
            throw new IllegalStateException("Could not persist attempt", ex);
        }
    }

    /**
     * @param id attempt id
     * @return attempt
     */
    public Optional<QuizAttemptEntity> findAttempt(String id) {
        List<String> payloads = jdbc.query(
                "SELECT payload FROM runtime_attempt_store WHERE id = ?",
                (rs, i) -> rs.getString("payload"),
                id
        );
        if (payloads.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(payloads.get(0), QuizAttemptEntity.class));
        } catch (Exception ex) {
            throw new IllegalStateException("Could not read attempt", ex);
        }
    }

    /**
     * @return all runtime attempts
     */
    public List<QuizAttemptEntity> listAttempts() {
        List<String> payloads = jdbc.query("SELECT payload FROM runtime_attempt_store", (rs, i) -> rs.getString("payload"));
        List<QuizAttemptEntity> out = new ArrayList<>();
        for (String payload : payloads) {
            try {
                out.add(objectMapper.readValue(payload, QuizAttemptEntity.class));
            } catch (Exception ex) {
                throw new IllegalStateException("Could not read attempt store", ex);
            }
        }
        return out;
    }

    /**
     * Updates topic progress fields after scoring.
     *
     * @param topicId topic
     * @param score score
     * @param level level
     * @param notes notes
     */
    public void updateTopicProgress(String topicId, int score, String level, String notes) {
        jdbc.update(
                "UPDATE topics SET current_score = ?, current_level = ?, generated_notes = ? WHERE id = ?",
                score,
                level,
                notes,
                topicId
        );
    }

    /**
     * @param value optional
     * @param id id
     * @param <T> type
     * @return value or 404
     */
    public <T> T require(Optional<T> value, String id) {
        return value.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found: " + id));
    }
}
