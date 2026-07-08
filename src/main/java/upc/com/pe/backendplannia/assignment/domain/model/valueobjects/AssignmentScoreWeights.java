package upc.com.pe.backendplannia.assignment.domain.model.valueobjects;

public record AssignmentScoreWeights(
        float skillWeight,
        float experienceWeight,
        float interestWeight
) {
    // El skill es el requisito principal; la experiencia desempata pero no debe atropellarlo (S9): un
    // miembro con las skills equivocadas pero una tarea previa del dominio no debe ganarle al que tiene
    // las skills y es nuevo. Por eso experiencia pesa bastante menos que skill. Suma = 1.0.
    public static final float SKILL_WEIGHT = 0.65f;
    public static final float EXPERIENCE_WEIGHT = 0.20f;
    public static final float INTEREST_WEIGHT = 0.15f;

    public static AssignmentScoreWeights defaults() {
        return new AssignmentScoreWeights(SKILL_WEIGHT, EXPERIENCE_WEIGHT, INTEREST_WEIGHT);
    }
}
