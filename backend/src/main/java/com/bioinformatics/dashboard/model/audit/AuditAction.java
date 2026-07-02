package com.bioinformatics.dashboard.model.audit;

import lombok.Getter;

@Getter
public enum AuditAction {
    LOGIN(AuditTarget.AUTH),
    LOGOUT(AuditTarget.AUTH),
    UPDATE_PASSWORD(AuditTarget.AUTH),
    TOKEN_REFRESH(AuditTarget.AUTH),
    FILTER_SAVE(AuditTarget.SAVED_FILTER),
    FILTER_LOAD(AuditTarget.SAVED_FILTER),
    FILTER_DELETE(AuditTarget.SAVED_FILTER),
    ADMIN_DELETE_USER_FILTER(AuditTarget.SAVED_FILTER),
    SEARCH_QUERY(AuditTarget.SEARCH),
    DETAIL_VIEW(AuditTarget.DETAIL),
    COMPARE_ANALYTICS(AuditTarget.COMPARE),
    DATA_EXPORT_CSV(AuditTarget.EXPORT_CSV),
    DATA_EXPORT_CHART(AuditTarget.EXPORT_CHART),
    IMPORT_UPLOAD(AuditTarget.IMPORT_JOB),
    IMPORT_CANCEL(AuditTarget.IMPORT_JOB),
    PASSWORD_CHANGE(AuditTarget.USER),
    PROFILE_UPDATE(AuditTarget.USER);

    private final AuditTarget defaultTarget;

    AuditAction(AuditTarget defaultTarget) {
        this.defaultTarget = defaultTarget;
    }

}
