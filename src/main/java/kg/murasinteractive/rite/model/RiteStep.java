package kg.murasinteractive.rite.model;

import jakarta.persistence.*;

/** Этап обряда (таблица rite_step). */
@Entity
@Table(name = "rite_step")
public class RiteStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rite_step_id")
    private Long riteStepId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rite_id", nullable = false)
    private Rite rite;

    @Column(name = "step_order", nullable = false)
    private int stepOrder;

    /** Короткое название этапа (может быть пустым). */
    @Column(name = "step_title", length = 200)
    private String stepTitle;

    @Column(name = "step_text", nullable = false, columnDefinition = "TEXT")
    private String stepText;

    public Long getRiteStepId() { return riteStepId; }
    public Rite getRite() { return rite; }
    public void setRite(Rite rite) { this.rite = rite; }
    public int getStepOrder() { return stepOrder; }
    public void setStepOrder(int stepOrder) { this.stepOrder = stepOrder; }
    public String getStepTitle() { return stepTitle; }
    public void setStepTitle(String stepTitle) { this.stepTitle = stepTitle; }
    public String getStepText() { return stepText; }
    public void setStepText(String stepText) { this.stepText = stepText; }
}
