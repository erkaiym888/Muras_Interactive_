package kg.murasinteractive.points.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Справочник правил начисления очков (Приложение Б.1 ТЗ).
 * Значения хранятся в базе, чтобы администратор мог менять их без изменения кода.
 * Для множителей value - коэффициент (0.5), для бонусов - число очков, для лимитов - количество.
 */
@Entity
@Table(name = "point_rule")
public class PointRule {

    @Id
    @Column(name = "rule_code", length = 50)
    private String ruleCode;

    @Column(name = "rule_value", nullable = false)
    private double ruleValue;

    @Column(name = "description", length = 300)
    private String description;

    public PointRule() {}

    public PointRule(String ruleCode, double ruleValue, String description) {
        this.ruleCode = ruleCode;
        this.ruleValue = ruleValue;
        this.description = description;
    }

    public String getRuleCode() { return ruleCode; }
    public double getRuleValue() { return ruleValue; }
    public void setRuleValue(double ruleValue) { this.ruleValue = ruleValue; }
    public String getDescription() { return description; }
}
