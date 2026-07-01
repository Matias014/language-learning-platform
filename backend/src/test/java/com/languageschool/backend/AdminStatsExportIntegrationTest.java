package com.languageschool.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.languageschool.backend.testsupport.AbstractIntegrationTest;
import com.languageschool.backend.testsupport.AuthTestClient;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class AdminStatsExportIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void admin_can_export_hardest_exercises_pdf_in_en_and_pl() throws Exception {
        var auth = new AuthTestClient(mockMvc, objectMapper);
        var admin = auth.login("admin", "password");

        var enRes = mockMvc.perform(
                        get("/api/admin/stats/exercises/hardest/export.pdf")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + admin.accessToken())
                                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                )
                .andExpect(status().isOk())
                .andExpect(header().exists(HttpHeaders.CONTENT_DISPOSITION))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(content().contentType("application/pdf"))
                .andReturn();

        String enText = extractPdfText(enRes.getResponse().getContentAsByteArray());
        assertThat(enText).isNotBlank();

        var plRes = mockMvc.perform(
                        get("/api/admin/stats/exercises/hardest/export.pdf")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + admin.accessToken())
                                .header(HttpHeaders.ACCEPT_LANGUAGE, "pl")
                )
                .andExpect(status().isOk())
                .andExpect(header().exists(HttpHeaders.CONTENT_DISPOSITION))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(content().contentType("application/pdf"))
                .andReturn();

        String plText = extractPdfText(plRes.getResponse().getContentAsByteArray());
        assertThat(plText).isNotBlank();
    }

    @Test
    void admin_can_export_llm_stats_csv() throws Exception {
        var auth = new AuthTestClient(mockMvc, objectMapper);
        var admin = auth.login("admin", "password");

        var res = mockMvc.perform(
                        get("/api/admin/stats/llm/export")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + admin.accessToken())
                )
                .andExpect(status().isOk())
                .andExpect(header().exists(HttpHeaders.CONTENT_DISPOSITION))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andReturn();

        String csv = new String(res.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
        assertThat(csv).isNotBlank();
    }

    @Test
    void export_endpoints_require_admin_role() throws Exception {
        var auth = new AuthTestClient(mockMvc, objectMapper);
        String login = "u_" + java.util.UUID.randomUUID().toString().replace("-", "");
        String email = login + "@example.com";
        var user = auth.register(login, email, "password");

        mockMvc.perform(
                        get("/api/admin/stats/exercises/hardest/export.pdf")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + user.accessToken())
                )
                .andExpect(status().isForbidden());

        mockMvc.perform(
                        get("/api/admin/stats/llm/export")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + user.accessToken())
                )
                .andExpect(status().isForbidden());
    }

    private static String extractPdfText(byte[] pdfBytes) throws Exception {
        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
            return new PDFTextStripper().getText(doc);
        }
    }
}
