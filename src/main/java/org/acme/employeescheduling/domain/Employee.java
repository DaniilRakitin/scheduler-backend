package org.acme.employeescheduling.domain;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import ai.timefold.solver.core.api.domain.lookup.PlanningId;

public class Employee {
    @PlanningId
    private String name;
    private Set<String> skills;

    private Integer id;

    private Set<AvailabilityInterval> unavailableIntervals;
    private Set<AvailabilityInterval> vacationIntervals;
    private Set<AvailabilityInterval> undesiredIntervals;
    private Set<AvailabilityInterval> desiredIntervals;
    private int monthlyHours;
    public Employee() {

    }
    public Employee(int id, String name, Set<String> skills, Set<AvailabilityInterval> unavailableIntervals,
            Set<AvailabilityInterval> undesiredIntervals,Set<AvailabilityInterval> vacationIntervals, Set<AvailabilityInterval> desiredIntervals, int monthlyHours) {
		this.name = name;
		this.skills = skills;
		this.unavailableIntervals = unavailableIntervals;
		this.undesiredIntervals = undesiredIntervals;
		this.desiredIntervals = desiredIntervals;
		this.monthlyHours = monthlyHours;
        this.vacationIntervals = vacationIntervals;
        this.id = id;
	}

    public String getName() {
        return name;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<String> getSkills() {
        return skills;
    }

    public void setSkills(Set<String> skills) {
        this.skills = skills;
    }

    public Set<AvailabilityInterval> getVacationIntervals() {
        return vacationIntervals;
    }

    public void setVacationIntervals(Set<AvailabilityInterval> vacationIntervals) {
        this.vacationIntervals = vacationIntervals;
    }

    public Set<AvailabilityInterval> getUnavailableIntervals() {
        Set<AvailabilityInterval> combined = new HashSet<>(unavailableIntervals);
        combined.addAll(vacationIntervals);
        return combined;
	}

	public void setUnavailableIntervals(Set<AvailabilityInterval> unavailableIntervals) {
		this.unavailableIntervals = unavailableIntervals;
	}

	public Set<AvailabilityInterval> getUndesiredIntervals() {
		return undesiredIntervals;
	}

	public void setUndesiredIntervals(Set<AvailabilityInterval> undesiredIntervals) {
		this.undesiredIntervals = undesiredIntervals;
	}

	public Set<AvailabilityInterval> getDesiredIntervals() {
		return desiredIntervals;
	}

	public void setDesiredIntervals(Set<AvailabilityInterval> desiredIntervals) {
		this.desiredIntervals = desiredIntervals;
	}

	public int getMonthlyHours() {
		return monthlyHours;
	}

	public void setMonthlyHours(int monthlyHours) {
		this.monthlyHours = monthlyHours;
	}

	@Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Employee employee)) {
            return false;
        }
        return Objects.equals(getName(), employee.getName());
    }

    @Override
    public int hashCode() {
        return getName().hashCode();
    }
}
