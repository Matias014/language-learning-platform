package com.languageschool.backend.service;

public interface ChatSessionCleanupService {

    /**
     * Usuwa całą sesję czatu wraz z powiązanymi danymi (wiadomości, logi LLM).
     *
     * @param sessionId        ID sesji do usunięcia
     * @param requesterLogin   login użytkownika wywołującego
     * @param requesterIsAdmin czy wywołujący ma rolę ADMIN
     */
    void deleteSessionCascade(Long sessionId, String requesterLogin, boolean requesterIsAdmin);
}
