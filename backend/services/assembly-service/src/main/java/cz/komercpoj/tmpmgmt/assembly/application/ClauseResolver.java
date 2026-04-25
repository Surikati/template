package cz.komercpoj.tmpmgmt.assembly.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import cz.komercpoj.tmpmgmt.assembly.client.ClauseServiceClient;
import cz.komercpoj.tmpmgmt.assembly.client.ClauseVersionDto;
import cz.komercpoj.tmpmgmt.common.DomainException;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Expands {@code clauseRef} nodes in a template AST by fetching the referenced clause version from
 * clause-service and inlining its fragment content in place. A clauseRef is a block node that gets
 * replaced by zero or more block nodes.
 *
 * <p>Recursion descends into {@code content} arrays so clauseRefs inside {@code conditionBlock},
 * {@code repeatBlock}, and nested clauses are all resolved. The resolver walks the expanded content
 * too, so clauses referencing other clauses are handled transitively — subject to a cycle guard
 * (max depth) to avoid stack overflow.
 */
@Component
public class ClauseResolver {

  private static final int MAX_DEPTH = 10;

  private final ClauseServiceClient clauses;
  private final ObjectMapper mapper;

  public ClauseResolver(ClauseServiceClient clauses, ObjectMapper mapper) {
    this.clauses = clauses;
    this.mapper = mapper;
  }

  /** Returns a new AST with all clauseRef nodes expanded. The input is left untouched. */
  public JsonNode resolveClauseRefs(JsonNode root) {
    if (root == null || !root.isObject()) return root;
    return resolveContainer(root, 0);
  }

  /**
   * Copies the given container node, replacing its {@code content} array with the resolved
   * (expanded) children. Returns the copy — inputs are never mutated.
   */
  private JsonNode resolveContainer(JsonNode container, int depth) {
    JsonNode children = container.path("content");
    if (!children.isArray()) return container;

    ObjectNode out = container.deepCopy();
    ArrayNode resolvedChildren = mapper.createArrayNode();
    for (JsonNode child : children) {
      appendResolved(child, resolvedChildren, depth);
    }
    out.set("content", resolvedChildren);
    return out;
  }

  private void appendResolved(JsonNode node, ArrayNode target, int depth) {
    String type = node.path("type").asText("");
    if ("clauseRef".equals(type)) {
      expandClauseRef(node, target, depth);
      return;
    }
    if (node.isObject() && node.has("content") && node.get("content").isArray()) {
      target.add(resolveContainer(node, depth));
    } else {
      target.add(node);
    }
  }

  private void expandClauseRef(JsonNode refNode, ArrayNode target, int depth) {
    if (depth >= MAX_DEPTH) {
      throw new DomainException(
          "clause.resolution_cycle",
          "Clause reference resolution exceeded depth " + MAX_DEPTH + " — likely a cycle.");
    }
    JsonNode attrs = refNode.path("attrs");
    String clauseIdText = attrs.path("clauseId").asText(null);
    int versionNumber = attrs.path("versionNumber").asInt(-1);
    if (clauseIdText == null || versionNumber <= 0) {
      throw new DomainException(
          "clause.ref_invalid", "clauseRef missing clauseId or versionNumber: " + refNode);
    }

    UUID clauseId;
    try {
      clauseId = UUID.fromString(clauseIdText);
    } catch (IllegalArgumentException e) {
      throw new DomainException(
          "clause.ref_invalid", "clauseRef.clauseId is not a UUID: " + clauseIdText);
    }

    ClauseVersionDto clause = clauses.getVersion(clauseId, versionNumber);
    JsonNode fragment = clause.content();
    if (fragment == null
        || !fragment.isObject()
        || !"fragment".equals(fragment.path("type").asText(null))) {
      throw new DomainException(
          "clause.content_invalid",
          "Clause " + clauseId + " v" + versionNumber + " does not have a fragment root.");
    }
    JsonNode fragmentChildren = fragment.path("content");
    if (!fragmentChildren.isArray()) {
      return; // empty fragment — nothing to inline
    }

    // Walk fragment children, recursively resolving any further clauseRefs they contain.
    for (JsonNode frag : fragmentChildren) {
      appendResolved(frag, target, depth + 1);
    }
  }
}
