package org.acme.employeescheduling.solver;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.Set;

import jakarta.inject.Inject;

import ai.timefold.solver.test.api.score.stream.ConstraintVerifier;

import org.acme.employeescheduling.domain.Employee;
import org.acme.employeescheduling.domain.EmployeeSchedule;
import org.acme.employeescheduling.domain.Shift;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class EmployeeSchedulingConstraintProviderTest {
    private static final LocalDate DAY_1 = LocalDate.of(2021, 2, 1);

    private static final LocalDateTime DAY_START_TIME = DAY_1.atTime(LocalTime.of(9, 0));
    private static final LocalDateTime DAY_END_TIME = DAY_1.atTime(LocalTime.of(17, 0));
    private static final LocalDateTime AFTERNOON_START_TIME = DAY_1.atTime(LocalTime.of(13, 0));
    private static final LocalDateTime AFTERNOON_END_TIME = DAY_1.atTime(LocalTime.of(21, 0));

    @Inject
    ConstraintVerifier<EmployeeSchedulingConstraintProvider, EmployeeSchedule> constraintVerifier;

    @Test
    void requiredSkill() {
        Employee employee = new Employee("1","Amy", Set.of(), null, null, null, null, 160);
        constraintVerifier.verifyThat(EmployeeSchedulingConstraintProvider::requiredSkill)
                .given(employee,
                        new Shift("1", DAY_START_TIME, DAY_END_TIME, "Location", "Skill", employee, Set.of("Male"), false))
                .penalizes(1);

        employee = new Employee("2", "Beth", Set.of("Skill"), null, null, null, null, 160);
        constraintVerifier.verifyThat(EmployeeSchedulingConstraintProvider::requiredSkill)
                .given(employee,
                        new Shift("2", DAY_START_TIME, DAY_END_TIME, "Location", "Skill", employee, Set.of("Male"), false))
                .penalizes(0);
    }

    @Test
    void overlappingShifts() {
        Employee employee1 = new Employee("3", "Amy", null, null, null, null, null, 160);
        Employee employee2 = new Employee("4", "Beth", null, null, null, null, null, 160);
        constraintVerifier.verifyThat(EmployeeSchedulingConstraintProvider::noOverlappingShifts)
                .given(employee1, employee2,
                        new Shift("1", DAY_START_TIME, DAY_END_TIME, "Location", "Skill", employee1, Set.of("Male"), false),
                        new Shift("2", DAY_START_TIME, DAY_END_TIME, "Location 2", "Skill", employee1, Set.of("Male"), false))
                .penalizesBy((int) Duration.ofHours(8).toMinutes());

        constraintVerifier.verifyThat(EmployeeSchedulingConstraintProvider::noOverlappingShifts)
                .given(employee1, employee2,
                        new Shift("1", DAY_START_TIME, DAY_END_TIME, "Location", "Skill", employee1, Set.of("Male"), false),
                        new Shift("2", DAY_START_TIME, DAY_END_TIME, "Location 2", "Skill", employee2, Set.of("Male"), false))
                .penalizes(0);

        constraintVerifier.verifyThat(EmployeeSchedulingConstraintProvider::noOverlappingShifts)
                .given(employee1, employee2,
                        new Shift("1", DAY_START_TIME, DAY_END_TIME, "Location", "Skill", employee1, Set.of("Male"), false),
                        new Shift("2", AFTERNOON_START_TIME, AFTERNOON_END_TIME, "Location 2", "Skill", employee1, Set.of("Male"), false))
                .penalizesBy((int) Duration.ofHours(4).toMinutes());
    }

    @Test
    void oneShiftPerDay() {
        Employee employee1 = new Employee("5", "Amy", null, null, null, null, null, 160);
        Employee employee2 = new Employee("6", "Beth", null, null, null, null, null, 160);
        constraintVerifier.verifyThat(EmployeeSchedulingConstraintProvider::noOverlappingShifts)
                .given(employee1, employee2,
                        new Shift("1", DAY_START_TIME, DAY_END_TIME, "Location", "Skill", employee1, Set.of("Male"), false),
                        new Shift("2", DAY_START_TIME, DAY_END_TIME, "Location 2", "Skill", employee1, Set.of("Male"), false))
                .penalizes(1);

        constraintVerifier.verifyThat(EmployeeSchedulingConstraintProvider::noOverlappingShifts)
                .given(employee1, employee2,
                        new Shift("1", DAY_START_TIME, DAY_END_TIME, "Location", "Skill", employee1, Set.of("Male"), false),
                        new Shift("2", DAY_START_TIME, DAY_END_TIME, "Location 2", "Skill", employee2, Set.of("Male"), false))
                .penalizes(0);

        constraintVerifier.verifyThat(EmployeeSchedulingConstraintProvider::noOverlappingShifts)
                .given(employee1, employee2,
                        new Shift("1", DAY_START_TIME, DAY_END_TIME, "Location", "Skill", employee1, Set.of("Male"), false),
                        new Shift("2", AFTERNOON_START_TIME, AFTERNOON_END_TIME, "Location 2", "Skill", employee1, Set.of("Male"), false))
                .penalizes(1);

        constraintVerifier.verifyThat(EmployeeSchedulingConstraintProvider::noOverlappingShifts)
                .given(employee1, employee2,
                        new Shift("1", DAY_START_TIME, DAY_END_TIME, "Location", "Skill", employee1, Set.of("Male"), false),
                        new Shift("2", DAY_START_TIME.plusDays(1), DAY_END_TIME.plusDays(1), "Location 2", "Skill", employee1, Set.of("Male"), false))
                .penalizes(0);
    }

    @Test
    void atLeast11HoursBetweenConsecutiveShifts() {
        Employee employee1 = new Employee("7", "Amy", null, null, null, null, null, 160);
        Employee employee2 = new Employee("8", "Beth", null, null, null, null, null, 160);
        constraintVerifier.verifyThat(EmployeeSchedulingConstraintProvider::atLeast11HoursBetweenTwoShifts)
                .given(employee1, employee2,
                        new Shift("1", DAY_START_TIME, DAY_END_TIME, "Location", "Skill", employee1, Set.of("Male"), false),
                        new Shift("2", AFTERNOON_END_TIME, DAY_START_TIME.plusDays(1), "Location 2", "Skill", employee1, Set.of("Male"), false))
                .penalizesBy(420);
        constraintVerifier.verifyThat(EmployeeSchedulingConstraintProvider::atLeast11HoursBetweenTwoShifts)
                .given(employee1, employee2,
                        new Shift("1", DAY_START_TIME, DAY_END_TIME, "Location", "Skill", employee1, Set.of("Male"), false),
                        new Shift("2", DAY_END_TIME, DAY_START_TIME.plusDays(1), "Location 2", "Skill", employee1, Set.of("Male"), false))
                .penalizesBy(660);
        constraintVerifier.verifyThat(EmployeeSchedulingConstraintProvider::atLeast11HoursBetweenTwoShifts)
                .given(employee1, employee2,
                        new Shift("1", DAY_END_TIME, DAY_START_TIME.plusDays(1), "Location", "Skill", employee1, Set.of("Male"), false),
                        new Shift("2", DAY_START_TIME, DAY_END_TIME, "Location 2", "Skill", employee1, Set.of("Male"), false))
                .penalizesBy(660);
        constraintVerifier.verifyThat(EmployeeSchedulingConstraintProvider::atLeast11HoursBetweenTwoShifts)
                .given(employee1, employee2,
                        new Shift("1", DAY_START_TIME, DAY_END_TIME, "Location", "Skill", employee1, Set.of("Male"), false),
                        new Shift("2", DAY_END_TIME.plusHours(11), DAY_START_TIME.plusDays(1), "Location 2",  "Skill",
                                employee1,Set.of("Male"), false))
                .penalizes(0);
        constraintVerifier.verifyThat(EmployeeSchedulingConstraintProvider::atLeast11HoursBetweenTwoShifts)
                .given(employee1, employee2,
                        new Shift("1", DAY_START_TIME, DAY_END_TIME, "Location", "Skill", employee1, Set.of("Male"), false),
                        new Shift("2", AFTERNOON_END_TIME, DAY_START_TIME.plusDays(1), "Location 2", "Skill", employee2, Set.of("Male"), false))
                .penalizes(0);
        constraintVerifier.verifyThat(EmployeeSchedulingConstraintProvider::noOverlappingShifts)
                .given(employee1, employee2,
                        new Shift("1", DAY_START_TIME, DAY_END_TIME, "Location", "Skill", employee1, Set.of("Male"), false),
                        new Shift("2", DAY_START_TIME.plusDays(1), DAY_END_TIME.plusDays(1), "Location 2", "Skill", employee1, Set.of("Male"), false))
                .penalizes(0);
    }






    @Test
    void balanceEmployeeShiftAssignments() {
        Employee employee1 = new Employee("9", "Amy", null, null, null, null, Collections.emptySet(), 160);
        Employee employee2 = new Employee("10","Beth", null, null, null, null, Collections.emptySet(), 160);
        // No employees have shifts assigned; the schedule is perfectly balanced.
        constraintVerifier.verifyThat(EmployeeSchedulingConstraintProvider::balanceEmployeeShiftAssignments)
                .given(employee1, employee2)
                .penalizesBy(0);
        // Only one employee has shifts assigned; the schedule is less balanced.
        constraintVerifier.verifyThat(EmployeeSchedulingConstraintProvider::balanceEmployeeShiftAssignments)
                .given(employee1, employee2,
                        new Shift("1", DAY_START_TIME.minusDays(1), DAY_END_TIME, "Location", "Skill", employee1, Set.of("Male"), false))
                .penalizesByMoreThan(0);
        // Every employee has a shift assigned; the schedule is once again perfectly balanced.
        constraintVerifier.verifyThat(EmployeeSchedulingConstraintProvider::balanceEmployeeShiftAssignments)
                .given(employee1, employee2,
                        new Shift("1", DAY_START_TIME.minusDays(1), DAY_END_TIME, "Location", "Skill", employee1, Set.of("Male"), false),
                        new Shift("2", DAY_START_TIME.minusDays(1), DAY_END_TIME, "Location", "Skill", employee2, Set.of("Male"), false))
                .penalizesBy(0);

    }
}
