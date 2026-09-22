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
import com.google.zetasql.parser.ASTNodes.ASTStatement;
import com.google.zetasql.toolkit.antipattern.parser.visitors.IdentifyOrderByWithoutLimitVisitor;
import com.google.zetasql.toolkit.antipattern.parser.visitors.IdentifySimpleSelectStarVisitor;
import org.junit.Before;
import org.junit.Test;

// Regression test for #101. GROUP BY ALL is unknown to the ZetaSQL parser before 2023.11.1;
// against an older zetasql-client these fail with "Syntax error: Unexpected keyword ALL", which
// aborts every visitor for the query and drops it from the report entirely.
public class GroupByAllTest {

  LanguageOptions languageOptions;

  @Before
  public void setUp() {
    languageOptions = new LanguageOptions();
    languageOptions.enableMaximumLanguageFeatures();
    languageOptions.setSupportsAllStatementKinds();
  }

  @Test
  public void groupByAllTest() {
    String expected = "";
    String query = "SELECT \n"
        + "t1.col1, \n"
        + "COUNT(1) \n"
        + "FROM \n"
        + "`project.dataset.table1` t1 \n"
        + "GROUP BY ALL;";
    ASTStatement parsedQuery = Parser.parseStatement(query, languageOptions);
    IdentifySimpleSelectStarVisitor visitor = new IdentifySimpleSelectStarVisitor();
    parsedQuery.accept(visitor);
    String recommendation = visitor.getResult();
    assertEquals(expected, recommendation);
  }

  @Test
  public void groupByAllWithOrderByTest() {
    String expected = "ORDER BY clause without LIMIT at line 7.";
    String query = "SELECT \n"
        + "t1.col1, \n"
        + "COUNT(1) c \n"
        + "FROM \n"
        + "`project.dataset.table1` t1 \n"
        + "GROUP BY ALL \n"
        + "ORDER BY c DESC;";
    ASTStatement parsedQuery = Parser.parseStatement(query, languageOptions);
    IdentifyOrderByWithoutLimitVisitor visitor = new IdentifyOrderByWithoutLimitVisitor(query);
    parsedQuery.accept(visitor);
    String recommendation = visitor.getResult();
    assertEquals(expected, recommendation);
  }

  @Test
  public void groupByAllMultiStatementTest() {
    String expected = "";
    String query = "SELECT t1.col1, COUNT(1) FROM `project.dataset.table1` t1 GROUP BY ALL; \n"
        + "SELECT t2.col1, COUNT(1) FROM `project.dataset.table2` t2 GROUP BY ALL;";
    ASTScript parsedQuery = Parser.parseScript(query, languageOptions);
    IdentifySimpleSelectStarVisitor visitor = new IdentifySimpleSelectStarVisitor();
    parsedQuery.accept(visitor);
    String recommendation = visitor.getResult();
    assertEquals(expected, recommendation);
  }
}
