package com.littlehorizons.erp.service;

/** Summary of a bulk charge generation. */
public record BulkChargeResult(int created, int skippedDuplicates, int skippedFullScholarship) {
}
