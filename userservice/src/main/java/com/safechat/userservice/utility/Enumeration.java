package com.safechat.userservice.utility;

import java.util.stream.Stream;

public interface Enumeration {

    public interface Role {
        String USER = "user";

    }

    public interface Status {
        String ACTIVE = "active";
        String BLOCK = "block";
    }

    public interface OtpType {
        String ACCOUNT_CREATION = "ACCOUNT_CREATION";
        String ACCOUNT_DELETION_REQUEST = "ACCOUNT_DELETION_REQUEST";
        String PASSWORD_RESET = "PASSWORD_RESET";
        String EMAIL_UPDATE = "EMAIL_UPDATE";
        String ACCOUNT_DELETION_INSTANT = "ACCOUNT_DELETION_INSTANT";

        // Added validation method
        static boolean isValid(String value) {
            return Stream
                    .of(ACCOUNT_CREATION, ACCOUNT_DELETION_REQUEST, PASSWORD_RESET, EMAIL_UPDATE,
                            ACCOUNT_DELETION_INSTANT)
                    .anyMatch(type -> type.equals(value));
        }
    }

    public interface SortDirection {
        String DESC = "desc";
        String ASC = "asc";

        static boolean isValid(String value) {
            return DESC.equalsIgnoreCase(value) || ASC.equalsIgnoreCase(value);
        }

    }

}