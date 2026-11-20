package com.nttdata.assessment.api.service;

/**
 * Score to knowledge-level mapping.
 */
public final class LevelRules {

    private LevelRules() {
    }

    /**
     * @param level level name
     * @return order (NOVATO=1 ... MASTER=5), unknown=-1
     */
    public static int order(String level) {
        return switch (level) {
            case "NOVATO" -> 1;
            case "PRINCIPIANTE_AVANZADO" -> 2;
            case "COMPETENTE" -> 3;
            case "PROFICIENTE" -> 4;
            case "MASTER" -> 5;
            default -> -1;
        };
    }

    /**
     * @param score 0..100
     * @return level
     */
    public static String fromScore(int score) {
        if (score <= 39) {
            return "NOVATO";
        }
        if (score <= 59) {
            return "PRINCIPIANTE_AVANZADO";
        }
        if (score <= 79) {
            return "COMPETENTE";
        }
        if (score <= 89) {
            return "PROFICIENTE";
        }
        return "MASTER";
    }
}
