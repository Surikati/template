package cz.komercpoj.tmpmgmt.assembly.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.komercpoj.tmpmgmt.assembly.client.ClauseServiceClient;
import cz.komercpoj.tmpmgmt.assembly.client.ClauseVersionDto;
import cz.komercpoj.tmpmgmt.common.DomainException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClauseResolverTest {

  @Mock ClauseServiceClient clauseClient;

  private final ObjectMapper mapper = new ObjectMapper();

  private ClauseResolver resolver() {
    return new ClauseResolver(clauseClient, mapper);
  }

  @Test
  void noClauseRefs_returnsStructurallyEqualAst() throws Exception {
    JsonNode ast =
        mapper.readTree(
            """
            {
              "type": "doc",
              "content": [
                { "type": "paragraph", "content": [{ "type": "text", "text": "Hello" }] }
              ]
            }
            """);

    JsonNode resolved = resolver().resolveClauseRefs(ast);

    assertThat(resolved).isEqualTo(ast);
    verifyNoInteractions(clauseClient);
  }

  @Test
  void clauseRefAtDocRoot_isReplacedByFragmentChildren() throws Exception {
    UUID clauseId = UUID.randomUUID();
    when(clauseClient.getVersion(clauseId, 2))
        .thenReturn(
            stubVersion(
                clauseId,
                2,
                """
                    { "type": "fragment", "content": [
                      { "type": "paragraph", "content": [{ "type": "text", "text": "GDPR clause." }] },
                      { "type": "paragraph", "content": [{ "type": "text", "text": "Signed." }] }
                    ]}
                    """));

    JsonNode ast =
        mapper.readTree(
            """
            {
              "type": "doc",
              "content": [
                { "type": "paragraph", "content": [{ "type": "text", "text": "Pre-clause." }] },
                { "type": "clauseRef", "attrs": { "clauseId": "%s", "versionNumber": 2 } },
                { "type": "paragraph", "content": [{ "type": "text", "text": "Post-clause." }] }
              ]
            }
            """
                .formatted(clauseId));

    JsonNode resolved = resolver().resolveClauseRefs(ast);

    // Root stays 'doc', children expanded 1 + 2 + 1 = 4 paragraphs in order.
    assertThat(resolved.get("type").asText()).isEqualTo("doc");
    JsonNode children = resolved.get("content");
    assertThat(children.size()).isEqualTo(4);
    assertThat(textOfFirstChildOf(children.get(0))).isEqualTo("Pre-clause.");
    assertThat(textOfFirstChildOf(children.get(1))).isEqualTo("GDPR clause.");
    assertThat(textOfFirstChildOf(children.get(2))).isEqualTo("Signed.");
    assertThat(textOfFirstChildOf(children.get(3))).isEqualTo("Post-clause.");
  }

  @Test
  void clauseRefInsideConditionBlock_resolvesNested() throws Exception {
    UUID clauseId = UUID.randomUUID();
    when(clauseClient.getVersion(clauseId, 1))
        .thenReturn(
            stubVersion(
                clauseId,
                1,
                """
                    { "type": "fragment", "content": [
                      { "type": "paragraph", "content": [{ "type": "text", "text": "Extended terms." }] }
                    ]}
                    """));

    JsonNode ast =
        mapper.readTree(
            """
            {
              "type": "doc",
              "content": [
                {
                  "type": "conditionBlock",
                  "attrs": { "when": "total > 1000" },
                  "content": [
                    { "type": "clauseRef", "attrs": { "clauseId": "%s", "versionNumber": 1 } }
                  ]
                }
              ]
            }
            """
                .formatted(clauseId));

    JsonNode resolved = resolver().resolveClauseRefs(ast);

    JsonNode condBlock = resolved.get("content").get(0);
    assertThat(condBlock.get("type").asText()).isEqualTo("conditionBlock");
    assertThat(condBlock.get("attrs").get("when").asText()).isEqualTo("total > 1000");
    JsonNode condChildren = condBlock.get("content");
    assertThat(condChildren.size()).isEqualTo(1);
    assertThat(textOfFirstChildOf(condChildren.get(0))).isEqualTo("Extended terms.");
  }

  @Test
  void clauseReferencingAnotherClause_isTransitivelyResolved() throws Exception {
    UUID outerId = UUID.randomUUID();
    UUID innerId = UUID.randomUUID();

    when(clauseClient.getVersion(outerId, 1))
        .thenReturn(
            stubVersion(
                outerId,
                1,
                """
                    { "type": "fragment", "content": [
                      { "type": "paragraph", "content": [{ "type": "text", "text": "Outer." }] },
                      { "type": "clauseRef", "attrs": { "clauseId": "%s", "versionNumber": 1 } }
                    ]}
                    """
                    .formatted(innerId)));
    when(clauseClient.getVersion(innerId, 1))
        .thenReturn(
            stubVersion(
                innerId,
                1,
                """
                    { "type": "fragment", "content": [
                      { "type": "paragraph", "content": [{ "type": "text", "text": "Inner." }] }
                    ]}
                    """));

    JsonNode ast =
        mapper.readTree(
            """
            {
              "type": "doc",
              "content": [
                { "type": "clauseRef", "attrs": { "clauseId": "%s", "versionNumber": 1 } }
              ]
            }
            """
                .formatted(outerId));

    JsonNode resolved = resolver().resolveClauseRefs(ast);

    JsonNode children = resolved.get("content");
    assertThat(children.size()).isEqualTo(2);
    assertThat(textOfFirstChildOf(children.get(0))).isEqualTo("Outer.");
    assertThat(textOfFirstChildOf(children.get(1))).isEqualTo("Inner.");
  }

  @Test
  void invalidClauseRefId_throwsDomainException() throws Exception {
    JsonNode ast =
        mapper.readTree(
            """
            {
              "type": "doc",
              "content": [
                { "type": "clauseRef", "attrs": { "clauseId": "not-a-uuid", "versionNumber": 1 } }
              ]
            }
            """);

    assertThatThrownBy(() -> resolver().resolveClauseRefs(ast))
        .isInstanceOf(DomainException.class)
        .hasMessageContaining("not a UUID");
  }

  @Test
  void clauseWithoutFragmentRoot_throwsDomainException() throws Exception {
    UUID clauseId = UUID.randomUUID();
    when(clauseClient.getVersion(eq(clauseId), anyInt()))
        .thenReturn(stubVersion(clauseId, 1, "{ \"type\": \"doc\", \"content\": [] }"));

    JsonNode ast =
        mapper.readTree(
            """
            {
              "type": "doc",
              "content": [
                { "type": "clauseRef", "attrs": { "clauseId": "%s", "versionNumber": 1 } }
              ]
            }
            """
                .formatted(clauseId));

    assertThatThrownBy(() -> resolver().resolveClauseRefs(ast))
        .isInstanceOf(DomainException.class)
        .hasMessageContaining("fragment");
  }

  // --- helpers --------------------------------------------------------------------------

  private ClauseVersionDto stubVersion(UUID clauseId, int version, String contentJson)
      throws Exception {
    return new ClauseVersionDto(
        UUID.randomUUID(),
        clauseId,
        version,
        mapper.readTree(contentJson),
        null,
        Instant.now(),
        UUID.randomUUID());
  }

  private static String textOfFirstChildOf(JsonNode paragraph) {
    return paragraph.path("content").get(0).path("text").asText();
  }
}
