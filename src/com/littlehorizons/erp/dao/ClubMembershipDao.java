package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.ClubMembership;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Club memberships (membresias_club). */
public interface ClubMembershipDao {

    int insert(int studentId, int clubId, LocalDate joinDate);

    Optional<ClubMembership> findById(int id);

    List<ClubMembership> findByClub(int clubId, boolean activeOnly);

    List<ClubMembership> findByStudent(int studentId);

    long countActive(int clubId);

    boolean hasActive(int studentId, int clubId);

    boolean withdraw(int membershipId, LocalDate date, String reason);

    int withdrawAllOfStudent(int studentId, LocalDate date, String reason);
}
