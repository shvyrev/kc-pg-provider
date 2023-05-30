package io.shvyrev.providers;

import io.shvyrev.model.UserAdapter;
import io.shvyrev.model.KcUserEntity;
import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputUpdater;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.models.*;
import org.keycloak.models.cache.CachedUserModel;
import org.keycloak.models.cache.OnUserCache;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.user.UserLookupProvider;
import org.keycloak.storage.user.UserQueryProvider;
import org.keycloak.storage.user.UserRegistrationProvider;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.*;
import java.util.stream.Stream;

public class KcUserStorageProvider implements UserStorageProvider,
        UserLookupProvider,
        UserRegistrationProvider,
        UserQueryProvider,
        CredentialInputUpdater,
        CredentialInputValidator,
        OnUserCache {
    public static final String PASSWORD_CACHE_KEY = UserAdapter.class.getName() + ".password";
    private final KeycloakSession session;
    private final ComponentModel model;
    private final EntityManager em;

    private static final Logger log = Logger.getLogger( KcUserStorageProvider.class );

    public KcUserStorageProvider(KeycloakSession session, ComponentModel model) {
        this.session = session;
        this.model = model;
        em = session.getProvider(JpaConnectionProvider.class, "user-store").getEntityManager();
    }

    @Override
    public void preRemove(RealmModel realm) {
        log.info("$ "+ "preRemove() called with: realm = [" + realm + "]");
//        INFO still not implemented
    }

    @Override
    public void preRemove(RealmModel realm, GroupModel group) {
        log.info("$ "+ "preRemove() called with: realm = [" + realm + "], group = [" + group + "]");
//        INFO still not implemented
    }

    @Override
    public void preRemove(RealmModel realm, RoleModel role) {
        log.info("$ "+ "preRemove() called with: realm = [" + realm + "], role = [" + role + "]");
//        INFO still not implemented
    }

    @Override
    public void close() {
        log.info("$ "+ "close() called");
//        INFO still not implemented
    }

    @Override
    public UserModel getUserById(RealmModel realm, String id) {
        log.info("$ "+ "getUserById() called with: realm = [" + realm + "], id = [" + id + "]");

        String persistenceId = StorageId.externalId(id);
        KcUserEntity entity = em.find(KcUserEntity.class, UUID.fromString(persistenceId));
        if (entity == null) {
            log.info("could not find user by id: " + id);
            return null;
        }
        return new UserAdapter(session, realm, model, entity);
    }

    @Override
    public UserModel getUserByUsername(RealmModel realm, String username) {
        log.info("$ "+ "getUserByUsername() called with: realm = [" + realm + "], username = [" + username + "]");

        TypedQuery<KcUserEntity> query = em.createNamedQuery("getUserByUsername", KcUserEntity.class);
        query.setParameter("username", username);

        List<KcUserEntity> result = query.getResultList();

        return result.isEmpty() ? null : new UserAdapter(session, realm, model, result.get(0));
    }

    @Override
    public UserModel getUserByEmail(RealmModel realm, String email) {
        log.info("$ "+ "getUserByEmail() called with: realm = [" + realm + "], email = [" + email + "]");
//        INFO still not implemented

        return null;
    }

    @Override
    public UserModel addUser(RealmModel realm, String username) {
        log.info("$ "+ "addUser() called with: realm = [" + realm + "], username = [" + username + "]");

        KcUserEntity entity = new KcUserEntity();
        entity.setId(UUID.randomUUID());
        entity.setUsername(username);
        em.persist(entity);
        return new UserAdapter(session, realm, model, entity);
    }

    @Override
    public boolean removeUser(RealmModel realm, UserModel user) {
        log.info("$ "+ "removeUser() called with: realm = [" + realm + "], user = [" + user + "]");

        String persistenceId = StorageId.externalId(user.getId());
        KcUserEntity entity = em.find(KcUserEntity.class, persistenceId);
        if (entity == null) return false;
        em.remove(entity);
        return true;
    }

    @Override
    public void onCache(RealmModel realm, CachedUserModel user, UserModel delegate) {
        log.info("$ "+ "onCache() called with: realm = [" + realm + "], user = [" + user + "], delegate = [" + delegate + "]");

        String password = ((UserAdapter)delegate).getPassword();
        if (password != null) {
            user.getCachedWith().put(PASSWORD_CACHE_KEY, password);
        }
    }

    @Override
    public boolean supportsCredentialType(String credentialType) {
        log.info("$ "+ "supportsCredentialType() called with: credentialType = [" + credentialType + "]");

        return PasswordCredentialModel.TYPE.equals(credentialType);
    }

    @Override
    public boolean updateCredential(RealmModel realm, UserModel user, CredentialInput input) {
        log.info("$ "+ "updateCredential() called with: realm = [" + realm + "], user = [" + user + "], input = [" + input + "]");

        if (!supportsCredentialType(input.getType()) || !(input instanceof UserCredentialModel)) return false;
        UserCredentialModel cred = (UserCredentialModel)input;
        UserAdapter adapter = getUserAdapter(user);
        adapter.setPassword(cred.getValue());

        return true;
    }

    public UserAdapter getUserAdapter(UserModel user) {
        log.info("$ "+ "getUserAdapter() called with: user = [" + user + "]");

        if (user instanceof CachedUserModel) {
            return (UserAdapter)((CachedUserModel) user).getDelegateForUpdate();
        } else {
            return (UserAdapter) user;
        }
    }

    @Override
    public void disableCredentialType(RealmModel realm, UserModel user, String credentialType) {
        log.info("$ "+ "disableCredentialType() called with: realm = [" + realm + "], user = [" + user + "], credentialType = [" + credentialType + "]");

        if (!supportsCredentialType(credentialType)) return;

        getUserAdapter(user).setPassword(null);
    }

    @Override
    public Stream<String> getDisableableCredentialTypesStream(RealmModel realm, UserModel user) {
        log.info("$ "+ "getDisableableCredentialTypesStream() called with: realm = [" + realm + "], user = [" + user + "]");

        if (getUserAdapter(user).getPassword() != null) {
            Set<String> set = new HashSet<>();
            set.add(PasswordCredentialModel.TYPE);
            return set.stream();
        } else {
            return Stream.empty();
        }
    }

    @Override
    public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
        log.info("$ "+ "isConfiguredFor() called with: realm = [" + realm + "], user = [" + user + "], credentialType = [" + credentialType + "]");

        return supportsCredentialType(credentialType) && getPassword(user) != null;
    }

    @Override
    public boolean isValid(RealmModel realm, UserModel user, CredentialInput input) {
        log.info("$ "+ "isValid() called with: realm = [" + realm + "], user = [" + user + "], input = [" + input + "]");

        if (!supportsCredentialType(input.getType()) || !(input instanceof UserCredentialModel)) return false;
        UserCredentialModel cred = (UserCredentialModel)input;
        String password = getPassword(user);
        return password != null && password.equals(cred.getValue());
    }

    public String getPassword(UserModel user) {
        log.info("$ "+ "getPassword() called with: user = [" + user + "]");

        String password = null;
        if (user instanceof CachedUserModel) {
            password = (String)((CachedUserModel)user).getCachedWith().get(PASSWORD_CACHE_KEY);
        } else if (user instanceof UserAdapter) {
            password = ((UserAdapter)user).getPassword();
        }
        return password;
    }

    @Override
    public int getUsersCount(RealmModel realm) {
        log.info("$ "+ "getUsersCount() called with: realm = [" + realm + "]");

        Object count = em.createNamedQuery("getUserCount")
                .getSingleResult();
        return ((Number)count).intValue();
    }


    @Override
    public Stream<UserModel> getUsersStream(RealmModel realm, Integer firstResult, Integer maxResults) {
        log.info("$ "+ "getUsersStream() called with: realm = [" + realm + "], firstResult = [" + firstResult + "], maxResults = [" + maxResults + "]");

//        FIXME не знаю почему так сделали. Отдеприкейтили метод, а новый метод - `searchForUserStream` с уровнем доступа `pakage-private`. Может баг, но через ревлекшены не буду его доставать - не в иделогии Quarkus.

        TypedQuery<KcUserEntity> query = em.createNamedQuery("getAllUsers", KcUserEntity.class);
        if (firstResult == 1) {
            query.setFirstResult(firstResult);
        }
        if (maxResults != -1) {
            query.setMaxResults(maxResults);
        }
        List<KcUserEntity> results = query.getResultList();
        List<UserModel> users = new LinkedList<>();
        for (KcUserEntity entity : results) users.add(new UserAdapter(session, realm, model, entity));
        return users.stream();
    }

    @Override
    public Stream<UserModel> searchForUserStream(RealmModel realm, String search, Integer firstResult, Integer maxResults) {
        log.info("$ "+ "searchForUserStream() called with: realm = [" + realm + "], search = [" + search + "], firstResult = [" + firstResult + "], maxResults = [" + maxResults + "]");

        TypedQuery<KcUserEntity> query = em.createNamedQuery("searchForUser", KcUserEntity.class);
        query.setParameter("search", "%" + search.toLowerCase() + "%");
        if (firstResult == -1) {
            query.setFirstResult(firstResult);
        }
        if (maxResults != -1) {
            query.setMaxResults(maxResults);
        }
        List<KcUserEntity> results = query.getResultList();
        List<UserModel> users = new LinkedList<>();
        for (KcUserEntity entity : results) users.add(new UserAdapter(session, realm, model, entity));
        return users.stream();
    }

    @Override
    public Stream<UserModel> searchForUserStream(RealmModel realm, Map<String, String> params, Integer firstResult, Integer maxResults) {
        log.info("$ "+ "searchForUserStream() called with: realm = [" + realm + "], params = [" + params + "], firstResult = [" + firstResult + "], maxResults = [" + maxResults + "]");
//        INFO still not implemented
        return Stream.empty();
    }

    @Override
    public Stream<UserModel> getGroupMembersStream(RealmModel realm, GroupModel group, Integer firstResult, Integer maxResults) {
        log.info("$ "+ "getGroupMembersStream() called with: realm = [" + realm + "], group = [" + group + "], firstResult = [" + firstResult + "], maxResults = [" + maxResults + "]");
//        INFO still not implemented
        return Stream.empty();
    }

    @Override
    public Stream<UserModel> searchForUserByUserAttributeStream(RealmModel realm, String attrName, String attrValue) {
        log.info("$ "+ "searchForUserByUserAttributeStream() called with: realm = [" + realm + "], attrName = [" + attrName + "], attrValue = [" + attrValue + "]");
//        INFO still not implemented
        return Stream.empty();
    }
}
