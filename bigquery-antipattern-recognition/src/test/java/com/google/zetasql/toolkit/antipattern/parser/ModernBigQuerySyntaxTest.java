/*
 * Copyright (C) 2024 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.zetasql.toolkit.antipattern.parser;

import static org.junit.Assert.assertEquals;

import com.google.zetasql.LanguageOptions;
import com.google.zetasql.Parser;
import com.google.zetasql.parser.ASTNodes.ASTScript;
import com.google.zetasql.toolkit.antipattern.parser.visitors.IdentifySimpleSelectStarVisitor;
import org.junit.Before;
import org.junit.Test;

// BigQuery syntax the previously pinned zetasql-client could not parse. Each of these aborted the
// whole query, which the tool reports as "no anti-patterns found" rather than as an error, so an
// unparsed query was indistinguishable from a clean one. These also record why the upgrade targets
// 2025.09.1 rather than the 2024.03.1 that zetasql-toolkit 0.5.2 is built against: every query
// below still fails to parse on 2024.03.1.
public class ModernBigQuerySyntaxTest {

  LanguageOptions languageOptions;

  @Before
  public void setUp() {
    languageOptions = new LanguageOptions();
    languageOptions.enableMaximumLanguageFeatures();
    languageOptions.setSupportsAllStatementKinds();
  }

  @Test
  public void pipeSyntaxTest() {
    String expected = "";
    String query = "FROM `project.dataset.table1` \n"
        + "|> WHERE col2 > 10 \n"
        + "|> AGGREGATE COUNT(*) AS n GROUP BY col1;";
    ASTScript parsedQuery = Parser.parseScript(query, languageOptions);
    IdentifySimpleSelectStarVisitor visitor = new IdentifySimpleSelectStarVisitor();
    parsedQuery.accept(visitor);
    String recommendation = visitor.getResult();
    assertEquals(expected, recommendation);
  }

  @Test
  public void unionAllByNameTest() {
    String expected = "";
    String query = "SELECT col1, col2 FROM `project.dataset.table1` \n"
        + "UNION ALL BY NAME \n"
        + "SELECT col2, col1 FROM `project.dataset.table2`;";
    ASTScript parsedQuery = Parser.parseScript(query, languageOptions);
    IdentifySimpleSelectStarVisitor visitor = new IdentifySimpleSelectStarVisitor();
    parsedQuery.accept(visitor);
    String recommendation = visitor.getResult();
    assertEquals(expected, recommendation);
  }

  @Test
  public void lateralJoinTest() {
    String expected = "";
    String query = "SELECT t1.col1, x \n"
        + "FROM `project.dataset.table1` t1, \n"
        + "LATERAL (SELECT val AS x FROM UNNEST(t1.arr) AS val);";
    ASTScript parsedQuery = Parser.parseScript(query, languageOptions);
    IdentifySimpleSelectStarVisitor visitor = new IdentifySimpleSelectStarVisitor();
    parsedQuery.accept(visitor);
    String recommendation = visitor.getResult();
    assertEquals(expected, recommendation);
  }

  @Test
  public void chainedFunctionCallsTest() {
    String expected = "";
    String query = "SELECT (t1.col1).REPLACE('a', 'b').UPPER() \n"
        + "FROM `project.dataset.table1` t1;";
    ASTScript parsedQuery = Parser.parseScript(query, languageOptions);
    IdentifySimpleSelectStarVisitor visitor = new IdentifySimpleSelectStarVisitor();
    parsedQuery.accept(visitor);
    String recommendation = visitor.getResult();
    assertEquals(expected, recommendation);
  }

  @Test
  public void limitAllTest() {
    String expected = "";
    String query = "SELECT t1.col1 FROM `project.dataset.table1` t1 LIMIT ALL;";
    ASTScript parsedQuery = Parser.parseScript(query, languageOptions);
    IdentifySimpleSelectStarVisitor visitor = new IdentifySimpleSelectStarVisitor();
    parsedQuery.accept(visitor);
    String recommendation = visitor.getResult();
    assertEquals(expected, recommendation);
  }
}
