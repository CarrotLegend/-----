package com.carrot123.yixiangshouronglu.util;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ExtractEntityNormalizationTest {
    @Test void onlyConfirmedWendigoVariantsNormalizeIdempotently() {
        for (String id : List.of("antlers:wendigo", "antlers:wendigomad", "antlers:wendigo_flute")) {
            String normalized = SoulEntityUtil.normalizeExtractEntityId(id);
            assertEquals("antlers:wendigo", normalized);
            assertEquals(normalized, SoulEntityUtil.normalizeExtractEntityId(normalized));
        }
        for (String id : List.of("minecraft:warden", "antlers:unknown_wendigo", "other:wendigo", "")) { assertEquals(id, SoulEntityUtil.normalizeExtractEntityId(id)); }
        assertTrue(SoulEntityUtil.isAntlersWendigo("antlers:unknown_wendigo"));
    }
}
