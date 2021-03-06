/*
 * Copyright 1999-2015 dangdang.com.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * </p>
 */

package io.shardingjdbc.test.sql.jaxb.helper;

import com.google.common.collect.Sets;
import io.shardingjdbc.test.sql.jaxb.DatabaseType;
import io.shardingjdbc.test.sql.jaxb.SQLStatement;
import io.shardingjdbc.test.sql.jaxb.SQLStatements;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SQLStatementHelper {
    
    private static final Map<String, SQLStatement> STATEMENT_MAP;
    
    private static final Map<String, SQLStatement> UNSUPPORTED_STATEMENT_MAP;
    
    static {
        STATEMENT_MAP = loadSQLCases("sql");
        UNSUPPORTED_STATEMENT_MAP = loadSQLCases("sql/unsupported");
    }
    
    private static Map<String, SQLStatement> loadSQLCases(final String path) {
        File file = new File(SQLStatementHelper.class.getProtectionDomain().getCodeSource().getLocation().getPath());
        try {
            return file.isFile() ? loadSQLCasesFromJar(path, file) : loadSQLCasesFromTargetFolder(path);
        } catch (final IOException | JAXBException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    private static Map<String, SQLStatement> loadSQLCasesFromJar(final String path, final File file) throws IOException, JAXBException {
        Map<String, SQLStatement> result = new HashMap<>(65536, 1);
        try (JarFile jar = new JarFile(file)) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                String name = entries.nextElement().getName();
                if (name.startsWith(path + "/") && name.endsWith(".xml")) {
                    fillStatementMap(result, SQLStatementHelper.class.getClassLoader().getResourceAsStream(name));
                }
            }
        }
        return result;
    }
    
    private static Map<String, SQLStatement> loadSQLCasesFromTargetFolder(final String path) throws FileNotFoundException, JAXBException {
        Map<String, SQLStatement> result = new HashMap<>(65536, 1);
        URL url = SQLStatementHelper.class.getClassLoader().getResource(path);
        if (null == url) {
            return result;
        }
        File filePath = new File(url.getPath());
        if (!filePath.exists()) {
            return result;
        }
        File[] files = filePath.listFiles();
        if (null == files) {
            return result;
        }
        for (File each : files) {
            loadSQLCasesFromFolder(result, each);
        }
        return result;
    }
    
    private static void loadSQLCasesFromFolder(final Map<String, SQLStatement> sqlStatementMap, final File file) throws FileNotFoundException, JAXBException {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (null == files) {
                return;
            }
            for (File each : files) {
                fillStatementMap(sqlStatementMap, new FileInputStream(each));
            }
        } else {
            fillStatementMap(sqlStatementMap, new FileInputStream(file));
        }
    }
    
    private static void fillStatementMap(final Map<String, SQLStatement> result, final InputStream inputStream) throws JAXBException {
        SQLStatements statements = (SQLStatements) JAXBContext.newInstance(SQLStatements.class).createUnmarshaller().unmarshal(inputStream);
        for (SQLStatement statement : statements.getSqls()) {
            result.put(statement.getId(), statement);
        }
    }
    
    /**
     * Get unsupported SQL statements.
     * 
     * @return unsupported SQL statements
     */
    public static Collection<SQLStatement> getUnsupportedSqlStatements() {
        return UNSUPPORTED_STATEMENT_MAP.values();
    }
    
    /**
     * Get SQL.
     * @param sqlId SQL ID
     * @return SQL
     */
    public static String getSql(final String sqlId) {
        checkSqlId(sqlId);
        SQLStatement statement = STATEMENT_MAP.get(sqlId);
        return statement.getSql();
    }
    
    /**
     * Get database types.
     * 
     * @param sqlId SQL ID
     * @return database types
     */
    public static Set<DatabaseType> getTypes(final String sqlId) {
        checkSqlId(sqlId);
        SQLStatement statement = STATEMENT_MAP.get(sqlId);
        if (null == statement.getTypes()) {
            return Sets.newHashSet(DatabaseType.values());
        }
        Set<DatabaseType> result = new HashSet<>();
        for (String each : statement.getTypes().split(",")) {
            result.add(DatabaseType.valueOf(each));
        }
        return result;
    }
    
    private static void checkSqlId(final String sqlId) {
        if (null == sqlId || !STATEMENT_MAP.containsKey(sqlId)) {
            throw new RuntimeException("Can't find sql of id:" + sqlId);
        }
    }
}
