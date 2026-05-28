package com.auction.client.controllers;

import com.auction.client.core.ClientContext;
import com.auction.shared.interfaces.IAuctionService;
import com.auction.shared.models.AuctionItem;
import javafx.embed.swing.JFXPanel;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.rmi.RemoteException;
import java.time.Instant;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

import static org.junit.jupiter.api.Assertions.*;

public class AuctionDetailLazyThumbTest {

    @Test
    void lazyThumbsAreNotLoadedUntilClicked() throws Exception {
        new JFXPanel(); // init JavaFX

        // fake service that records thumbnail calls
        AtomicInteger[] counts = new AtomicInteger[]{new AtomicInteger(0), new AtomicInteger(0), new AtomicInteger(0), new AtomicInteger(0)};

        IAuctionService fake = new IAuctionService() {
            @Override
            public String login(String username, String password) { return null; }
            @Override public void register(String username, String password, String role) {}
            @Override public String getMyRole(String token) { return null; }
            @Override public void logout(String token) {}
            @Override public String serverTime() { return Instant.now().toString(); }
            @Override public List<AuctionItem> getActiveAuctions() { return List.of(); }
            @Override public List<AuctionItem> getAllAuctions() { return List.of(); }
            @Override public List<AuctionItem> searchActiveAuctions(String query, String category, String sortBy) { return List.of(); }
            @Override public List<AuctionItem> getActiveAuctionsBySeller(String sellerUsername, String token) { return List.of(); }
            @Override public AuctionItem getAuctionById(int auctionId) {
                return new AuctionItem(auctionId, "Title", "Desc", "misc", 1000, "seller", Instant.now().toString(), Instant.now().plusSeconds(3600).toString(), null);
            }
            @Override public void placeBid(int auctionId, long amountCents, long clientExpectedPriceCents, String token) {}
            @Override public List getBidHistory(int auctionId) { return List.of(); }
            @Override public int createAuction(AuctionItem item, byte[] image1, byte[] image2, byte[] image3, String token) { return 0; }
            @Override public void cancelAuction(int auctionId, String token) {}
            @Override public void relistAuction(int auctionId, String newEndTimeIso, String token) {}
            @Override public void startAuction(int auctionId, String token) {}
            @Override public List getMyBids(String token) { return List.of(); }
            @Override public List getMyWonAuctions(String token) { return List.of(); }
            @Override
            public byte[] getThumbnail(int auctionId, int imageIndex) throws RemoteException {
                counts[imageIndex].incrementAndGet();
                // return the embedded placeholder image bytes so JavaFX can construct an image
                try (InputStream in = getClass().getResourceAsStream("/images/placeholder.png")) {
                    if (in == null) return new byte[0];
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    byte[] buf = new byte[4096];
                    int r;
                    while ((r = in.read(buf)) > 0) bos.write(buf, 0, r);
                    return bos.toByteArray();
                } catch (Exception e) {
                    return new byte[0];
                }
            }
            @Override public byte[] getFullImage(int auctionId, int imageIndex) { return new byte[0]; }
            @Override public byte[] exportAuctionsToCSV(String token) { return new byte[0]; }
            @Override public void createUser(String newUsername, String password, String role, String token) {}
            @Override public List getAllUsers(String token) { return List.of(); }
            @Override public byte[] backupDatabase(String token) { return new byte[0]; }
            @Override public List<String> getAuditLogs(int lastNLines, String token) { return List.of(); }
        };

        // inject fake into ClientContext.rmiProvider.service via reflection
        Object prov = ClientContext.getInstance().getRmiProvider();
        Field svc = prov.getClass().getDeclaredField("service");
        svc.setAccessible(true);
        svc.set(prov, fake);

        // instantiate controller without FXML to avoid FXML parsing issues
        AuctionDetailController ctrl = new AuctionDetailController();
        // set image view fields
        javafx.scene.image.ImageView hero = new javafx.scene.image.ImageView();
        javafx.scene.image.ImageView t1 = new javafx.scene.image.ImageView();
        javafx.scene.image.ImageView t2 = new javafx.scene.image.ImageView();
        javafx.scene.image.ImageView t3 = new javafx.scene.image.ImageView();
        Field fh = ctrl.getClass().getDeclaredField("heroImageView"); fh.setAccessible(true); fh.set(ctrl, hero);
        Field f1 = ctrl.getClass().getDeclaredField("thumb1View"); f1.setAccessible(true); f1.set(ctrl, t1);
        Field f2 = ctrl.getClass().getDeclaredField("thumb2View"); f2.setAccessible(true); f2.set(ctrl, t2);
        Field f3 = ctrl.getClass().getDeclaredField("thumb3View"); f3.setAccessible(true); f3.set(ctrl, t3);

        // call loadDetailThumbnail for hero and thumb1 eagerly via reflection (private method)
        java.lang.reflect.Method loadMeth = ctrl.getClass().getDeclaredMethod("loadDetailThumbnail", int.class, int.class, javafx.scene.image.ImageView.class);
        loadMeth.setAccessible(true);
        loadMeth.invoke(ctrl, 42, 0, hero);
        loadMeth.invoke(ctrl, 42, 1, t1);
        Thread.sleep(600);
        assertTrue(counts[0].get() >= 1, "hero/thumb0 should be requested");
        assertTrue(counts[1].get() >= 1, "thumb1 should be requested eagerly");
        assertEquals(0, counts[2].get(), "thumb2 should NOT be requested yet");
        assertEquals(0, counts[3].get(), "thumb3 should NOT be requested yet");

        // simulate clicking thumb2 (private handler) via reflection
        java.lang.reflect.Method click = ctrl.getClass().getDeclaredMethod("handleThumb2Click", javafx.scene.input.MouseEvent.class);
        click.setAccessible(true);
        click.invoke(ctrl, (Object) null);
        Thread.sleep(800);

        assertTrue(counts[1].get() >= 2, "thumb2 (image index 1) should be requested after click");

        // check that thumb2 view was populated
        assertNotNull(t2.getImage(), "thumb2 view should have been loaded after click");
    }

    @Test
    void showHeroImageIndexShouldPromoteTheMatchingImageSlot() throws Exception {
        new JFXPanel();

        AtomicInteger[] counts = new AtomicInteger[]{new AtomicInteger(0), new AtomicInteger(0), new AtomicInteger(0)};

        IAuctionService fake = new IAuctionService() {
            @Override public String login(String username, String password) { return null; }
            @Override public void register(String username, String password, String role) {}
            @Override public String getMyRole(String token) { return null; }
            @Override public void logout(String token) {}
            @Override public String serverTime() { return Instant.now().toString(); }
            @Override public List<AuctionItem> getActiveAuctions() { return List.of(); }
            @Override public List<AuctionItem> getAllAuctions() { return List.of(); }
            @Override public List<AuctionItem> searchActiveAuctions(String query, String category, String sortBy) { return List.of(); }
            @Override public List<AuctionItem> getActiveAuctionsBySeller(String sellerUsername, String token) { return List.of(); }
            @Override public AuctionItem getAuctionById(int auctionId) {
                return new AuctionItem(auctionId, "Title", "Desc", "misc", 1000, "seller", Instant.now().toString(), Instant.now().plusSeconds(3600).toString(), null);
            }
            @Override public void placeBid(int auctionId, long amountCents, long clientExpectedPriceCents, String token) {}
            @Override public List getBidHistory(int auctionId) { return List.of(); }
            @Override public int createAuction(AuctionItem item, byte[] image1, byte[] image2, byte[] image3, String token) { return 0; }
            @Override public void cancelAuction(int auctionId, String token) {}
            @Override public void relistAuction(int auctionId, String newEndTimeIso, String token) {}
            @Override public void startAuction(int auctionId, String token) {}
            @Override public List getMyBids(String token) { return List.of(); }
            @Override public List getMyWonAuctions(String token) { return List.of(); }
            @Override public byte[] getThumbnail(int auctionId, int imageIndex) throws RemoteException {
                counts[imageIndex].incrementAndGet();
                return createSinglePixelPng(switch (imageIndex) {
                    case 0 -> new Color(220, 20, 60);
                    case 1 -> new Color(34, 139, 34);
                    default -> new Color(30, 144, 255);
                });
            }
            @Override public byte[] getFullImage(int auctionId, int imageIndex) { return new byte[0]; }
            @Override public byte[] exportAuctionsToCSV(String token) { return new byte[0]; }
            @Override public void createUser(String newUsername, String password, String role, String token) {}
            @Override public List getAllUsers(String token) { return List.of(); }
            @Override public byte[] backupDatabase(String token) { return new byte[0]; }
            @Override public List<String> getAuditLogs(int lastNLines, String token) { return List.of(); }
        };

        Object prov = ClientContext.getInstance().getRmiProvider();
        Field svc = prov.getClass().getDeclaredField("service");
        svc.setAccessible(true);
        svc.set(prov, fake);

        AuctionDetailController ctrl = new AuctionDetailController();
        javafx.scene.image.ImageView hero = new javafx.scene.image.ImageView();
        javafx.scene.image.ImageView t1 = new javafx.scene.image.ImageView();
        javafx.scene.image.ImageView t2 = new javafx.scene.image.ImageView();
        javafx.scene.image.ImageView t3 = new javafx.scene.image.ImageView();
        Field fh = ctrl.getClass().getDeclaredField("heroImageView"); fh.setAccessible(true); fh.set(ctrl, hero);
        Field f1 = ctrl.getClass().getDeclaredField("thumb1View"); f1.setAccessible(true); f1.set(ctrl, t1);
        Field f2 = ctrl.getClass().getDeclaredField("thumb2View"); f2.setAccessible(true); f2.set(ctrl, t2);
        Field f3 = ctrl.getClass().getDeclaredField("thumb3View"); f3.setAccessible(true); f3.set(ctrl, t3);

        java.lang.reflect.Method loadMeth = ctrl.getClass().getDeclaredMethod("loadDetailThumbnail", int.class, int.class, javafx.scene.image.ImageView.class);
        loadMeth.setAccessible(true);
        loadMeth.invoke(ctrl, 77, 0, hero);
        loadMeth.invoke(ctrl, 77, 0, t1);
        loadMeth.invoke(ctrl, 77, 1, t2);
        loadMeth.invoke(ctrl, 77, 2, t3);
        Thread.sleep(800);

        java.lang.reflect.Method show = ctrl.getClass().getDeclaredMethod("showHeroImageIndex", int.class);
        show.setAccessible(true);
        show.invoke(ctrl, 1);
        Thread.sleep(400);

        assertEquals(new Color(34, 139, 34).getRGB(), pixelArgb(hero), "gallery index 1 should promote the second image slot");
        assertEquals(new Color(220, 20, 60).getRGB(), pixelArgb(t1), "thumb1 should contain primary image (index 0)");
        assertEquals(new Color(34, 139, 34).getRGB(), pixelArgb(t2), "thumb2 should contain image 1");
        assertEquals(new Color(30, 144, 255).getRGB(), pixelArgb(t3), "thumb3 should contain image 2");
        assertTrue(counts[1].get() >= 1, "image index 1 should be requested");
    }

    @Test
    void thirdThumbnailShouldLoadAndPromoteHero() throws Exception {
        new JFXPanel();

        AtomicInteger[] counts = new AtomicInteger[]{new AtomicInteger(0), new AtomicInteger(0), new AtomicInteger(0)};

        IAuctionService fake = new IAuctionService() {
            @Override public String login(String username, String password) { return null; }
            @Override public void register(String username, String password, String role) {}
            @Override public String getMyRole(String token) { return null; }
            @Override public void logout(String token) {}
            @Override public String serverTime() { return Instant.now().toString(); }
            @Override public List<AuctionItem> getActiveAuctions() { return List.of(); }
            @Override public List<AuctionItem> getAllAuctions() { return List.of(); }
            @Override public List<AuctionItem> searchActiveAuctions(String query, String category, String sortBy) { return List.of(); }
            @Override public List<AuctionItem> getActiveAuctionsBySeller(String sellerUsername, String token) { return List.of(); }
            @Override public AuctionItem getAuctionById(int auctionId) {
                return new AuctionItem(auctionId, "Title", "Desc", "misc", 1000, "seller", Instant.now().toString(), Instant.now().plusSeconds(3600).toString(), null);
            }
            @Override public void placeBid(int auctionId, long amountCents, long clientExpectedPriceCents, String token) {}
            @Override public List getBidHistory(int auctionId) { return List.of(); }
            @Override public int createAuction(AuctionItem item, byte[] image1, byte[] image2, byte[] image3, String token) { return 0; }
            @Override public void cancelAuction(int auctionId, String token) {}
            @Override public void relistAuction(int auctionId, String newEndTimeIso, String token) {}
            @Override public void startAuction(int auctionId, String token) {}
            @Override public List getMyBids(String token) { return List.of(); }
            @Override public List getMyWonAuctions(String token) { return List.of(); }
            @Override public byte[] getThumbnail(int auctionId, int imageIndex) throws RemoteException {
                counts[imageIndex].incrementAndGet();
                return createSinglePixelPng(switch (imageIndex) {
                    case 0 -> new Color(220, 20, 60);
                    case 1 -> new Color(34, 139, 34);
                    default -> new Color(30, 144, 255);
                });
            }
            @Override public byte[] getFullImage(int auctionId, int imageIndex) { return new byte[0]; }
            @Override public byte[] exportAuctionsToCSV(String token) { return new byte[0]; }
            @Override public void createUser(String newUsername, String password, String role, String token) {}
            @Override public List getAllUsers(String token) { return List.of(); }
            @Override public byte[] backupDatabase(String token) { return new byte[0]; }
            @Override public List<String> getAuditLogs(int lastNLines, String token) { return List.of(); }
        };

        Object prov = ClientContext.getInstance().getRmiProvider();
        Field svc = prov.getClass().getDeclaredField("service");
        svc.setAccessible(true);
        svc.set(prov, fake);

        AuctionDetailController ctrl = new AuctionDetailController();
        javafx.scene.image.ImageView hero = new javafx.scene.image.ImageView();
        javafx.scene.image.ImageView t1 = new javafx.scene.image.ImageView();
        javafx.scene.image.ImageView t2 = new javafx.scene.image.ImageView();
        javafx.scene.image.ImageView t3 = new javafx.scene.image.ImageView();
        Field fh = ctrl.getClass().getDeclaredField("heroImageView"); fh.setAccessible(true); fh.set(ctrl, hero);
        Field f1 = ctrl.getClass().getDeclaredField("thumb1View"); f1.setAccessible(true); f1.set(ctrl, t1);
        Field f2 = ctrl.getClass().getDeclaredField("thumb2View"); f2.setAccessible(true); f2.set(ctrl, t2);
        Field f3 = ctrl.getClass().getDeclaredField("thumb3View"); f3.setAccessible(true); f3.set(ctrl, t3);

        java.lang.reflect.Method loadMeth = ctrl.getClass().getDeclaredMethod("loadDetailThumbnail", int.class, int.class, javafx.scene.image.ImageView.class);
        loadMeth.setAccessible(true);
        loadMeth.invoke(ctrl, 88, 0, hero);
        loadMeth.invoke(ctrl, 88, 0, t1);
        loadMeth.invoke(ctrl, 88, 1, t2);
        loadMeth.invoke(ctrl, 88, 2, t3);
        Thread.sleep(800);

        java.lang.reflect.Method click = ctrl.getClass().getDeclaredMethod("handleThumb3Click", javafx.scene.input.MouseEvent.class);
        click.setAccessible(true);
        click.invoke(ctrl, (Object) null);
        Thread.sleep(400);

        assertEquals(new Color(30, 144, 255).getRGB(), pixelArgb(hero), "third thumbnail should promote image index 2 into the hero");
        assertEquals(new Color(30, 144, 255).getRGB(), pixelArgb(t3), "thumb3 should contain image 2");
        assertTrue(counts[2].get() >= 1, "image index 2 should be requested");
    }

    @Test
    void backButtonShouldReturnToDashboardAfterDashboardOpen() throws Exception {
        new JFXPanel();

        IAuctionService fake = new IAuctionService() {
            @Override public String login(String username, String password) { return null; }
            @Override public void register(String username, String password, String role) {}
            @Override public String getMyRole(String token) { return null; }
            @Override public void logout(String token) {}
            @Override public String serverTime() { return Instant.now().toString(); }
            @Override public List<AuctionItem> getActiveAuctions() { return List.of(); }
            @Override public List<AuctionItem> getAllAuctions() { return List.of(); }
            @Override public List<AuctionItem> searchActiveAuctions(String query, String category, String sortBy) { return List.of(); }
            @Override public List<AuctionItem> getActiveAuctionsBySeller(String sellerUsername, String token) { return List.of(); }
            @Override public AuctionItem getAuctionById(int auctionId) {
                return new AuctionItem(auctionId, "Title", "Desc", "misc", 1000, "seller", Instant.now().toString(), Instant.now().plusSeconds(3600).toString(), null);
            }
            @Override public void placeBid(int auctionId, long amountCents, long clientExpectedPriceCents, String token) {}
            @Override public List getBidHistory(int auctionId) { return List.of(); }
            @Override public int createAuction(AuctionItem item, byte[] image1, byte[] image2, byte[] image3, String token) { return 0; }
            @Override public void cancelAuction(int auctionId, String token) {}
            @Override public void relistAuction(int auctionId, String newEndTimeIso, String token) {}
            @Override public void startAuction(int auctionId, String token) {}
            @Override public List getMyBids(String token) { return List.of(); }
            @Override public List getMyWonAuctions(String token) { return List.of(); }
            @Override public byte[] getThumbnail(int auctionId, int imageIndex) throws RemoteException { return new byte[0]; }
            @Override public byte[] getFullImage(int auctionId, int imageIndex) { return new byte[0]; }
            @Override public byte[] exportAuctionsToCSV(String token) { return new byte[0]; }
            @Override public void createUser(String newUsername, String password, String role, String token) {}
            @Override public List getAllUsers(String token) { return List.of(); }
            @Override public byte[] backupDatabase(String token) { return new byte[0]; }
            @Override public List<String> getAuditLogs(int lastNLines, String token) { return List.of(); }
        };

        Object prov = ClientContext.getInstance().getRmiProvider();
        Field svc = prov.getClass().getDeclaredField("service");
        svc.setAccessible(true);
        svc.set(prov, fake);

        final class FakeViewLoader extends com.auction.client.ui.ViewLoader {
            volatile String loadedView;
            FakeViewLoader() { super(new javafx.stage.Stage()); }
            @Override
            public <T> T loadView(String fxmlFile) throws IOException {
                loadedView = fxmlFile;
                return null;
            }
        }

        CountDownLatch latch = new CountDownLatch(1);
        FakeViewLoader[] holder = new FakeViewLoader[1];
        javafx.application.Platform.runLater(() -> {
            holder[0] = new FakeViewLoader();
            ClientContext.getInstance().setViewLoader(holder[0]);
            latch.countDown();
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));

        ClientContext.getInstance().setPreviousViewName("user_dashboard.fxml");
        ClientContext.getInstance().setCurrentAuctionId(42);

        AuctionDetailController ctrl = new AuctionDetailController();
        Field backField = ctrl.getClass().getDeclaredField("backButton");
        backField.setAccessible(true);
        backField.set(ctrl, new javafx.scene.control.Button());
        Field heroField = ctrl.getClass().getDeclaredField("heroImageView");
        heroField.setAccessible(true);
        heroField.set(ctrl, new javafx.scene.image.ImageView());

        java.lang.reflect.Method setReturn = ctrl.getClass().getDeclaredMethod("setReturnViewName", String.class);
        setReturn.setAccessible(true);
        setReturn.invoke(ctrl, "user_dashboard.fxml");

        java.lang.reflect.Method back = ctrl.getClass().getDeclaredMethod("handleBackToGallery");
        back.setAccessible(true);
        back.invoke(ctrl);

        Thread.sleep(500);
        assertEquals("user_dashboard.fxml", holder[0].loadedView);
        assertEquals(-1, ClientContext.getInstance().getCurrentAuctionId());
    }

    private static byte[] createSinglePixelPng(Color color) {
        try {
            BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
            image.setRGB(0, 0, color.getRGB());
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "png", out);
            return out.toByteArray();
        } catch (Exception e) {
            return new byte[0];
        }
    }

    private static int pixelArgb(javafx.scene.image.ImageView imageView) {
        assertNotNull(imageView.getImage(), "image view should have an image");
        return imageView.getImage().getPixelReader().getArgb(0, 0);
    }
}
