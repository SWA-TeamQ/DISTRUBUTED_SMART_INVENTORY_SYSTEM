package com.auction.client.controllers;

import com.auction.client.core.ClientContext;
import com.auction.client.network.RmiClientProvider;
import com.auction.shared.interfaces.IAuctionService;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.rmi.RemoteException;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class DashboardFxmlSmokeTest {

    @BeforeAll
    static void initJavaFx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // JavaFX already started in this JVM.
        }
    }

    @AfterAll
    static void shutdownJavaFx() {
        // Intentionally left empty. JavaFX toolkit is shared across tests in this JVM.
    }

    @Test
    void shouldLoadUserDashboardFXML() throws Exception {
        prepareClientContext();
        assertDoesNotThrow(() -> loadFXML("/fxml/user_dashboard.fxml"));
    }

    @Test
    void shouldLoadAdminPanelFXML() throws Exception {
        prepareClientContext();
        assertDoesNotThrow(() -> loadFXML("/fxml/admin_panel.fxml"));
    }

    private void prepareClientContext() throws Exception {
        ClientContext context = ClientContext.getInstance();

        Field rmiProviderField = ClientContext.class.getDeclaredField("rmiProvider");
        rmiProviderField.setAccessible(true);
        rmiProviderField.set(context, createStubProvider());

        context.setUsername("admin");
        context.setSessionToken("test-token");
        context.setUserRole("ADMIN");
    }

    private RmiClientProvider createStubProvider() throws Exception {
        IAuctionService service = (IAuctionService) Proxy.newProxyInstance(
                IAuctionService.class.getClassLoader(),
                new Class<?>[] { IAuctionService.class },
                (proxy, method, args) -> {
                    return switch (method.getName()) {
                        case "getActiveAuctions", "getActiveAuctionsBySeller", "getMyBids", "getMyWonAuctions", "getAllUsers", "getAuditLogs" -> Collections.emptyList();
                        case "serverTime" -> "";
                        case "login" -> "test-token";
                        case "getSessionRole", "getMyRole" -> "ADMIN";
                        case "getAuctionById", "createAuction" -> null;
                        case "getThumbnail", "getFullImage", "backupDatabase", "exportAuctionsToCSV" -> new byte[0];
                        default -> null;
                    };
                }
        );

        RmiClientProvider provider = new RmiClientProvider();
        Field serviceField = RmiClientProvider.class.getDeclaredField("service");
        serviceField.setAccessible(true);
        serviceField.set(provider, service);
        return provider;
    }

    private Parent loadFXML(String resourcePath) throws java.io.IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(resourcePath));
        Parent root = loader.load();
        assertNotNull(root);
        return root;
    }
}