/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ignite.example.compute;

import static java.sql.DriverManager.getConnection;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.apache.ignite.compute.BroadcastJobTarget.table;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.CompletableFuture;
import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.compute.BroadcastJobTarget;
import org.apache.ignite.compute.ComputeJob;
import org.apache.ignite.compute.IgniteCompute;
import org.apache.ignite.compute.JobDescriptor;
import org.apache.ignite.compute.JobExecutionContext;
import org.apache.ignite.deployment.DeploymentUnit;
import org.apache.ignite.table.QualifiedName;

/**
 * This example demonstrates the usage of the {@link IgniteCompute#execute(BroadcastJobTarget, JobDescriptor, Object)} API.
 *
 * <p>Find instructions on how to run the example in the README.md file located in the "examples" directory root.
 *
 * <p>This example is intended to be run on a cluster with more than one node to show that the job is broadcast to each node.
 *
 * <p>The following steps related to code deployment should be additionally executed before running the current example:
 * <ol>
 *     <li>
 *         Build "ignite-examples-x.y.z.jar" using the next command:<br>
 *         {@code ./gradlew :ignite-examples:jar}
 *     </li>
 *     <li>
 *         Create a new deployment unit using the CLI tool:<br>
 *         {@code cluster unit deploy computeExampleUnit \
 *          --version 1.0.0 \
 *          --path=$IGNITE_HOME/examples/build/libs/ignite-examples-x.y.z.jar}
 *     </li>
 * </ol>
 */
public class ComputeBroadcastExample {
    /** Deployment unit name. */
    private static final String DEPLOYMENT_UNIT_NAME = "computeExampleUnit";

    /** Deployment unit version. */
    private static final String DEPLOYMENT_UNIT_VERSION = "1.0.0";

    private static final Path projectRoot = Paths.get("").toAbsolutePath(); // This resolves ignite-examples/
    private static final Path CLASSES_DIR = projectRoot.resolve("examples/java/build/classes/java/main"); // Compiled output
    private static final Path JAR_PATH = Path.of("build/libs/serialization-example-1.0.0.jar"); // Output jar

    /**
     * Main method of the example.
     *
     * @param args The command line arguments.
     */
    public static void main(String[] args) {
        //--------------------------------------------------------------------------------------
        //
        // Creating a client to connect to the cluster.
        //
        //--------------------------------------------------------------------------------------

        System.out.println("\nConnecting to server...");

        try (IgniteClient client = IgniteClient.builder()
                .addresses("127.0.0.1:10800")
                .build()
        ) {

            try (
                    Connection conn = getConnection("jdbc:ignite:thin://127.0.0.1:10800/");
                    Statement stmt = conn.createStatement()
            ) {

                stmt.executeUpdate("DROP TABLE IF EXISTS Person");

                // Create table
                stmt.executeUpdate("CREATE TABLE PERSON ("
                        + "    ID INT PRIMARY KEY"
                        + "    FIRST_NAME VARCHAR,"
                        + "    LAST_NAME VARCHAR,"
                        + "    AGE INT"
                        + ");"
                );

                System.out.println("PERSON table created.");

                // Insert sample data
                stmt.executeUpdate("INSERT INTO PERSON(ID, FIRST_NAME, LAST_NAME, AGE) VALUES (1, 'John', 'Doe', 30)");
                stmt.executeUpdate("INSERT INTO PERSON(ID, FIRST_NAME, LAST_NAME, AGE) VALUES (2, 'Jane', 'Smith', 25)");
                stmt.executeUpdate("INSERT INTO PERSON(ID, FIRST_NAME, LAST_NAME, AGE) VALUES (3, 'Alice', 'Johnson', 40)");
                stmt.executeUpdate("INSERT INTO PERSON(ID, FIRST_NAME, LAST_NAME, AGE) VALUES (4, 'Bob', 'Brown', 22)");

                System.out.println("Sample data inserted.");

                // Step 2: Create a schema
                stmt.executeUpdate("CREATE SCHEMA IF NOT EXISTS CUSTOM_SCHEMA");

                // Step 3: Create a table in that schema
                stmt.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS CUSTOM_SCHEMA.MY_QUALIFIED_TABLE (" +
                                "ID INT PRIMARY KEY, " +
                                "NAME VARCHAR, " +
                                "AGE INT" +
                                ")"
                );

                // Step 4: Insert some sample data
                stmt.executeUpdate("INSERT INTO CUSTOM_SCHEMA.MY_QUALIFIED_TABLE VALUES (1, 'Alice', 30)");
                stmt.executeUpdate("INSERT INTO CUSTOM_SCHEMA.MY_QUALIFIED_TABLE VALUES (2, 'Bob', 25)");

                System.out.println("Schema and table created successfully!");

            } catch (SQLException e) {
                throw new RuntimeException(e);
            }

            //--------------------------------------------------------------------------------------
            //
            // Configuring compute job.
            //
            //--------------------------------------------------------------------------------------

            System.out.println("\nConfiguring compute job...");

            JobDescriptor<String, Void> job = JobDescriptor.builder(HelloMessageJob.class)
                    .units(new DeploymentUnit(DEPLOYMENT_UNIT_NAME, DEPLOYMENT_UNIT_VERSION))
                    .build();

            BroadcastJobTarget target = table("Person");

            //--------------------------------------------------------------------------------------
            //
            // Executing compute job using configured jobTarget.
            //
            //--------------------------------------------------------------------------------------

            System.out.println("\nExecuting compute job...");

            client.compute().execute(target, job, "John");

            System.out.println("\nCompute job executed...");

            //--------------------------------------------------------------------------------------
            //
            // Executing compute job using a custom by specifying a fully qualified table name .
            //
            //

            QualifiedName customSchemaTable = QualifiedName.parse("CUSTOM_SCHEMA.MY_QUALIFIED_TABLE");
            client.compute().execute(table(customSchemaTable),
                    JobDescriptor.builder(HelloMessageJob.class).build(), null
            );

            QualifiedName customSchemaTableName = QualifiedName.of("PUBLIC", "MY_TABLE");
            client.compute().execute(table(customSchemaTableName),
                    JobDescriptor.builder(HelloMessageJob.class).build(), null
            );
        }
    }

    /**
     * Job that prints hello message with provided name.
     */
    public static class HelloMessageJob implements ComputeJob<String, Void> {
        /** {@inheritDoc} */
        @Override
        public CompletableFuture<Void> executeAsync(JobExecutionContext context, String arg) {
            System.out.println("Hello " + arg + "!");

            return completedFuture(null);
        }
    }
}
