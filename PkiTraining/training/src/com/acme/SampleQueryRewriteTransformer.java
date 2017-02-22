/**
* Copyright 2015 Attivio Inc., All rights reserved.
*/
package com.acme;

import java.util.ArrayList;
import java.util.List;

import com.attivio.sdk.search.QueryRequest;
import com.attivio.sdk.AttivioException;
import com.attivio.sdk.schema.FieldNames;
import com.attivio.sdk.search.query.BooleanAndQuery;
import com.attivio.sdk.search.query.BooleanOrQuery;
import com.attivio.sdk.search.query.JoinQuery;
import com.attivio.sdk.search.query.PhraseQuery;
import com.attivio.sdk.search.query.Query;
import com.attivio.sdk.search.QueryFeedback;
import com.attivio.sdk.server.annotation.ConfigurationOption;
import com.attivio.sdk.server.component.query.QueryTransformer;
import com.attivio.util.ObjectUtils;
import com.attivio.util.query.QueryTraverser;

/** This query transformer shows an example of rewriting a simple table:field query into a complex join.
 *
 * <p>The example use case is to allow users to search for an author name but have that query get mapped to a first name or last name search.
 * This could be accomplished with include fields in the schema but this method requires no configuration or ingestion changes.
 * In addition, the rewriter will join in all the books for each author as child documents based on authorid.</p>
 *
 * <h3>User query</h3>
 *
 * <code>authors:mike</code>
 *
 * <h3>will be rewritten to</h3>
 *
 * <code>JOIN(AND(table:authors, OR(firstname:mike, lastname:mike)), INNER(table:books, on="authorid"))</code>
 */
public class SampleQueryRewriteTransformer implements QueryTransformer {

  private String primaryTableName = "authors";
  private String childTableName = "books";
  private String joinKey = "authorid";
  private List<String> orFields = ObjectUtils.newList("firstname", "lastname");

  private final SampleQueryTraverser rewriter = new SampleQueryTraverser();

  @Override
  public List<QueryFeedback> processQuery(QueryRequest queryRequest) throws AttivioException {

    /* A note about rewriting queries.
     *
     * Performing blanket rewrites of queries without first inspecting the user's input is not always safe.
     * Depending on the user's input and the type of query it is being rewritten to, the query logic may not work as intended.
     * For example, wrapping a user's query in a join may have unintended consequences if a user passes in an already
     * complex join expression.  For this reason, we usually recommend that developers use some kind of flag on the query request or
     * query itself to indicate that a user's query should be rewritten.
     */

    rewriter.rewriteAll(queryRequest);

    // adding feedback is optional but is useful for letting end users know what happened.  Return null if there is no feedback
    List<QueryFeedback> feedback = new ArrayList<QueryFeedback>();
    feedback.add(new QueryFeedback(this.getClass().getSimpleName(), "queryrewrite", "rewrote query to" + queryRequest.getQuery()));
    return feedback;
  }

  /** Performs the actual rewriting of the query. */
  private final class SampleQueryTraverser extends QueryTraverser {
    @Override
    protected Query rewriteQuery(Query query) {
      if (query instanceof PhraseQuery) {
        PhraseQuery pq = (PhraseQuery) query;
        if (primaryTableName.equals(pq.getField())) {
          BooleanOrQuery orQuery = new BooleanOrQuery();
          for (String fieldName : orFields) {
            orQuery.add(new PhraseQuery(fieldName, pq.getPhrase().clone()));
          }
          JoinQuery join = new JoinQuery(new BooleanAndQuery(new PhraseQuery(FieldNames.TABLE, primaryTableName), orQuery));
          join.addInnerJoin(new PhraseQuery(FieldNames.TABLE, childTableName), joinKey, joinKey);
          query = join;
        }
      }
      return query;
    }
  }

  @ConfigurationOption(description="Table to look for rewriting and table that becomes the parent table of the join.")
  public String getPrimaryTableName() {
    return primaryTableName;
  }

  public void setPrimaryTableName(String primaryTableName) {
    this.primaryTableName = primaryTableName;
  }

  @ConfigurationOption(description="Table to join in as child documents")
  public String getChildTableName() {
    return childTableName;
  }

  public void setChildTableName(String childTableName) {
    this.childTableName = childTableName;
  }

  @ConfigurationOption(description="Join key field name")
  public String getJoinKey() {
    return joinKey;
  }

  public void setJoinKey(String joinKey) {
    this.joinKey = joinKey;
  }

  @ConfigurationOption(formEntryClass=ConfigurationOption.STRING_LIST, description="List of fields to search across for the user's query term")
  public List<String> getOrFields() {
    return orFields;
  }

  public void setOrFields(List<String> orFields) {
    this.orFields = orFields;
  }

}
