package com.monitor.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Lectura defensiva de los parametros {@code Object...} que reciben los servicios. */
final class Params {

    private Params() {
    }

    static String requireString(Object[] params, int index, String name) {
        Object value = at(params, index);
        if (value == null || String.valueOf(value).isBlank()) {
            throw new IllegalArgumentException(
                    "Missing required parameter '" + name + "' at position " + index);
        }
        return String.valueOf(value).trim();
    }

    static int optInt(Object[] params, int index, int defaultValue) {
        Object value = at(params, index);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Parameter at position " + index + " must be an integer");
        }
    }

    static BigDecimal requireAmount(Object[] params, int index, String name) {
        Object value = at(params, index);
        if (value == null) {
            throw new IllegalArgumentException(
                    "Missing required parameter '" + name + "' at position " + index);
        }
        try {
            return new BigDecimal(String.valueOf(value).trim()).setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Parameter '" + name + "' must be a number");
        }
    }

    private static Object at(Object[] params, int index) {
        return params != null && index < params.length ? params[index] : null;
    }
}
