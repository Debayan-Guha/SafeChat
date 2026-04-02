package com.safechat.chatservice.service.chatService;

import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import com.safechat.chatservice.document.ConversationDocument;
import com.safechat.chatservice.exception.ApplicationException.AlreadyExistsException;
import com.safechat.chatservice.exception.ApplicationException.NotFoundException;
import com.safechat.chatservice.jwt.JwtUtils;
import com.safechat.chatservice.service.dbService.ConversationDbService;
import com.safechat.chatservice.service.dbService.MessageDbService;
import com.safechat.chatservice.utility.Enumeration.DeleteType;
import com.safechat.chatservice.utility.OperationExecutor;
import com.safechat.chatservice.utility.encryption.AesEncryption;

@Service
public class ChatWriteService {

        private final String SERVICE_NAME = "ChatWriteService";

        private final ConversationDbService conversationDbService;
        private final MessageDbService messageDbService;
        private final AesEncryption aesEncryption;
        private final JwtUtils jwtUtils;

        public ChatWriteService(
                        ConversationDbService conversationDbService,
                        MessageDbService messageDbService,
                        AesEncryption aesEncryption,
                        JwtUtils jwtUtils) {

                this.conversationDbService = conversationDbService;
                this.messageDbService = messageDbService;
                this.aesEncryption = aesEncryption;
                this.jwtUtils = jwtUtils;
        }

        public void deleteConversation(String encryptToken, String conversationId, String deleteType)
                        throws NotFoundException, AlreadyExistsException {

                final String METHOD_NAME = "deleteConversation";

                String decryptToken = aesEncryption.decrypt(encryptToken);
                String userId = (String) jwtUtils.extractAllClaims(decryptToken).get("uid");

                Query query1 = Query.query(
                                Criteria.where("id").is(conversationId)
                                                .and("participants").in(userId));

                ConversationDocument conversation = OperationExecutor.dbGet(
                                () -> conversationDbService.getConversation(query1),
                                SERVICE_NAME,
                                METHOD_NAME)
                                .orElseThrow(() -> new NotFoundException(
                                                "Conversation not found for id: " + conversationId));

                // delete for everyone
                if (DeleteType.EVERYONE.equalsIgnoreCase(deleteType)) {
                        Query query2 = Query.query(Criteria.where("id").is(conversationId));
                        OperationExecutor.dbRemove(
                                        () -> conversationDbService.delete(query2),
                                        SERVICE_NAME,
                                        METHOD_NAME);

                        return;
                }

                // delete for me
                if (!conversation.getDeletedForUsers().contains(userId)) {
                        conversation.getDeletedForUsers().add(userId);
                        OperationExecutor.dbSave(
                                        () -> conversationDbService.save(conversation),
                                        SERVICE_NAME,
                                        METHOD_NAME);
                }

        }

}