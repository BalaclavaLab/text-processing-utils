package org.kgusarov.textprocessing.analysis;

import org.junit.Test;

import java.util.List;

import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertThat;

public class TermExtractionServiceTest {
    private static final TermExtractionService SERVICE = new TermExtractionService();

    @Test
    public void testGetLvTerms() throws Exception {
        final List<String> terms = SERVICE.getTerms("šašliks rīgā");
        assertThat(terms, hasItem("šašlik"));
    }

    @Test
    public void testGetRuTerms() throws Exception {
        final List<String> terms = SERVICE.getTerms("шашлык в Риге");
        assertThat(terms, hasItem("шашлык"));
    }
}