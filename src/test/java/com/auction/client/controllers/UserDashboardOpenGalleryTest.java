package com.auction.client.controllers;

import com.auction.client.core.ClientContext;
import com.auction.client.ui.ViewLoader;
import javafx.embed.swing.JFXPanel;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class UserDashboardOpenGalleryTest {

    @Test
    void openGalleryShouldInvokeGalleryView() throws Exception {
        new JFXPanel();

        UserDashboardController controller = new UserDashboardController();
        ClientContext context = ClientContext.getInstance();

        class FakeViewLoader extends ViewLoader {
            String loadedView;
            FakeViewLoader() { super(new Stage()); }
            @Override
            public <T> T loadView(String fxmlFile) throws IOException {
                loadedView = fxmlFile;
                return null;
            }
        }

        final FakeViewLoader[] fakeLoaderHolder = new FakeViewLoader[1];
        CountDownLatch latch = new CountDownLatch(1);
        javafx.application.Platform.runLater(() -> {
            fakeLoaderHolder[0] = new FakeViewLoader();
            latch.countDown();
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        FakeViewLoader fakeLoader = fakeLoaderHolder[0];
        context.setViewLoader(fakeLoader);
        context.setPreviousViewName(null);

        java.lang.reflect.Method method = UserDashboardController.class.getDeclaredMethod("handleOpenGallery");
        method.setAccessible(true);
        method.invoke(controller);

        assertEquals("gallery.fxml", fakeLoader.loadedView);
        assertEquals("user_dashboard.fxml", context.getPreviousViewName());
    }
}
