package com.app.models;

import java.sql.Timestamp;

/**
 * Represents a participant/member in a group order.
 * Mapped to the 'group_members' table.
 */
public class GroupMember {

    private int memberId;
    private int groupOrderId;
    private int userId;
    private Timestamp joinedAt;

    /**
     * Default constructor.
     */
    public GroupMember() {
    }

    /**
     * Parameterized constructor.
     *
     * @param memberId     unique identifier for the group membership
     * @param groupOrderId group order reference
     * @param userId       user identifier of the member
     * @param joinedAt     timestamp when user joined the group order
     */
    public GroupMember(int memberId, int groupOrderId, int userId, Timestamp joinedAt) {
        this.memberId = memberId;
        this.groupOrderId = groupOrderId;
        this.userId = userId;
        this.joinedAt = joinedAt;
    }

    public int getMemberId() {
        return memberId;
    }

    public void setMemberId(int memberId) {
        this.memberId = memberId;
    }

    public int getGroupOrderId() {
        return groupOrderId;
    }

    public void setGroupOrderId(int groupOrderId) {
        this.groupOrderId = groupOrderId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public Timestamp getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(Timestamp joinedAt) {
        this.joinedAt = joinedAt;
    }

    @Override
    public String toString() {
        return "GroupMember [memberId=" + memberId + ", groupOrderId=" + groupOrderId
                + ", userId=" + userId + ", joinedAt=" + joinedAt + "]";
    }
}
