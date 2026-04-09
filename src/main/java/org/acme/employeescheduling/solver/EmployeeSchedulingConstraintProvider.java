package org.acme.employeescheduling.solver;

import static ai.timefold.solver.core.api.score.stream.Joiners.equal;
import static ai.timefold.solver.core.api.score.stream.Joiners.lessThanOrEqual;
import static ai.timefold.solver.core.api.score.stream.Joiners.overlapping;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.IsoFields;
import java.util.function.Function;

import ai.timefold.solver.core.api.score.buildin.hardmediumsoftbigdecimal.HardMediumSoftBigDecimalScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintCollectors;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.api.score.stream.common.LoadBalance;

import org.acme.employeescheduling.domain.AvailabilityInterval;
import org.acme.employeescheduling.domain.Employee;
import org.acme.employeescheduling.domain.Shift;

public class EmployeeSchedulingConstraintProvider implements ConstraintProvider {

    private static int getMinuteOverlap(Shift shift1, Shift shift2) {
        // The overlap of two timeslot occurs in the range common to both timeslots.
        // Both timeslots are active after the higher of their two start times,
        // and before the lower of their two end times.
        LocalDateTime shift1Start = shift1.getStart();
        LocalDateTime shift1End = shift1.getEnd();
        LocalDateTime shift2Start = shift2.getStart();
        LocalDateTime shift2End = shift2.getEnd();
        return (int) Duration.between((shift1Start.isAfter(shift2Start)) ? shift1Start : shift2Start,
                (shift1End.isBefore(shift2End)) ? shift1End : shift2End).toMinutes();
    }

    @Override
    public Constraint[] defineConstraints(ConstraintFactory constraintFactory) {
        return new Constraint[] {
                // Hard constraints
                requiredSkill(constraintFactory),
                requiredGender(constraintFactory),
                noOverlappingShifts(constraintFactory),
                atLeast11HoursBetweenTwoShifts(constraintFactory),
                oneShiftPerDay(constraintFactory),
                unavailableEmployee(constraintFactory),
                employeeNotExceedMonthlyHours(constraintFactory),
                fullDayShift24HourBreak(constraintFactory),
                employeeNotExceedWeeklyHours(constraintFactory),
                // Medium constrains
                desiredDayForEmployee(constraintFactory),
                balanceEmployeeShiftAssignments(constraintFactory),
                maxThreeConsecutiveDays(constraintFactory),
                    

                // Soft constraints
                undesiredDayForEmployee(constraintFactory)
        };
    }

    Constraint requiredSkill(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Shift.class)
                .filter(shift -> !shift.getEmployee().getSkills().contains(shift.getRequiredSkill()))
                .penalize(HardMediumSoftBigDecimalScore.ONE_HARD)
                .asConstraint("Missing required skill");
    }

    Constraint requiredGender(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Shift.class)
                .filter(shift -> shift.getEmployee().getSkills().stream().noneMatch(shift.getGender()::contains))
                .penalize(HardMediumSoftBigDecimalScore.ONE_MEDIUM)
                .asConstraint("Missing required gender");
    }

    Constraint noOverlappingShifts(ConstraintFactory constraintFactory) {
        return constraintFactory.forEachUniquePair(Shift.class, equal(Shift::getEmployee),
                overlapping(Shift::getStart, Shift::getEnd))
                .penalize(HardMediumSoftBigDecimalScore.ONE_HARD,
                        EmployeeSchedulingConstraintProvider::getMinuteOverlap)
                .asConstraint("Overlapping shift");
    }

    Constraint atLeast11HoursBetweenTwoShifts(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Shift.class)
                .join(Shift.class, equal(Shift::getEmployee), lessThanOrEqual(Shift::getEnd, Shift::getStart))
                .filter((firstShift,
                        secondShift) -> Duration.between(firstShift.getEnd(), secondShift.getStart()).toHours() < 11)
                .penalize(HardMediumSoftBigDecimalScore.ONE_HARD,
                        (firstShift, secondShift) -> {
                            int breakLength = (int) Duration.between(firstShift.getEnd(), secondShift.getStart()).toMinutes();
                            return (11 * 60) - breakLength;
                        })
                .asConstraint("At least 11 hours between 2 shifts");
    }

    Constraint oneShiftPerDay(ConstraintFactory constraintFactory) {
        return constraintFactory.forEachUniquePair(Shift.class, equal(Shift::getEmployee),
                equal(shift -> shift.getStart().toLocalDate()))
                .penalize(HardMediumSoftBigDecimalScore.ONE_HARD)
                .asConstraint("Max one shift per day");
    }

    Constraint unavailableEmployee(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Shift.class)
            .join(Employee.class, equal(Shift::getEmployee, Function.identity()))
            .flattenLast(Employee::getUnavailableIntervals) // Flatten the collection of intervals
            .filter((shift, interval) -> shift.getStart().isBefore(interval.getEnd()) && shift.getEnd().isAfter(interval.getStart()))
            .penalize(HardMediumSoftBigDecimalScore.ONE_HARD,
                    this::calculateOverlapMinutes)
            .asConstraint("Employee unavailable during shift");
    }

    Constraint undesiredDayForEmployee(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Shift.class)
            .join(Employee.class, equal(Shift::getEmployee, Function.identity()))
            .flattenLast(Employee::getUndesiredIntervals) // Flatten the collection of intervals
            .filter((shift, interval) -> shift.getStart().isBefore(interval.getEnd()) && shift.getEnd().isAfter(interval.getStart()))
            .penalize(HardMediumSoftBigDecimalScore.ONE_SOFT,
                    this::calculateOverlapMinutes)
            .asConstraint("Undesired time for employee");
    }

    Constraint desiredDayForEmployee(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Shift.class)
            .join(Employee.class, equal(Shift::getEmployee, Function.identity()))
            .flattenLast(Employee::getDesiredIntervals) // Flatten the collection of intervals
            .filter((shift, interval) -> shift.getStart().isBefore(interval.getEnd()) && shift.getEnd().isAfter(interval.getStart()))
            .reward(HardMediumSoftBigDecimalScore.ONE_MEDIUM,
                    (shift, interval) -> Math.max(0, calculateOverlapMinutes(shift, interval)))
            .asConstraint("Desired time for employee");
    }

    Constraint balanceEmployeeShiftAssignments(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Shift.class)
                .groupBy(Shift::getEmployee, ConstraintCollectors.count())
                .complement(Employee.class, e -> 0) // Include all employees which are not assigned to any shift.c
                .groupBy(ConstraintCollectors.loadBalance((employee, shiftCount) -> employee,
                        (employee, shiftCount) -> shiftCount))
                .penalizeBigDecimal(HardMediumSoftBigDecimalScore.ONE_MEDIUM, LoadBalance::unfairness)
                .asConstraint("Balance employee shift assignments");
    }
    

    Constraint employeeNotExceedMonthlyHours(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Shift.class)
            .groupBy(Shift::getEmployee, 
                     ConstraintCollectors.sumBigDecimal(this::calculateShiftHours))
            .filter((employee, totalHours) -> totalHours.compareTo(BigDecimal.valueOf(employee.getMonthlyHours())) > 0)
            .penalizeBigDecimal(HardMediumSoftBigDecimalScore.ONE_HARD,
                (employee, totalHours) -> totalHours.subtract(BigDecimal.valueOf(employee.getMonthlyHours())))
            .asConstraint("Employee over monthly hours");
    }/*
    Constraint enforceMinimumWorkHours(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Shift.class)
                .groupBy(Shift::getEmployee, 
                         ConstraintCollectors.sumBigDecimal(this::calculateShiftHours)) // Sum shift duration per employee
                .filter((employee, totalMinutes) -> totalMinutes.compareTo(BigDecimal.valueOf(7200L)) < 0) // Filter employees working less than 120 hours
                .penalizeBigDecimal(HardMediumSoftBigDecimalScore.ofHard(BigDecimal.valueOf(1000)),
                		(employee, totalMinutes) -> BigDecimal.valueOf(7200L).subtract(totalMinutes)) // Penalize the deficit
                .asConstraint("Employee must work at least 120 hours per month");
    }*/
    Constraint fullDayShift24HourBreak(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Shift.class)
            .filter(Shift::getIsFullDay)  // Select only full-day shifts
            .join(Shift.class, equal(Shift::getEmployee))  // Join on the same employee
            .filter((firstShift, secondShift) -> 
                firstShift.getEnd().isBefore(secondShift.getStart()) &&  // Ensure the second shift starts after the first
                Duration.between(firstShift.getEnd(), secondShift.getStart()).toHours() < 24)  // Check for 24-hour gap
            .penalize(HardMediumSoftBigDecimalScore.ONE_HARD,
                    (firstShift, secondShift) -> {
                        int breakLength = (int) Duration.between(firstShift.getEnd(), secondShift.getStart()).toMinutes();
                        return (24 * 60) - breakLength;  // Penalize based on the missing break time
                    })
            .asConstraint("Full day shift requires 24-hour break");
    }

    BigDecimal calculateShiftHours(Shift shift) {
        Duration duration = Duration.between(shift.getStart(), shift.getEnd());
        return BigDecimal.valueOf(duration.toHours());
    }
    
    private int calculateOverlapMinutes(Shift shift, AvailabilityInterval interval) {
        LocalDateTime overlapStart = shift.getStart().isAfter(interval.getStart()) ? shift.getStart() : interval.getStart();
        LocalDateTime overlapEnd = shift.getEnd().isBefore(interval.getEnd()) ? shift.getEnd() : interval.getEnd();
        return (int) Duration.between(overlapStart, overlapEnd).toMinutes();
    }
    
    Constraint employeeNotExceedWeeklyHours(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Shift.class)
            .groupBy(
                Shift::getEmployee,
                shift -> shift.getStart().get(IsoFields.WEEK_BASED_YEAR),
                shift -> shift.getStart().get(IsoFields.WEEK_OF_WEEK_BASED_YEAR),
                ConstraintCollectors.sumBigDecimal(this::calculateShiftHours)
            )
            .filter((employee, year, week, totalHours) -> 
                totalHours.compareTo(BigDecimal.valueOf(56)) > 0)
            .penalizeBigDecimal(
                HardMediumSoftBigDecimalScore.ONE_HARD,
                (employee, year, week, totalHours) -> 
                    totalHours.subtract(BigDecimal.valueOf(56))
            )
            .asConstraint("Employee over 56 weekly hours");
    }
    Constraint maxThreeConsecutiveDays(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Shift.class)
            .filter(shift -> "KLIENDITEENINDAJA".equals(shift.getRequiredSkill()))
            .join(Shift.class,
                equal(Shift::getEmployee),
                equal(shift -> shift.getStart().toLocalDate().plusDays(1), shift -> shift.getStart().toLocalDate())
            )
            .join(Shift.class,
                equal((s1, s2) -> s1.getEmployee(), Shift::getEmployee),
                equal((s1, s2) -> s2.getStart().toLocalDate().plusDays(1), shift -> shift.getStart().toLocalDate())
            )
            .join(Shift.class,
                equal((s1, s2, s3) -> s1.getEmployee(), Shift::getEmployee),
                equal((s1, s2, s3) -> s3.getStart().toLocalDate().plusDays(1), shift -> shift.getStart().toLocalDate())
            )
            .penalize(HardMediumSoftBigDecimalScore.ONE_MEDIUM)
            .asConstraint("Maksimaalselt 3 järjestikust tööpäeva");
    }

    
}
