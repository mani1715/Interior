package com.interior.platform.ai.service;

import com.interior.platform.ai.dto.PromptEnhanceRequest;
import com.interior.platform.ai.dto.PromptEnhanceResponse;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;

@Service
public class PromptEnhancementService {

    private static final Map<String, String> KNOWN_ELEMENTS = new LinkedHashMap<>();
    private static final Map<String, String> KNOWN_MATERIALS = new LinkedHashMap<>();
    private static final Map<String, String> KNOWN_COLORS = new LinkedHashMap<>();
    private static final Map<String, String> KNOWN_HARDWARE = new LinkedHashMap<>();

    static {
        // Elements
        KNOWN_ELEMENTS.put("wardrobe shutter", "wardrobe shutters");
        KNOWN_ELEMENTS.put("wardrobe", "modular wardrobe unit");
        KNOWN_ELEMENTS.put("shutter", "cabinet shutters");
        KNOWN_ELEMENTS.put("cupboard", "modular cupboards");
        KNOWN_ELEMENTS.put("cabinet", "cabinetry frontages");
        KNOWN_ELEMENTS.put("countertop", "countertop surface");
        KNOWN_ELEMENTS.put("counter", "countertop surface");
        KNOWN_ELEMENTS.put("backsplash", "kitchen backsplash");
        KNOWN_ELEMENTS.put("wall", "architectural feature wall");
        KNOWN_ELEMENTS.put("floor", "flooring surface");
        KNOWN_ELEMENTS.put("ceiling", "ceiling soffit and cove");
        KNOWN_ELEMENTS.put("pooja", "pooja unit mandir niche");
        KNOWN_ELEMENTS.put("tv unit", "living media console wall");
        KNOWN_ELEMENTS.put("media wall", "living media feature wall");
        KNOWN_ELEMENTS.put("island", "kitchen island unit");

        // Materials & Finishes
        KNOWN_MATERIALS.put("walnut", "natural walnut wood veneer with vertical grain");
        KNOWN_MATERIALS.put("oak", "fluted warm oak millwork");
        KNOWN_MATERIALS.put("fluted", "architectural vertical fluted profiles");
        KNOWN_MATERIALS.put("marble", "honed Italian Calacatta marble slab");
        KNOWN_MATERIALS.put("granite", "leathered granite finish");
        KNOWN_MATERIALS.put("quartz", "seamless engineered quartz");
        KNOWN_MATERIALS.put("matte", "soft-touch ultra-matte anti-fingerprint finish");
        KNOWN_MATERIALS.put("lacquer", "satin lacquer cabinetry finish");
        KNOWN_MATERIALS.put("veneer", "architectural natural timber veneer");
        KNOWN_MATERIALS.put("laminate", "high-pressure architectural laminate");
        KNOWN_MATERIALS.put("cane", "natural woven cane webbing insert");
        KNOWN_MATERIALS.put("glass", "fluted bronze-tinted tempered glass");
        KNOWN_MATERIALS.put("linen", "textured Belgian linen upholstery");
        KNOWN_MATERIALS.put("terracotta", "matte lime-washed terracotta plaster");
        KNOWN_MATERIALS.put("acoustic", "vertical slatted acoustic timber panels");

        // Colors
        KNOWN_COLORS.put("white", "warm alabaster white");
        KNOWN_COLORS.put("black", "deep matte charcoal black");
        KNOWN_COLORS.put("grey", "soft architectural grey");
        KNOWN_COLORS.put("gray", "soft architectural grey");
        KNOWN_COLORS.put("beige", "warm oat beige");
        KNOWN_COLORS.put("cream", "rich cream ivory");
        KNOWN_COLORS.put("gold", "brushed champagne gold");
        KNOWN_COLORS.put("brass", "brushed warm brass");
        KNOWN_COLORS.put("bronze", "aged antique bronze");
        KNOWN_COLORS.put("burgundy", "muted deep burgundy");
        KNOWN_COLORS.put("sage", "muted sage olive");
        KNOWN_COLORS.put("terracotta", "warm earthy terracotta");

        // Hardware
        KNOWN_HARDWARE.put("handle", "architectural handle pulls");
        KNOWN_HARDWARE.put("pull", "pull handles");
        KNOWN_HARDWARE.put("knob", "minimal knurled knobs");
        KNOWN_HARDWARE.put("profile", "integrated J-pull recessed profile");
        KNOWN_HARDWARE.put("j-pull", "concealed J-pull seamless edge");
        KNOWN_HARDWARE.put("knurled", "precision knurled cylindrical pulls");
    }

    public PromptEnhanceResponse enhancePrompt(PromptEnhanceRequest request) {
        String raw = request.prompt() != null ? request.prompt().trim() : "";
        String lower = raw.toLowerCase(Locale.ROOT);

        List<String> detectedElements = extractMatches(lower, KNOWN_ELEMENTS);
        List<String> detectedMaterials = extractMatches(lower, KNOWN_MATERIALS);
        List<String> detectedColors = extractMatches(lower, KNOWN_COLORS);
        List<String> detectedHardware = extractMatches(lower, KNOWN_HARDWARE);

        boolean isPrecisionMask = "PRECISION_MASK".equalsIgnoreCase(request.editingMode());
        boolean preserveStructure = request.preserveStructure() == null || request.preserveStructure();

        StringBuilder sb = new StringBuilder();

        // 1. Primary Action & Target
        if (isPrecisionMask) {
            sb.append("Targeted inpainting edit on selected masked area: ");
        } else {
            sb.append("Photorealistic architectural interior transformation: ");
        }

        // Room context if available
        if (request.roomType() != null && !request.roomType().isBlank()) {
            sb.append("in ").append(request.roomType().toLowerCase(Locale.ROOT)).append(" space. ");
        }

        // 2. Element and Finishes Synthesis
        if (!detectedElements.isEmpty() || !detectedMaterials.isEmpty() || !detectedColors.isEmpty()) {
            sb.append("Update ");
            if (!detectedElements.isEmpty()) {
                sb.append(String.join(" and ", detectedElements));
            } else {
                sb.append("designated cabinetry surfaces");
            }
            sb.append(" to feature ");

            List<String> finishParts = new ArrayList<>();
            if (!detectedColors.isEmpty()) {
                finishParts.add(String.join(", ", detectedColors));
            }
            if (!detectedMaterials.isEmpty()) {
                finishParts.add(String.join(", ", detectedMaterials));
            }
            if (!detectedHardware.isEmpty()) {
                finishParts.add("accented with " + String.join(" and ", detectedHardware));
            }

            if (!finishParts.isEmpty()) {
                sb.append(String.join(" paired with ", finishParts)).append(". ");
            } else {
                sb.append("refined architectural finishes matching natural material references. ");
            }
        } else {
            // Fallback structured expansion of user's exact intent
            sb.append(raw);
            if (!raw.endsWith(".")) {
                sb.append(".");
            }
            sb.append(" ");
        }

        // 3. Style integration
        if (request.architecturalStyle() != null && !request.architecturalStyle().isBlank()) {
            sb.append("Aesthetic direction adheres strictly to ").append(request.architecturalStyle()).append(" style principles. ");
        }

        // 4. Structural Preservation & Negative Constraints
        List<String> preservationClauses = new ArrayList<>();
        if (isPrecisionMask) {
            preservationClauses.add("Surrounding unmasked walls, ceiling, flooring, and adjacent fixtures remain 100% untouched and geometrically identical");
        }
        if (preserveStructure) {
            preservationClauses.add("Architectural structural boundaries, door/window openings, electrical switchboards, and room perspective must remain strictly preserved without warping");
        }
        preservationClauses.add("Ensure authentic material texture, diffused ambient illumination, and photorealistic joinery reveals");

        String preservationDirectives = String.join("; ", preservationClauses) + ".";
        sb.append(preservationDirectives);

        String enhancedPrompt = sb.toString().trim();

        return new PromptEnhanceResponse(
                raw,
                enhancedPrompt,
                detectedElements,
                detectedMaterials,
                detectedColors,
                detectedHardware,
                preservationDirectives,
                "DETERMINISTIC_ARCHITECTURAL_PARSER"
        );
    }

    private List<String> extractMatches(String text, Map<String, String> vocabulary) {
        Set<String> matches = new LinkedHashSet<>();
        for (Map.Entry<String, String> entry : vocabulary.entrySet()) {
            Pattern pattern = Pattern.compile("\\b" + Pattern.quote(entry.getKey()) + "(?:s|es)?\\b", Pattern.CASE_INSENSITIVE);
            if (pattern.matcher(text).find()) {
                matches.add(entry.getValue());
            }
        }
        return new ArrayList<>(matches);
    }
}
