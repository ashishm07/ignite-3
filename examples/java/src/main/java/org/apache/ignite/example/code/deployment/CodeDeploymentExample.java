package org.apache.ignite.example.code.deployment;

import static org.apache.ignite.example.util.DeployComputeUnit.buildJar;
import static org.apache.ignite.example.util.DeployComputeUnit.deployUnitIfNeeded;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.compute.JobDescriptor;
import org.apache.ignite.compute.JobTarget;
import org.apache.ignite.deployment.DeploymentUnit;

public class CodeDeploymentExample {

    private static final String UNIT_NAME = "codeDeploymentExampleUnit";
    private static final String UNIT_VERSION = "1.0.0";

    private static final DeploymentUnit DEPLOYMENT_UNIT = new DeploymentUnit(UNIT_NAME, UNIT_VERSION);

    private static final Path BASE_DIR = Paths.get(System.getProperty("user.dir"));

    private static final Path CLASSES_DIR = BASE_DIR.resolve("build/classes/java/main");
    private static final Path JAR_PATH = BASE_DIR.resolve("my-job-unit.jar");


    public static void main(String[] args) throws Exception {

        buildJar(CLASSES_DIR, JAR_PATH);
        deployUnitIfNeeded(UNIT_NAME, UNIT_VERSION, JAR_PATH);

        try (IgniteClient client = IgniteClient.builder().addresses("127.0.0.1:10800").build()) {

            JobDescriptor<String, String> job = JobDescriptor
                    .builder(MyJob.class)
                    .units(DEPLOYMENT_UNIT)
                    .resultClass(String.class)
                    .build();

            JobTarget target = JobTarget.anyNode(client.cluster().nodes());

            String result = client.compute().execute(target, job, "Hello from Java deployment!");
            System.out.println("\n=== Result from cluster ===\n" + result);
        }
    }


}