package com.auction.client.controllers;

import com.auction.client.core.ClientContext;
import com.auction.client.ui.ViewLoader;
import com.auction.shared.models.AuctionItem;
import javafx.embed.swing.JFXPanel;
import javafx.scene.control.TableView;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UserDashboardOpenAuctionDetailTest {

    @Test
    void openAuctionDetailShouldLoadSelectedAuction() throws Exception {
        new JFXPanel();

        UserDashboardController controller = new UserDashboardController();
        ClientContext context = ClientContext.getInstance();

        class FakeAuctionDetailController extends AuctionDetailController {
            int loadedAuctionId = -1;

            @Override
            public void loadAuction(int auctionId) {
                loadedAuctionId = auctionId;
            }
        }

        class FakeViewLoader extends ViewLoader {
            String loadedView;
            final FakeAuctionDetailController detailController = new FakeAuctionDetailController();

            FakeViewLoader() {
                super(new Stage());
            }

            @Override
            public <T> T loadView(String fxmlFile) throws IOException {
                loadedView = fxmlFile;
                detailController.initialize();
                @SuppressWarnings("unchecked")
                T controller = (T) detailController;
                return controller;
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

        TableView<AuctionItem> marketTable = new TableView<>();
        AuctionItem item = new AuctionItem(
            42,
            "Test Camera",
            "A sample item",
            "Collectibles",
            12999,
            "seller",
            Instant.now().toString(),
            Instant.now().plusSeconds(3600).toString(),
            null
        );
        marketTable.getItems().add(item);
        marketTable.getSelectionModel().select(item);

        java.lang.reflect.Field marketField = UserDashboardController.class.getDeclaredField("marketTable");
        marketField.setAccessible(true);
        marketField.set(controller, marketTable);

        java.lang.reflect.Field myListingsField = UserDashboardController.class.getDeclaredField("myListingsTable");
        myListingsField.setAccessible(true);
        myListingsField.set(controller, new TableView<AuctionItem>());

        java.lang.reflect.Field wonField = UserDashboardController.class.getDeclaredField("wonAuctionsTable");
        wonField.setAccessible(true);
        wonField.set(controller, new TableView<AuctionItem>());

        java.lang.reflect.Field statusField = UserDashboardController.class.getDeclaredField("statusLabel");
        statusField.setAccessible(true);
        statusField.set(controller, new javafx.scene.control.Label());

        java.lang.reflect.Method method = UserDashboardController.class.getDeclaredMethod("handleOpenAuctionDetail");
        method.setAccessible(true);
        method.invoke(controller);

        assertEquals("auction_detail.fxml", fakeLoader.loadedView);
        assertEquals(42, fakeLoader.detailController.loadedAuctionId);
        assertEquals("user_dashboard.fxml", context.getPreviousViewName());
        assertNotNull(context.getViewLoader());
    }
}
