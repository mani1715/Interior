package com.interior.platform.ai.provider;

import com.interior.platform.ai.domain.AiJobRecord;
import com.interior.platform.ai.domain.CostSource;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Profile("test")
public class MockAiImageProvider implements AiImageProvider {

    public enum SimulationMode {
        SUCCESS,
        RATE_LIMIT,
        TIMEOUT,
        REJECTION,
        AUTH_FAILURE,
        MALFORMED_OUTPUT,
        ASYNC
    }

    private SimulationMode mode = SimulationMode.SUCCESS;
    private final AtomicInteger submissionCount = new AtomicInteger(0);
    private final AtomicInteger cancelCount = new AtomicInteger(0);
    private AiJobRecord lastJob;
    private List<AiGenerationReference> lastReferences;
    private byte[] lastMaskBytes;

    public void setSimulationMode(SimulationMode mode) {
        this.mode = mode;
    }

    public void reset() {
        this.mode = SimulationMode.SUCCESS;
        this.submissionCount.set(0);
        this.cancelCount.set(0);
        this.lastJob = null;
        this.lastReferences = null;
        this.lastMaskBytes = null;
    }

    public int getSubmissionCount() {
        return submissionCount.get();
    }

    public int getCancelCount() {
        return cancelCount.get();
    }

    public AiJobRecord getLastJob() {
        return lastJob;
    }

    public List<AiGenerationReference> getLastReferences() {
        return lastReferences;
    }

    public byte[] getLastMaskBytes() {
        return lastMaskBytes;
    }

    @Override
    public String getProviderKey() {
        return "mock_configured";
    }

    @Override
    public boolean isConfigured() {
        return true;
    }

    @Override
    public boolean supportsReferenceImages() {
        return true;
    }

    @Override
    public int getMaxReferenceImages() {
        return 4;
    }

    @Override
    public boolean supportsMaskEditing() {
        return true;
    }

    @Override
    public ProviderGenerationResponse submitGeneration(
            AiJobRecord job,
            byte[] inputImageBytes,
            String inputContentType,
            List<AiGenerationReference> references
    ) {
        return submitGeneration(job, inputImageBytes, inputContentType, null, null, references);
    }

    @Override
    public ProviderGenerationResponse submitGeneration(
            AiJobRecord job,
            byte[] inputImageBytes,
            String inputContentType,
            byte[] maskBytes,
            String maskContentType,
            List<AiGenerationReference> references
    ) {
        submissionCount.incrementAndGet();
        this.lastJob = job;
        this.lastReferences = references;
        this.lastMaskBytes = maskBytes;

        return switch (mode) {
            case RATE_LIMIT ->
                    ProviderGenerationResponse.failure("PROVIDER_QUOTA_REACHED", "Mock AI quota reached.");
            case TIMEOUT ->
                    ProviderGenerationResponse.failure("PROVIDER_TIMEOUT", "Mock AI provider timed out.");
            case REJECTION ->
                    ProviderGenerationResponse.failure("AI_SAFETY_REJECTED", "Mock AI safety filter blocked the request.");
            case AUTH_FAILURE ->
                    ProviderGenerationResponse.failure("AI_AUTH_FAILED", "Mock AI authentication failed.");
            case MALFORMED_OUTPUT ->
                    ProviderGenerationResponse.immediateSuccess(new byte[]{0x00, 0x01, 0x02, 0x03}, "image/jpeg", "{\"model\":\"mock-model\"}");
            case ASYNC ->
                    ProviderGenerationResponse.asyncStarted("mock-job-" + job.id(), "{\"model\":\"mock-model\"}");
            case SUCCESS -> {
                byte[] mockImage = createMockJpegImage(job.prompt());
                yield ProviderGenerationResponse.immediateSuccess(
                        mockImage,
                        "image/jpeg",
                        "{\"model\":\"mock-model\",\"synthetic\":true}",
                        4L,
                        "USD",
                        CostSource.CALCULATED
                );
            }
        };
    }

    @Override
    public ProviderStatusResponse checkStatus(String providerJobId) {
        if (mode == SimulationMode.ASYNC) {
            byte[] mockImage = createMockJpegImage("Async Complete");
            return ProviderStatusResponse.success(providerJobId, mockImage, "image/jpeg", "{\"model\":\"mock-model\"}");
        }
        return ProviderStatusResponse.processing(providerJobId);
    }

    @Override
    public boolean cancel(String providerJobId) {
        cancelCount.incrementAndGet();
        return true;
    }

    private byte[] createMockJpegImage(String text) {
        int width = 300;
        int height = 300;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setColor(new Color(245, 240, 235)); // Warm stone background
        g2d.fillRect(0, 0, width, height);
        g2d.setColor(new Color(184, 138, 90)); // Bronze accent
        g2d.drawRect(10, 10, width - 20, height - 20);
        g2d.setColor(new Color(40, 40, 40));
        g2d.setFont(new Font("SansSerif", Font.BOLD, 14));
        g2d.drawString("ELÉGANCE AI CONCEPT", 25, 40);
        g2d.setFont(new Font("SansSerif", Font.PLAIN, 10));
        String label = (text != null && text.length() > 25) ? text.substring(0, 25) + "..." : (text != null ? text : "concept");
        g2d.drawString(label, 25, 70);
        g2d.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "jpg", baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate mock JPEG image", e);
        }
    }
}
