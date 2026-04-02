package com.safechat.chatservice.utility;

public interface Enumeration {

    public interface Role{
         String CUSTOMER="customer";
         String SELLER="seller";
         String ADMIN="admin";

    }

    public interface MessageType{
        String NORMAL="normal";
        String PRIVACY="privacy";
    }

    public interface DeleteType{
        String ME="me";
        String EVERYONE="everyone";

        static boolean isValid(String value) {
            return ME.equalsIgnoreCase(value) || EVERYONE.equalsIgnoreCase(value);
        }
    }


    public interface SortDirection {
        String DESC="desc";
        String ASC="asc";

        static boolean isValid(String value) {
            return DESC.equalsIgnoreCase(value) || ASC.equalsIgnoreCase(value);
        }
        
    }


}