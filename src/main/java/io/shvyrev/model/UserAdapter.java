package io.shvyrev.model;

import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.adapter.AbstractUserAdapterFederatedStorage;

public class UserAdapter extends AbstractUserAdapterFederatedStorage {

    private static final Logger log = Logger.getLogger( UserAdapter.class );
    private final KcUserEntity entity;

    //        INFO still not implemented
    private final String keycloakId;

    public UserAdapter(KeycloakSession session, RealmModel realm, ComponentModel model, KcUserEntity entity) {
        super(session, realm, model);

        this.entity = entity;
        keycloakId = StorageId.keycloakId(model, entity.getId().toString());
    }

    public String getPassword() {
        return entity.getPassword();
    }

    public void setPassword(String password) {
        log.info("$ "+ "setPassword() called with: password = [" + password + "]");

        entity.setPassword(password);
    }

    @Override
    public String getUsername() {
        return entity.getUsername();
    }

    @Override
    public void setUsername(String username) {
        log.info("$ "+ "setUsername() called with: username = [" + username + "]");

        entity.setUsername(username);
    }
}
