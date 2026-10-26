package framework.session;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager {

    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    public Session getSession(HttpServletRequest request) {
        return getSession(request.getSession(true).getId());
    }

    public Session getSession(String clientId) {
        sessions.values().removeIf(session -> !session.isValid());
        return sessions.compute(clientId, (id, session) ->
                session != null ? session : new Session(id));
    }

    public void invalidate(String clientId) {
        Session session = sessions.remove(clientId);
        if (session != null) {
            session.invalidate();
        }
    }

    public int getSessionCount() {
        return sessions.size();
    }

    public Map<String, Session> getSessions() {
        return Map.copyOf(sessions);
    }
}
