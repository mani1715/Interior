package com.interior.platform.ai.service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.regex.Pattern;

/** Recognizes explicit two-region colour requests; does not infer image geometry. */
public final class RegionalColourInstructions {
    private RegionalColourInstructions() {}
    public static final String GUIDANCE = "Different upper and lower colours need separate precise edits. Paint one region and describe only its colour, then use the result for the next region.";
    public static boolean needsSeparateSelections(String prompt) {
        if (prompt == null) return false;
        String instruction = prompt.replaceAll("(?i)\\b(?:do not|don't|preserve|leave|keep)\\b[^.;]*(?:[.;]|$)", " ");
        var regions = Pattern.compile("(?i)\\b(upper|top|lower|bottom|down)\\b([^.;]*?)(?=\\b(?:upper|top|lower|bottom|down)\\b|[.;]|$)").matcher(instruction);
        var colours = Pattern.compile("(?i)\\b(white|black|yellow|blue|green|red|orange|pink|purple|grey|gray|beige|cream|brown|navy|sage)\\b");
        var assignments = new HashMap<String, String>();
        while (regions.find()) {
            var found = colours.matcher(regions.group(2));
            var values = new HashSet<String>();
            while (found.find()) values.add(found.group(1).toLowerCase(Locale.ROOT));
            if (values.size() != 1) continue;
            String region = regions.group(1).matches("(?i)upper|top") ? "upper" : "lower";
            String colour = values.iterator().next();
            if (assignments.containsKey(region) && !assignments.get(region).equals(colour)) return false;
            assignments.put(region, colour);
        }
        return assignments.size() == 2 && !assignments.get("upper").equals(assignments.get("lower"));
    }
}
