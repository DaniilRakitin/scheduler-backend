package org.acme.employeescheduling.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Set;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;

@PlanningEntity
public class Shift {
    @PlanningId
    private String id;

    private LocalDateTime start;
    private LocalDateTime end;

    private String location;
    private String requiredSkill;

    private Set<String> gender;

    private boolean isFullDay;
    @PlanningVariable
    private Employee employee;

    public Shift() {
    }

    public Shift(LocalDateTime start, LocalDateTime end, String location, String requiredSkill, Set<String> gender, boolean isFullDay) {
        this(start, end, location, requiredSkill, null, gender, isFullDay);
    }

    public Shift(LocalDateTime start, LocalDateTime end, String location, String requiredSkill, Employee employee, Set<String> gender, boolean isFullDay) {
        this(null, start, end, location, requiredSkill, employee, gender, isFullDay);
    }

    public Shift(String id, LocalDateTime start, LocalDateTime end, String location, String requiredSkill, Employee employee, Set<String> gender,  boolean isFullDay) {
        this.id = id;
        this.start = start;
        this.end = end;
        this.location = location;
        this.requiredSkill = requiredSkill;
        this.employee = employee;
        this.isFullDay = isFullDay;
        this.gender = gender;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Set<String> getGender() {
        return gender;
    }

    public void setGender(Set<String> gender) {
        this.gender = gender;
    }

    public LocalDateTime getStart() {
        return start;
    }

    public void setStart(LocalDateTime start) {
        this.start = start;
    }

    public LocalDateTime getEnd() {
        return end;
    }

    public void setEnd(LocalDateTime end) {
        this.end = end;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getRequiredSkill() {
        return requiredSkill;
    }

    public void setRequiredSkill(String requiredSkill) {
        this.requiredSkill = requiredSkill;
    }

    public Employee getEmployee() {
        return employee;
    }

    public void setEmployee(Employee employee) {
        this.employee = employee;
    }

    public boolean isOverlappingWithDate(LocalDate date) {
        return getStart().toLocalDate().equals(date) || getEnd().toLocalDate().equals(date);
    }

    public int getOverlappingDurationInMinutes(LocalDate date) {
        LocalDateTime startDateTime = LocalDateTime.of(date, LocalTime.MIN);
        LocalDateTime endDateTime = LocalDateTime.of(date, LocalTime.MAX);
        return getOverlappingDurationInMinutes(startDateTime, endDateTime, getStart(), getEnd());
    }

    private int getOverlappingDurationInMinutes(LocalDateTime firstStartDateTime, LocalDateTime firstEndDateTime,
            LocalDateTime secondStartDateTime, LocalDateTime secondEndDateTime) {
        LocalDateTime maxStartTime = firstStartDateTime.isAfter(secondStartDateTime) ? firstStartDateTime : secondStartDateTime;
        LocalDateTime minEndTime = firstEndDateTime.isBefore(secondEndDateTime) ? firstEndDateTime : secondEndDateTime;
        long minutes = maxStartTime.until(minEndTime, ChronoUnit.MINUTES);
        return minutes > 0 ? (int) minutes : 0;
    }
    
    public long getDurationInMinutes(LocalDateTime start, LocalDateTime end) {
        return ChronoUnit.MINUTES.between(start, end);
    }
    
    public boolean getIsFullDay() {
		return isFullDay;
	}
    
    public void setFullDay(boolean isFullDay) {
		this.isFullDay = isFullDay;
		System.out.println("isFullDay set to: " + this.isFullDay + " for Shift: " + this.toString());
}

    @Override
    public String toString() {
        return location + " " + start + "-" + end;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Shift shift)) {
            return false;
        }
        return Objects.equals(getId(), shift.getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }
}
