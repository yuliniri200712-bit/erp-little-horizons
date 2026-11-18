package com.littlehorizons.erp.model.dto;

/** Summary of a bulk charge generation. */
public record BulkChargeResult(int created, int skippedDuplicates, int skippedFullScholarship) {
}
