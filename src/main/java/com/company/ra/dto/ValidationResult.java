package com.company.ra.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO representing validation result
 */
public class ValidationResult {

    private boolean valid;
    private List<String> errors = new ArrayList<>();

    public ValidationResult() {
    }

    public ValidationResult(boolean valid) {
        this.valid = valid;
    }

    public static ValidationResult success() {
        return new ValidationResult(true);
    }

    public static ValidationResult failure(String error) {
        ValidationResult result = new ValidationResult(false);
        result.addError(error);
        return result;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }

    public void addError(String error) {
        this.errors.add(error);
        this.valid = false;
    }

    @Override
    public String toString() {
        return "ValidationResult{" +
                "valid=" + valid +
                ", errors=" + errors +
                '}';
    }
}
