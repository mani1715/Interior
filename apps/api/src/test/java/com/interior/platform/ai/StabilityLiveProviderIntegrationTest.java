package com.interior.platform.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interior.platform.ai.config.AiVisualizerProperties;
import com.interior.platform.ai.domain.AiJobRecord;
import com.interior.platform.ai.domain.AiJobStatus;
import com.interior.platform.ai.domain.EditingMode;
import com.interior.platform.ai.provider.ConfigurableAiImageProvider;
import com.interior.platform.ai.provider.ProviderGenerationResponse;
import com.interior.platform.ai.service.StabilityPromptBuilder;
import com.interior.platform.common.service.SsrfProtectionService;
import com.interior.platform.common.util.UuidV7;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Stability AI Real Live Provider Integration Tests (Opt-In)")
public class StabilityLiveProviderIntegrationTest {

    private static String apiKey;
    private static ConfigurableAiImageProvider provider;
    private static Path outputDir;
    private static Path wardrobePath;
    private static Path kitchenPath;

    @BeforeAll
    static void setUpAll() throws IOException {
        apiKey = resolveApiKey();
        boolean allowLive = "true".equalsIgnoreCase(System.getenv("ALLOW_LIVE_AI_TEST"))
                || "true".equalsIgnoreCase(System.getProperty("ALLOW_LIVE_AI_TEST"));

        Assumptions.assumeTrue(allowLive, "Skipping live AI test: ALLOW_LIVE_AI_TEST is not enabled.");
        Assumptions.assumeTrue(apiKey != null && !apiKey.isBlank(), "Skipping live AI test: No STABILITY_API_KEY resolved.");

        outputDir = Path.of("target/ai-live-results");
        Files.createDirectories(outputDir);

        // Locate sample interior images
        wardrobePath = findImagePath("apps/web/public/images/approved/wardrobe.png");
        kitchenPath = findImagePath("apps/web/public/images/approved/kitchen.png");

        Assumptions.assumeTrue(Files.exists(wardrobePath), "Wardrobe image not found at " + wardrobePath);
        Assumptions.assumeTrue(Files.exists(kitchenPath), "Kitchen image not found at " + kitchenPath);

        AiVisualizerProperties props = new AiVisualizerProperties();
        props.setProvider("stability");
        props.setApiKey(apiKey);
        props.setEndpoint("https://api.stability.ai");
        props.setModel("stability-v2beta");
        props.setTimeoutSeconds(90);
        props.setGrowMask(3);

        provider = new ConfigurableAiImageProvider(
                props,
                new SsrfProtectionService(),
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build(),
                new ObjectMapper()
        );
    }

    private static Path findImagePath(String relativePath) {
        Path p = Path.of(relativePath);
        if (Files.exists(p)) return p;
        Path oneUp = Path.of("../", relativePath);
        if (Files.exists(oneUp)) return oneUp;
        Path parentP = Path.of("../../", relativePath);
        if (Files.exists(parentP)) return parentP;
        return p;
    }

    private static String resolveApiKey() {
        String key = System.getenv("STABILITY_API_KEY");
        if (key != null && !key.isBlank()) return key.trim();
        key = System.getProperty("STABILITY_API_KEY");
        if (key != null && !key.isBlank()) return key.trim();

        for (Path envPath : List.of(Path.of(".env.local"), Path.of("../../.env.local"), Path.of("../.env.local"))) {
            if (Files.exists(envPath)) {
                try {
                    for (String line : Files.readAllLines(envPath)) {
                        if (line.trim().startsWith("STABILITY_API_KEY=")) {
                            String found = line.trim().substring("STABILITY_API_KEY=".length()).trim();
                            if (!found.isBlank()) return found;
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        }
        return null;
    }

    private AiJobRecord createJob(EditingMode mode, String prompt) {
        return new AiJobRecord(
                UuidV7.randomUuid(),
                UuidV7.randomUuid(),
                UuidV7.randomUuid(),
                UuidV7.randomUuid(),
                null,
                "stability",
                null,
                prompt,
                null,
                AiJobStatus.PROCESSING,
                null,
                null,
                0,
                "live-test-" + UUID.randomUUID(),
                UuidV7.randomUuid(),
                Instant.now(),
                Instant.now(),
                null,
                null,
                null,
                0,
                true,
                mode,
                mode == EditingMode.PRECISION_MASK ? "studio/test/masks/mask.png" : null
        );
    }

    @Test
    @DisplayName("Live Quick Recolor: Bedroom Wardrobe -> Matte Walnut Brown finish")
    void testLiveQuickRecolorWardrobeWalnut() throws Exception {
        byte[] inputBytes = Files.readAllBytes(wardrobePath);
        String prompt = "Change the wardrobe to a premium matte walnut brown finish while preserving the original wardrobe structure, handles, grooves, lighting, shadows and the entire surrounding room.";
        AiJobRecord job = createJob(EditingMode.FULL_IMAGE, prompt);

        long start = System.currentTimeMillis();
        ProviderGenerationResponse response = provider.submitGeneration(job, inputBytes, "image/png", null, null, List.of());
        long latencyMs = System.currentTimeMillis() - start;

        System.out.println("===> [LIVE 1/4] Quick Recolor (Wardrobe -> Walnut) latency: " + latencyMs + "ms, success: " + response.isSuccess());

        assertTrue(response.isSuccess(), "Live Quick Recolor call failed: " + response.errorMessage());
        assertNotNull(response.imageBytes());
        assertTrue(response.imageBytes().length > 10000, "Generated image bytes must be substantial");
        assertTrue(response.usageMetadata().contains("STABILITY_SEARCH_RECOLOR"));
        assertTrue(response.usageMetadata().contains("\"credits\":5"));

        Path outFile = outputDir.resolve("01_live_quick_wardrobe_walnut.png");
        Files.write(outFile, response.imageBytes());
        System.out.println("===> Saved: " + outFile.toAbsolutePath());
    }

    @Test
    @DisplayName("Live Quick Recolor: Kitchen Cabinets -> Warm Sage Green matte laminate")
    void testLiveQuickRecolorKitchenSage() throws Exception {
        byte[] inputBytes = Files.readAllBytes(kitchenPath);
        String prompt = "Recolor only the kitchen cabinet shutters to warm sage green matte laminate. Keep the countertop, backsplash, handles, appliances, walls, floor and lighting unchanged.";
        AiJobRecord job = createJob(EditingMode.FULL_IMAGE, prompt);

        long start = System.currentTimeMillis();
        ProviderGenerationResponse response = provider.submitGeneration(job, inputBytes, "image/png", null, null, List.of());
        long latencyMs = System.currentTimeMillis() - start;

        System.out.println("===> [LIVE 2/4] Quick Recolor (Kitchen -> Sage) latency: " + latencyMs + "ms, success: " + response.isSuccess());

        assertTrue(response.isSuccess(), "Live Quick Recolor call failed: " + response.errorMessage());
        assertNotNull(response.imageBytes());
        assertTrue(response.imageBytes().length > 10000);
        assertTrue(response.usageMetadata().contains("STABILITY_SEARCH_RECOLOR"));
        assertTrue(response.usageMetadata().contains("\"credits\":5"));

        Path outFile = outputDir.resolve("02_live_quick_kitchen_sage.png");
        Files.write(outFile, response.imageBytes());
        System.out.println("===> Saved: " + outFile.toAbsolutePath());
    }

    @Test
    @DisplayName("Live Precise Inpaint: Wardrobe Door Mask -> Natural Oak Wood finish")
    void testLivePreciseInpaintWardrobeOak() throws Exception {
        byte[] inputBytes = Files.readAllBytes(wardrobePath);
        BufferedImage inImg = ImageIO.read(wardrobePath.toFile());
        int w = inImg.getWidth();
        int h = inImg.getHeight();

        // Create deterministic mask for wardrobe doors (white in center, black outside)
        BufferedImage maskImg = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = maskImg.createGraphics();
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, w, h);
        g.setColor(Color.WHITE);
        g.fillRect((int) (w * 0.25), (int) (h * 0.15), (int) (w * 0.50), (int) (h * 0.70));
        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(maskImg, "png", baos);
        byte[] maskBytes = baos.toByteArray();

        String prompt = "Change only the masked wardrobe panels to natural oak wood finish. Preserve the exact door divisions, handles, grooves, perspective, lighting and every unmasked part of the room.";
        AiJobRecord job = createJob(EditingMode.PRECISION_MASK, prompt);

        long start = System.currentTimeMillis();
        ProviderGenerationResponse response = provider.submitGeneration(job, inputBytes, "image/png", maskBytes, "image/png", List.of());
        long latencyMs = System.currentTimeMillis() - start;

        System.out.println("===> [LIVE 3/4] Precise Inpaint (Wardrobe -> Oak) latency: " + latencyMs + "ms, success: " + response.isSuccess());

        assertTrue(response.isSuccess(), "Live Precise Inpaint call failed: " + response.errorMessage());
        assertNotNull(response.imageBytes());
        assertTrue(response.imageBytes().length > 10000);
        assertTrue(response.usageMetadata().contains("STABILITY_INPAINT"));
        assertTrue(response.usageMetadata().contains("\"credits\":5"));

        Path outFile = outputDir.resolve("03_live_precise_wardrobe_oak.png");
        Files.write(outFile, response.imageBytes());
        System.out.println("===> Saved: " + outFile.toAbsolutePath());
    }

    @Test
    @DisplayName("Live Precise Inpaint: Cabinet Door Mask -> Smooth Matte Off-White laminate")
    void testLivePreciseInpaintCabinetOffWhite() throws Exception {
        byte[] inputBytes = Files.readAllBytes(wardrobePath);
        BufferedImage inImg = ImageIO.read(wardrobePath.toFile());
        int w = inImg.getWidth();
        int h = inImg.getHeight();

        BufferedImage maskImg = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = maskImg.createGraphics();
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, w, h);
        g.setColor(Color.WHITE);
        g.fillRect((int) (w * 0.25), (int) (h * 0.15), (int) (w * 0.50), (int) (h * 0.70));
        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(maskImg, "png", baos);
        byte[] maskBytes = baos.toByteArray();

        String prompt = "Apply a smooth matte off-white laminate finish only to the masked cabinet doors. Do not alter hardware, geometry, shadows or surrounding objects.";
        AiJobRecord job = createJob(EditingMode.PRECISION_MASK, prompt);

        long start = System.currentTimeMillis();
        ProviderGenerationResponse response = provider.submitGeneration(job, inputBytes, "image/png", maskBytes, "image/png", List.of());
        long latencyMs = System.currentTimeMillis() - start;

        System.out.println("===> [LIVE 4/4] Precise Inpaint (Cabinet -> Off-White) latency: " + latencyMs + "ms, success: " + response.isSuccess());

        assertTrue(response.isSuccess(), "Live Precise Inpaint call failed: " + response.errorMessage());
        assertNotNull(response.imageBytes());
        assertTrue(response.imageBytes().length > 10000);
        assertTrue(response.usageMetadata().contains("STABILITY_INPAINT"));
        assertTrue(response.usageMetadata().contains("\"credits\":5"));

        Path outFile = outputDir.resolve("04_live_precise_cabinet_offwhite.png");
        Files.write(outFile, response.imageBytes());
        System.out.println("===> Saved: " + outFile.toAbsolutePath());
    }
}
