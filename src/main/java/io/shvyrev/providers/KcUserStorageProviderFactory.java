package io.shvyrev.providers;

import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.storage.UserStorageProviderFactory;

public class KcUserStorageProviderFactory implements UserStorageProviderFactory<KcUserStorageProvider> {

    public static final String PROVIDER_ID = "kc-pg-provider";

    private static final Logger log = Logger.getLogger( KcUserStorageProviderFactory.class );

    @Override
    public KcUserStorageProvider create(KeycloakSession session, ComponentModel model) {
        return new KcUserStorageProvider(session, model);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getHelpText() {
        return "Keycloak postgres provider";
    }

    @Override
    public void close() {
        log.info("$ "+ "close() called");
//        INFO still not implemented
    }
}
