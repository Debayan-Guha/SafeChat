package com.safechat.chatservice.utility.api;

public interface ApiMessage {

    String CONVERSATION_DELETED = "Conversation deleted successfully";
    String CONVERSATION_FOUND = "Conversation found successfully";
    String CONVERSATION_NOT_FOUND="Conversation not found";
    String CONVERSATION_ALREADY_EXISTS="Conversation already exists";

    String MESSAGE_EDITED="Message edited successfully";
    String MESSAGE_FOUND="Messages found successfully";
    String MESSAGE_NOT_FOUND="Messages not found";
    String MESSAGE_DELETED = "Messages deleted successfully";

    String PAGE_VALIDATION_ERROR = "Page and Size must be greater than 0";
    String DATA_PROCESSING_FAILED = "Data Processing Failed";
    String DATABASE_CONNECTION_FAILED = "Database Connection Failed";
    String USER_SESSION_EXPIRED = "User Session Expired";
}
