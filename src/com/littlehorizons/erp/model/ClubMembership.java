package com.littlehorizons.erp.model;

import com.littlehorizons.erp.model.enums.MembershipStatus;
import java.time.LocalDate;

/** Membership of a student in a club. Table: membresias_club. */
public record ClubMembership(
        Integer id,
        int studentId,
        String studentName,
        int clubId,
        String clubName,
        LocalDate joinDate,
        MembershipStatus status,
        LocalDate withdrawalDate,
        String withdrawalReason) {
}
