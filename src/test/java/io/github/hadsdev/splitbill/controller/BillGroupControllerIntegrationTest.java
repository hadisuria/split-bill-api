package io.github.hadsdev.splitbill.controller;

import io.github.hadsdev.splitbill.dto.BillGroupResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class BillGroupControllerIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void shouldCreateGroupAddExpenseWithRemainderAndCalculateSettlement() throws Exception {
        String createGroupPayload = """
                {
                  "name": "Yogyakarta Roadtrip",
                  "participants": ["Hadi", "Fajar", "Laras"]
                }
                """;

        MvcResult groupResult = mockMvc.perform(post("/api/v1/groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createGroupPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", startsWith("grp_")))
                .andExpect(jsonPath("$.name").value("Yogyakarta Roadtrip"))
                .andReturn();

        BillGroupResponse createdGroup = objectMapper.readValue(
                groupResult.getResponse().getContentAsString(),
                BillGroupResponse.class
        );
        String groupId = createdGroup.id();

        String addExpensePayload = """
                {
                  "description": "Gudeg Yu Djum",
                  "amount": 100000.00,
                  "paidBy": "Hadi",
                  "splitType": "EQUAL",
                  "splitAmong": ["Hadi", "Fajar", "Laras"]
                }
                """;

        mockMvc.perform(post("/api/v1/groups/{groupId}/expenses", groupId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addExpensePayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", startsWith("exp_")))
                .andExpect(jsonPath("$.amount").value(100000.00))
                .andExpect(jsonPath("$.shares[0].amount").value(33333.34))
                .andExpect(jsonPath("$.shares[1].amount").value(33333.33))
                .andExpect(jsonPath("$.shares[2].amount").value(33333.33));

        mockMvc.perform(get("/api/v1/groups/{groupId}/settlement", groupId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groupId").value(groupId))
                .andExpect(jsonPath("$.totalExpenses").value(100000.00))
                .andExpect(jsonPath("$.service_charge_pct").value(4))
                .andExpect(jsonPath("$.service_charge_amount").value(4000.00))
                .andExpect(jsonPath("$.settlements.length()").value(2));

        mockMvc.perform(get("/api/v1/groups/{groupId}/audit-logs", groupId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id", startsWith("aud_")))
                .andExpect(jsonPath("$[0].action").value("GROUP_CREATED"))
                .andExpect(jsonPath("$[1].id", startsWith("aud_")))
                .andExpect(jsonPath("$[1].action").value("EXPENSE_ADDED"))
                .andExpect(jsonPath("$[1].actor").value("Hadi"));
    }

    @Test
    void shouldListSeededBillGroups() throws Exception {
        mockMvc.perform(get("/api/v1/groups"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$[0].id", startsWith("grp_")))
                .andExpect(jsonPath("$[0].name").value("Bandung Weekend"));
    }

    @Test
    void shouldReturn404WhenGroupDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/v1/groups/{groupId}/settlement", "grp_nonexistent"))
                .andExpect(status().isNotFound());
    }
}
