package upc.com.pe.backendplannia.assignment.domain.model.valueobjects;

public record AssignmentScoreWeights(
        float skillWeight,
        float experienceWeight,
        float interestWeight
) {
    public static final float SKILL_WEIGHT = 0.55f;
    public static final float EXPERIENCE_WEIGHT = 0.35f;
    public static final float INTEREST_WEIGHT = 0.10f;

    public static AssignmentScoreWeights defaults() {
        return new AssignmentScoreWeights(SKILL_WEIGHT, EXPERIENCE_WEIGHT, INTEREST_WEIGHT);
    }
}
